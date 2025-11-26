/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.universal.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseAuthorizerTest {
    private static final String TEST_ROLE = "testRole";
    private AuthZResourceExtractor.Factory extractorFactory;
    private BaseAuthorizer<TeletraanPrincipal> sut;
    private TeletraanPrincipal principal;
    private ContainerRequestContext context;

    @BeforeEach
    void setUp() {
        extractorFactory = mock(AuthZResourceExtractor.Factory.class);
        context = mock(ContainerRequestContext.class);
        sut = new TestAuthorizer(extractorFactory);
        principal = new UserPrincipal("testUser", Collections.singletonList("group"));
    }

    @Test
    void testAuthorizeWithoutContext() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    sut.authorize(principal, TEST_ROLE);
                });
    }

    @Test
    void testAuthorize_contextIsNull() {
        assertFalse(sut.authorize(principal, TEST_ROLE, null));
    }

    @Test
    void testAuthorize_contextHasNoResourceAuthZInfo() {
        assertTrue(sut.authorize(principal, TEST_ROLE, context));
    }

    @Test
    void testAuthorize_contextHasInvalidResourceAuthZInfo() {
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(new Object());

        assertFalse(sut.authorize(principal, TEST_ROLE, context));
    }

    @Test
    void testAuthorize_contextHasValidResourceAuthZInfo() throws ExtractionException {
        ResourceAuthZInfo authZInfo = mock(ResourceAuthZInfo.class);
        AuthZResourceExtractor extractor = mock(AuthZResourceExtractor.class);

        when(context.getProperty(ResourceAuthZInfo.class.getName()))
                .thenReturn(new ResourceAuthZInfo[] {authZInfo});
        when(extractorFactory.create(authZInfo)).thenReturn(extractor);
        when(extractor.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test", AuthZResource.Type.ENV));

        assertTrue(sut.authorize(principal, TEST_ROLE, context));
        verify(extractorFactory).create(authZInfo);
        verify(extractor).extractResource(context, authZInfo.beanClass());

        when(extractor.extractResource(any(), any())).thenThrow(new ExtractionException(null));
        assertThrows(
                WebApplicationException.class, () -> sut.authorize(principal, TEST_ROLE, context));
    }

    @Test
    void testAuthorize_systemResource() throws ExtractionException {
        ResourceAuthZInfo authZInfo = mock(ResourceAuthZInfo.class);

        when(authZInfo.type()).thenReturn(AuthZResource.Type.SYSTEM);
        when(context.getProperty(ResourceAuthZInfo.class.getName()))
                .thenReturn(new ResourceAuthZInfo[] {authZInfo});

        assertTrue(sut.authorize(principal, TEST_ROLE, context));
        verify(extractorFactory, never()).create(authZInfo);
    }

    @Test
    void testAuthorize_arrayOfResourceAuthZInfo() throws ExtractionException {
        ResourceAuthZInfo authZInfo1 = mock(ResourceAuthZInfo.class);
        ResourceAuthZInfo authZInfo2 = mock(ResourceAuthZInfo.class);
        AuthZResourceExtractor extractor1 = mock(AuthZResourceExtractor.class);
        AuthZResourceExtractor extractor2 = mock(AuthZResourceExtractor.class);

        ResourceAuthZInfo[] authZInfoArray = new ResourceAuthZInfo[] {authZInfo1, authZInfo2};
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(authZInfoArray);
        when(extractorFactory.create(authZInfo1)).thenReturn(extractor1);
        when(extractorFactory.create(authZInfo2)).thenReturn(extractor2);
        when(extractor1.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test1", AuthZResource.Type.ENV));
        when(extractor2.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test2", AuthZResource.Type.ENV_STAGE));

        assertTrue(sut.authorize(principal, TEST_ROLE, context));
        verify(extractorFactory).create(authZInfo1);
    }

    @Test
    void testAuthorize_arrayWithORLogic() throws ExtractionException {
        // Create an authorizer that only grants access to ENV_STAGE resources
        BaseAuthorizer<TeletraanPrincipal> selectiveAuthorizer =
                new SelectiveTestAuthorizer(extractorFactory);

        ResourceAuthZInfo authZInfo1 = mock(ResourceAuthZInfo.class);
        ResourceAuthZInfo authZInfo2 = mock(ResourceAuthZInfo.class);
        AuthZResourceExtractor extractor1 = mock(AuthZResourceExtractor.class);
        AuthZResourceExtractor extractor2 = mock(AuthZResourceExtractor.class);

        ResourceAuthZInfo[] authZInfoArray = new ResourceAuthZInfo[] {authZInfo1, authZInfo2};
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(authZInfoArray);
        when(extractorFactory.create(authZInfo1)).thenReturn(extractor1);
        when(extractorFactory.create(authZInfo2)).thenReturn(extractor2);
        when(extractor1.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test1", AuthZResource.Type.ENV));
        when(extractor2.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test2", AuthZResource.Type.ENV_STAGE));

        // Should return true because second resource (ENV_STAGE) grants access
        assertTrue(selectiveAuthorizer.authorize(principal, TEST_ROLE, context));
    }

    @Test
    void testAuthorize_arrayWithAllDenied() throws ExtractionException {
        // Create an authorizer that denies all access
        BaseAuthorizer<TeletraanPrincipal> denyAuthorizer =
                new DenyAllTestAuthorizer(extractorFactory);

        ResourceAuthZInfo authZInfo1 = mock(ResourceAuthZInfo.class);
        ResourceAuthZInfo authZInfo2 = mock(ResourceAuthZInfo.class);
        AuthZResourceExtractor extractor1 = mock(AuthZResourceExtractor.class);
        AuthZResourceExtractor extractor2 = mock(AuthZResourceExtractor.class);

        ResourceAuthZInfo[] authZInfoArray = new ResourceAuthZInfo[] {authZInfo1, authZInfo2};
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(authZInfoArray);
        when(extractorFactory.create(authZInfo1)).thenReturn(extractor1);
        when(extractorFactory.create(authZInfo2)).thenReturn(extractor2);
        when(extractor1.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test1", AuthZResource.Type.ENV));
        when(extractor2.extractResource(any(), any()))
                .thenReturn(new AuthZResource("test2", AuthZResource.Type.ENV_STAGE));

        // Should return false because all resources deny access
        assertFalse(denyAuthorizer.authorize(principal, TEST_ROLE, context));
    }

    @Test
    void testAuthorize_arrayWithSystemResource() {
        ResourceAuthZInfo authZInfo1 = mock(ResourceAuthZInfo.class);
        ResourceAuthZInfo authZInfo2 = mock(ResourceAuthZInfo.class);

        ResourceAuthZInfo[] authZInfoArray = new ResourceAuthZInfo[] {authZInfo1, authZInfo2};
        when(authZInfo1.type()).thenReturn(AuthZResource.Type.SYSTEM);
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(authZInfoArray);

        // Should return true immediately when hitting SYSTEM resource
        assertTrue(sut.authorize(principal, TEST_ROLE, context));
        verify(extractorFactory, never()).create(any());
    }

    @Test
    void testAuthorize_emptyArray() {
        ResourceAuthZInfo[] emptyArray = new ResourceAuthZInfo[0];
        when(context.getProperty(ResourceAuthZInfo.class.getName())).thenReturn(emptyArray);

        BaseAuthorizer<TeletraanPrincipal> denyAuthorizer =
                new DenyAllTestAuthorizer(extractorFactory);

        // Should return false for empty array (no resources grant access)
        assertFalse(denyAuthorizer.authorize(principal, TEST_ROLE, context));
    }

    class TestAuthorizer extends BaseAuthorizer<TeletraanPrincipal> {
        public TestAuthorizer(AuthZResourceExtractor.Factory extractorFactory) {
            super(extractorFactory);
        }

        @Override
        public boolean authorize(
                TeletraanPrincipal principal,
                String role,
                AuthZResource requestedResource,
                @Nullable ContainerRequestContext context) {
            return true;
        }
    }

    class SelectiveTestAuthorizer extends BaseAuthorizer<TeletraanPrincipal> {
        public SelectiveTestAuthorizer(AuthZResourceExtractor.Factory extractorFactory) {
            super(extractorFactory);
        }

        @Override
        public boolean authorize(
                TeletraanPrincipal principal,
                String role,
                AuthZResource requestedResource,
                @Nullable ContainerRequestContext context) {
            // Only authorize ENV_STAGE resources
            return AuthZResource.Type.ENV_STAGE.equals(requestedResource.getType());
        }
    }

    class DenyAllTestAuthorizer extends BaseAuthorizer<TeletraanPrincipal> {
        public DenyAllTestAuthorizer(AuthZResourceExtractor.Factory extractorFactory) {
            super(extractorFactory);
        }

        @Override
        public boolean authorize(
                TeletraanPrincipal principal,
                String role,
                AuthZResource requestedResource,
                @Nullable ContainerRequestContext context) {
            return false;
        }
    }
}
