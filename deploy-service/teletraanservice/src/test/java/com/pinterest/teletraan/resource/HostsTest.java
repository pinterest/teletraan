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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.deployservice.bean.HostBeanWithStatuses;
import com.pinterest.deployservice.bean.HostState;
import com.pinterest.deployservice.bean.KnoxStatus;
import com.pinterest.deployservice.bean.NormandieStatus;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.AnonymousAuthFilter;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(DropwizardExtensionsSupport.class)
public class HostsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final ResourceExtension resourceExtension;

    private static final HostDAO hostDAOMock;

    static {
        TeletraanServiceContext context = new TeletraanServiceContext();

        hostDAOMock = mock(HostDAO.class);
        context.setHostDAO(hostDAOMock);

        resourceExtension =
                ResourceExtension.builder()
                        .addResource(new Hosts(context))
                        .addProvider(new AnonymousAuthFilter())
                        .build();
    }

    @BeforeEach
    public void setup() {
        // the mock is static due to resourceExtension requirements, so we need to reset it before
        // each test
        reset(hostDAOMock);
    }

    @ParameterizedTest
    @MethodSource("validHostBeansSource")
    public void postValidHostBeans() throws Exception {
        HostBean hostBean =
                HostBean.builder()
                        .host_id("hostId")
                        .host_name("hostName")
                        .state(HostState.ACTIVE)
                        .account_id("accountId")
                        .group_name("groupName")
                        .build();

        final Response put =
                resourceExtension.target(Target.V1_HOSTS).request().post(Entity.json(hostBean));

        assertNotEquals(422, put.getStatus());

        verify(hostDAOMock).insert(refEq(hostBean, "create_date", "last_update"));
    }

    @ParameterizedTest
    @MethodSource("validHostBeansSource")
    public void putValidHostBeans() throws Exception {
        HostBean hostBean =
                HostBean.builder()
                        .host_id("hostId")
                        .host_name("hostName")
                        .state(HostState.ACTIVE)
                        .account_id("accountId")
                        .group_name("groupName")
                        .build();

        final Response put =
                resourceExtension
                        .target(Target.V1_HOSTS + "/" + "hostId")
                        .request()
                        .put(Entity.json(hostBean));

        assertNotEquals(422, put.getStatus());

        verify(hostDAOMock)
                .updateHostById(eq(hostBean.getHost_id()), refEq(hostBean, "last_update"));
    }

    @Test
    public void getByHostName() throws Exception {
        HostBeanWithStatuses hostBean =
                HostBeanWithStatuses.builder()
                        .host_id("hostId")
                        .host_name("hostName")
                        .state(HostState.ACTIVE)
                        .account_id("accountId")
                        .group_name("groupName")
                        .normandie_status(NormandieStatus.OK)
                        .knox_status(KnoxStatus.ERROR)
                        .build();

        when(hostDAOMock.getHosts("hostName")).thenReturn(ImmutableList.of(hostBean));

        final Response get =
                resourceExtension
                        .target(Target.V1_HOSTS + "/" + hostBean.getHost_name())
                        .request()
                        .get();

        assertNotEquals(422, get.getStatus());

        // GET returns a list of maps, so we need to convert it to a HostBeanWithStatuses
        List<Map> list = get.readEntity(List.class);
        HostBeanWithStatuses resultBean =
                mapper.convertValue(list.get(0), HostBeanWithStatuses.class);
        assertEquals(hostBean, resultBean);
    }

    private static Stream<Arguments> validHostBeansSource() {
        HostBean hostBean = validHostBean();

        // All fields are null - host is still valid
        HostBean hostBean2 = HostBean.builder().build();

        return Stream.of(Arguments.of(hostBean), Arguments.of(hostBean2));
    }

    private static HostBean validHostBean() {
        return HostBean.builder()
                .host_id("hostId")
                .host_name("hostName")
                .state(HostState.ACTIVE)
                .account_id("accountId")
                .group_name("groupName")
                .build();
    }

    private static class Target {
        private static final String V1_HOSTS = "/v1/hosts";
    }
}
