package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jetty.http.HttpHeader;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.codahale.metrics.SharedMetricRegistries;
import com.pinterest.deployservice.bean.TeletraanPrincipalRoles;
import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupRolesDAO;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.RoleAuthorizationFactory;
import com.pinterest.teletraan.config.TokenAuthenticationFactory;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfoFeature;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ResourceAuthorizationTest {
    private static final String SCRIPT_TOKEN = "script_token";
    private static final String JWT_TOKEN = "jwt_token";
    private static final String JWT_ADMIN_TOKEN = "jwt_admin_token";
    private static final String TEST_USER = "testUser";
    private static final String TEST_ADMIN_USER = "testAdminUser";
    private static final String GROUP_PATH = "/group/";
    private static final String USER_PATH = "/user/";
    private static final String ENV_STAGE_SUFFIX = "/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}";
    private static final String TEST_ENV_STAGE_ID = "testEnv/testStage";
    private static final String TEST_ENV_STAGE_PATH = "/envs/" + TEST_ENV_STAGE_ID;

    private static ResourceExtension EXT;
    private static MockWebServer mockWebServer;

    private static class Targets {
        public static final String root = "/";
        public static final String read = "/read";
        public static final String write = "/write";
        public static final String delete = "/delete";
        public static final String execute = "/execute";
        public static final String none = "/none";
        public static final String all = "/all";
        public static final String admin = "/admin";
    }

    private static String[] protectedResourceTargetsProvider() {
        return new String[] { Targets.read + TEST_ENV_STAGE_PATH, Targets.write + TEST_ENV_STAGE_PATH,
                Targets.delete + TEST_ENV_STAGE_PATH, Targets.execute + TEST_ENV_STAGE_PATH };
    }

    static {
        SharedMetricRegistries.setDefault("test");
        TeletraanServiceContext context = new TeletraanServiceContext();
        TokenAuthenticationFactory authenticationFactory = new TokenAuthenticationFactory();
        RoleAuthorizationFactory authorizationFactory = new RoleAuthorizationFactory();
        TokenRolesDAO tokenRolesDAO = mock(TokenRolesDAO.class);
        UserRolesDAO userRolesDAO = mock(UserRolesDAO.class);
        GroupRolesDAO groupRolesDAO = mock(GroupRolesDAO.class);
        EnvironDAO environDAO = mock(EnvironDAO.class);
        UserRolesBean adminRolesBean = new UserRolesBean();
        TokenRolesBean tokenRolesBean = new TokenRolesBean();

        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(new AuthDispatcher());
        authenticationFactory.setGroupDataUrl(mockWebServer.url(GROUP_PATH).toString());
        authenticationFactory.setUserDataUrl(mockWebServer.url(USER_PATH).toString());
        adminRolesBean.setRole(TeletraanPrincipalRoles.ADMIN);
        tokenRolesBean.setRole(TeletraanPrincipalRoles.OPERATOR);
        tokenRolesBean.setResource_id("testEnv");
        tokenRolesBean.setResource_type(AuthZResource.Type.ENV);

        context.setAuthorizationFactory(authorizationFactory);
        context.setTokenRolesDAO(tokenRolesDAO);
        context.setUserRolesDAO(userRolesDAO);
        context.setGroupRolesDAO(groupRolesDAO);
        context.setEnvironDAO(environDAO);
        context.setAuthZResourceExtractorFactory(new TeletraanAuthZResourceExtractorFactory(context));

        try {
            when(userRolesDAO.getByNameAndResource(TEST_ADMIN_USER, AuthZResource.ALL, AuthZResource.Type.SYSTEM))
                    .thenReturn(adminRolesBean);
            when(tokenRolesDAO.getByToken(SCRIPT_TOKEN)).thenReturn(tokenRolesBean);
            EXT = ResourceExtension.builder()
                    .addProvider(new AuthDynamicFeature(authenticationFactory.create(context)))
                    .addProvider(RolesAllowedDynamicFeature.class)
                    .addResource(TestResource.class)
                    .addResource(ResourceAuthZInfoFeature.class)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void noAuthHeader_unprotected_200() {
        Response response = EXT.target(Targets.root).request().accept(MediaType.APPLICATION_JSON_TYPE).get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void noAuthHeader_permitAll_401() {
        Response response = EXT.target(Targets.all).request().accept(MediaType.APPLICATION_JSON_TYPE).get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void unsupportedTokenType_401() {
        Response response = EXT.target(Targets.all)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "Basic TOKEN")
                .get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    // OAuth legacy token tests
    @Test
    void invalidToken_401() {
        Response response = EXT.target(Targets.all)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token TOKEN")
                .get();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void validToken_200() {
        Response response = EXT.target(Targets.root)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + JWT_TOKEN)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void validToken_permitAll_200() {
        Response response = EXT.target(Targets.all)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + JWT_TOKEN)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("protectedResourceTargetsProvider")
    void validToken_protectedResource_403(String target) {
        Response response = EXT.target(target)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + JWT_TOKEN)
                .get();
        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("protectedResourceTargetsProvider")
    void adminToken_protectedResource_200(String target) {
        Response response = EXT.target(target)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + JWT_ADMIN_TOKEN)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    // Script token tests
    @Test
    void validScriptToken_200() {
        Response response = EXT.target(Targets.root)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + SCRIPT_TOKEN)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("protectedResourceTargetsProvider")
    void validScriptToken_protectedResource_200(String target) {
        Response response = EXT.target(target)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + SCRIPT_TOKEN)
                .get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void validScriptToken_protectedAdminResource_403() {
        Response response = EXT.target(Targets.admin + TEST_ENV_STAGE_PATH)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + SCRIPT_TOKEN)
                .get();
        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void validScriptToken_noAccessResource_403() {
        Response response = EXT.target(Targets.none)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeader.AUTHORIZATION.asString(), "token " + SCRIPT_TOKEN)
                .get();
        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Produces(MediaType.APPLICATION_JSON)
    @Path(Targets.root)
    public static class TestResource {
        @GET
        public Response unprotected() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.read + ENV_STAGE_SUFFIX)
        @RolesAllowed(TeletraanPrincipalRoles.Names.READ)
        @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
        public Response protectedReadResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.write + ENV_STAGE_SUFFIX)
        @RolesAllowed(TeletraanPrincipalRoles.Names.WRITE)
        @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
        public Response protectedWriteResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.execute + ENV_STAGE_SUFFIX)
        @RolesAllowed(TeletraanPrincipalRoles.Names.EXECUTE)
        @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
        public Response protectedExecuteResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.delete + ENV_STAGE_SUFFIX)
        @RolesAllowed(TeletraanPrincipalRoles.Names.DELETE)
        @ResourceAuthZInfo(type = AuthZResource.Type.ENV_STAGE, idLocation = ResourceAuthZInfo.Location.PATH)
        public Response protectedDeleteResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.admin + ENV_STAGE_SUFFIX)
        @RolesAllowed(TeletraanPrincipalRoles.Names.DELETE)
        @ResourceAuthZInfo(type = AuthZResource.Type.ENV, idLocation = ResourceAuthZInfo.Location.PATH)
        public Response protectedAdminResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.none)
        @DenyAll
        public Response deadResource() {
            return Response.ok().build();
        }

        @GET
        @Path(Targets.all)
        @PermitAll
        public Response permitAllResource() {
            return Response.ok().build();
        }
    }

    public static class AuthDispatcher extends Dispatcher {

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            if (request.getPath().contains(JWT_TOKEN)) {
                if (request.getPath().contains(USER_PATH)) {
                    return new MockResponse().setBody(String.format("{\"user\": {\"username\": \"%s\"}}", TEST_USER));
                } else if (request.getPath().contains(GROUP_PATH)) {
                    return new MockResponse().setBody("{\"groups\": [\"group1\", \"group2\"]}");
                }
            } else if (request.getPath().contains(JWT_ADMIN_TOKEN)) {
                if (request.getPath().contains(USER_PATH)) {
                    return new MockResponse()
                            .setBody(String.format("{\"user\": {\"username\": \"%s\"}}", TEST_ADMIN_USER));
                } else if (request.getPath().contains(GROUP_PATH)) {
                    return new MockResponse().setBody("{\"groups\": [\"admingroup1\", \"admingroup2\"]}");
                }
            }
            return new MockResponse().setResponseCode(404);
        }
    }
}
