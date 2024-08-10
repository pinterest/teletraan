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
package com.pinterest.teletraan.resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.GroupDAO;
import com.pinterest.deployservice.fixture.EnvironBeanFixture;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.config.AuthorizationFactory;
import com.pinterest.teletraan.resource.EnvCapacities.CapacityType;
import com.pinterest.teletraan.universal.security.TeletraanAuthorizer;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import java.security.Principal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class EnvCapacitiesTest {
    private EnvCapacities sut;
    private List<String> capacities;

    @Mock private EnvironDAO environDAO;
    @Mock private GroupDAO groupDAO;
    @Mock private TeletraanAuthorizer<TeletraanPrincipal> authorizer;
    @Mock private TeletraanPrincipal principal;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        TeletraanServiceContext serviceContext = new TeletraanServiceContext();
        AuthorizationFactory authorizationFactory = mock(AuthorizationFactory.class);

        when(authorizationFactory.create(any())).thenReturn(authorizer);
        serviceContext.setAuthorizationFactory(authorizationFactory);
        serviceContext.setEnvironDAO(environDAO);
        serviceContext.setGroupDAO(groupDAO);

        sut = new EnvCapacities(serviceContext);
        capacities = new ArrayList<>();
        capacities.add("cap1");
        capacities.add("cap2");
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeShouldAllowSidecarEnvsToAddCapacities(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        envBean.setSystem_priority(1);

        assertDoesNotThrow(() -> sut.authorize(envBean, principal, type, capacities));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeShouldAllowEmptyCapacities(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();

        assertDoesNotThrow(() -> sut.authorize(envBean, principal, type, new ArrayList<>()));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeShouldThrowExceptionForNonTeletraanPrincipal(CapacityType type) {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        Principal nonTeletraanPrincipal = mock(Principal.class);

        assertThrows(
                UnsupportedOperationException.class,
                () -> sut.authorize(envBean, nonTeletraanPrincipal, type, capacities));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeSuccess(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();

        for (String capacity : capacities) {
            when(environDAO.getMainEnvByHostName(capacity)).thenReturn(envBean);
            when(environDAO.getByCluster(capacity)).thenReturn(envBean);
        }
        when(authorizer.authorize(
                        (TeletraanPrincipal) principal,
                        TeletraanPrincipalRole.Names.WRITE,
                        new AuthZResource(envBean.getEnv_name(), envBean.getStage_name()),
                        null))
                .thenReturn(true);
        assertDoesNotThrow(() -> sut.authorize(envBean, principal, type, capacities));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeSucceedsWhenNoMainEnvFound(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();

        for (String capacity : capacities) {
            when(environDAO.getMainEnvByHostName(capacity)).thenReturn(null);
            when(environDAO.getByCluster(capacity)).thenReturn(null);
        }
        assertDoesNotThrow(() -> sut.authorize(envBean, principal, type, capacities));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeThrowsExceptionWhenDBException(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();

        for (String capacity : capacities) {
            when(environDAO.getMainEnvByHostName(capacity)).thenThrow(SQLException.class);
            when(environDAO.getByCluster(capacity)).thenThrow(SQLException.class);
        }
        assertThrows(
                InternalServerErrorException.class,
                () -> sut.authorize(envBean, principal, type, capacities));
    }

    @ParameterizedTest
    @MethodSource("capacityTypes")
    void authorizeThrowsExceptionWhenAccessDenied(CapacityType type) throws Exception {
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();

        for (String capacity : capacities) {
            when(environDAO.getMainEnvByHostName(capacity)).thenReturn(envBean);
            when(environDAO.getByCluster(capacity)).thenReturn(envBean);
        }
        when(authorizer.authorize(any(), any(), any(), any())).thenReturn(false);
        assertThrows(
                ForbiddenException.class,
                () -> sut.authorize(envBean, principal, type, capacities));
    }

    static Stream<Arguments> capacityTypes() {
        return Stream.of(
                Arguments.of(EnvCapacities.CapacityType.GROUP),
                Arguments.of(EnvCapacities.CapacityType.HOST));
    }

    @Test
    void addGroupSuccess() throws Exception {
        // Given the parameters
        String envName = "testEnvName";
        String stageName = "testStageName";
        Optional<CapacityType> capacityType = Optional.of(CapacityType.GROUP);
        String capacityGroup = "testCapacityGroup";
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getUserPrincipal()).thenReturn(principal);

        // And the mock dependencies
        EnvironBean envBean = EnvironBeanFixture.createRandomEnvironBean();
        when(environDAO.getByStage(envName, stageName)).thenReturn(envBean);
        when(environDAO.getByCluster(capacityGroup)).thenReturn(envBean);
        AuthZResource authZResource =
                new AuthZResource(envBean.getEnv_name(), envBean.getStage_name());
        when(authorizer.authorize(
                        principal, TeletraanPrincipalRole.Names.WRITE, authZResource, null))
                .thenReturn(true);

        // Verify the call succeeds
        sut.add(envName, stageName, capacityType, capacityGroup, sc);

        // Verify mock calls
        verify(environDAO).getByStage(envName, stageName);
        verify(environDAO).getByCluster(capacityGroup);
        verify(authorizer)
                .authorize(principal, TeletraanPrincipalRole.Names.WRITE, authZResource, null);
        verify(groupDAO).addGroupCapacity(envBean.getEnv_id(), capacityGroup);
    }
}
