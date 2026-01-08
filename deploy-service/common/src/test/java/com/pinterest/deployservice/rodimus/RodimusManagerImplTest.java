/**
 * Copyright (c) 2023 Pinterest, Inc.
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
package com.pinterest.deployservice.rodimus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.deployservice.bean.ClusterInfoPublicIdsBean;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingAlarm;
import com.pinterest.deployservice.bean.rodimus.RodimusAutoScalingPolicies;
import com.pinterest.deployservice.bean.rodimus.RodimusScheduledAction;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RodimusManagerImplTest {
    private static final String TEST_CLUSTER = "cluster1";
    private static final List<String> HOST_IDS = Collections.singletonList("i-001");
    private static final String TEST_PATH = "/testUrl";

    private static MockWebServer mockWebServer;
    private RodimusManagerImpl sut;

    @BeforeEach
    public void setUpEach() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        sut = new RodimusManagerImpl(mockWebServer.url(TEST_PATH).toString(), null, false, "", "");
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testConstructorInValidProxyConfig() {
        assertThrows(
                NumberFormatException.class,
                () -> {
                    new RodimusManagerImpl(TEST_PATH, null, true, "localhost", "invalidPort");
                });
    }

    @Test
    void testNullKnoxKeyUsesDefaultKey() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("[]"));
        sut.getTerminatedHosts(Collections.singletonList("testHost"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("token defaultKeyContent", request.getHeader("Authorization"));
    }

    @Test
    void testInvalidKnoxKeyThrowsException() throws Exception {
        RodimusManagerImpl sut =
                new RodimusManagerImpl(
                        mockWebServer.url(TEST_PATH).toString(),
                        "invalidRodimusKnoxKey",
                        false,
                        "",
                        "");
        assertThrows(IllegalStateException.class, () -> sut.getTerminatedHosts(HOST_IDS));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testTerminateHostsByClusterNameOk() {
        mockWebServer.enqueue(new MockResponse());

        assertDoesNotThrow(
                () -> {
                    sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                });
    }

    @Test
    void testTerminateHostsByClusterNameEmptyHosts() {
        assertDoesNotThrow(
                () -> {
                    sut.terminateHostsByClusterName(TEST_CLUSTER, Collections.emptyList());
                });
    }

    @Test
    void testTerminateHostsByClusterNameClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        ClientErrorException exception =
                assertThrows(
                        ClientErrorException.class,
                        () -> {
                            sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                        });
        assertEquals(404, exception.getResponse().getStatus());
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void testTerminateHostsByClusterNameServerError() {
        mockWebServer.setDispatcher(new ServerErrorDispatcher());
        ServerErrorException exception =
                assertThrows(
                        ServerErrorException.class,
                        () -> {
                            sut.terminateHostsByClusterName(TEST_CLUSTER, HOST_IDS);
                        });
        assertEquals(500, exception.getResponse().getStatus());
        assertEquals(3, mockWebServer.getRequestCount());
    }

    @Test
    void testGetTerminatedHostsOk() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody(HOST_IDS.toString()));

        Collection<String> terminatedHosts = sut.getTerminatedHosts(HOST_IDS);
        assertArrayEquals(HOST_IDS.toArray(), terminatedHosts.toArray());
    }

    @Test
    void testGetTerminatedHostEmptyHostIds() throws Exception {
        Collection<String> terminatedHosts = sut.getTerminatedHosts(Collections.emptyList());
        assertArrayEquals(new String[] {}, terminatedHosts.toArray());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanInstanceLaunchGracePeriodOk() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{\"launchLatencyTh\": 300}"));

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(300L, gracePeriod);
    }

    @Test
    void testGetClusterInfoPublicIdsBeanInstanceLaunchGracePeriodNullResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse());

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(null, gracePeriod);
    }

    @Test
    void testGetClusterInfoPublicIdsBeanInstanceLaunchGracePeriodNoLaunchLatencyTh()
            throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        Long gracePeriod = sut.getClusterInstanceLaunchGracePeriod(TEST_CLUSTER);
        assertEquals(null, gracePeriod);
    }

    @Test
    void testGetEc2TagsOk() throws Exception {
        String responseBody = "{\"i-001\": {\"Name\": \"test-instance\"}}";
        mockWebServer.enqueue(new MockResponse().setBody(responseBody));

        Map<String, Map<String, String>> ec2Tags = sut.getEc2Tags(HOST_IDS);
        assertEquals("test-instance", ec2Tags.get("i-001").get("Name"));
    }

    @Test
    void testGetEc2TagsEmptyResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        Map<String, Map<String, String>> ec2Tags = sut.getEc2Tags(HOST_IDS);
        assertTrue(ec2Tags.isEmpty());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanOk() throws Exception {
        String responseBody = "{\"accountId\": \"accountId1\", \"region\": \"region1\"}";
        mockWebServer.enqueue(
                new MockResponse()
                        .setBody(responseBody)
                        .setHeader("Content-Type", "application/json"));

        ClusterInfoPublicIdsBean cluster = sut.getClusterInfoPublicIdsBean(TEST_CLUSTER);

        assertEquals("accountId1", cluster.getAccountId());
        assertEquals("region1", cluster.getRegion());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanNotFound() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(
                ClientErrorException.class, () -> sut.getClusterInfoPublicIdsBean(TEST_CLUSTER));
    }

    @Test
    void testCreateClusterWithEnvPublicIdsOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        ClusterInfoPublicIdsBean bean = new ClusterInfoPublicIdsBean();
        assertDoesNotThrow(
                () -> {
                    sut.createClusterWithEnvPublicIds("testCluster", "testEnv", "testStage", bean);
                });

        // Validate HTTP request details if needed
    }

    @Test
    void testCreateClusterWithEnvPublicIdsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        ClusterInfoPublicIdsBean bean = new ClusterInfoPublicIdsBean();
        assertThrows(
                ClientErrorException.class,
                () -> {
                    sut.createClusterWithEnvPublicIds("testCluster", "testEnv", "testStage", bean);
                });
    }

    @Test
    void testUpdateClusterWithPublicIdsOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        ClusterInfoPublicIdsBean bean = new ClusterInfoPublicIdsBean();
        assertDoesNotThrow(
                () -> {
                    sut.updateClusterWithPublicIds("testCluster", bean);
                });
    }

    @Test
    void testUpdateClusterWithPublicIdsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        ClusterInfoPublicIdsBean bean = new ClusterInfoPublicIdsBean();
        assertThrows(
                ClientErrorException.class,
                () -> {
                    sut.updateClusterWithPublicIds("testCluster", bean);
                });
    }

    @Test
    void testUpdateClusterCapacityOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        assertDoesNotThrow(() -> sut.updateClusterCapacity(TEST_CLUSTER, 1, 2));
    }

    @Test
    void testUpdateClusterCapacityClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        assertThrows(
                ClientErrorException.class, () -> sut.updateClusterCapacity(TEST_CLUSTER, 1, 2));
    }

    @Test
    void testGetClusterInfoPublicIdsBeanScalingPoliciesOk() throws Exception {
        // Minimal JSON, all lists present
        String responseBody =
                "{\"scalingPolicies\":[],\"scaleupPolicies\":[],\"scaledownPolicies\":[]}";
        mockWebServer.enqueue(new MockResponse().setBody(responseBody));

        RodimusAutoScalingPolicies result = sut.getClusterScalingPolicies(TEST_CLUSTER);
        assertTrue(result.allSimplePolicies().isEmpty());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanScalingPoliciesClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        assertThrows(ClientErrorException.class, () -> sut.getClusterScalingPolicies(TEST_CLUSTER));
    }

    @Test
    void testGetClusterInfoPublicIdsBeanAlarmsOk() throws Exception {
        String responseBody =
                "[{\"alarmId\":\"alarm1\",\"scalingPolicies\":[],\"alarmActions\":[],\"metricSource\":\"cpu\",\"comparator\":\"gt\",\"actionType\":\"scaleUp\",\"groupName\":\"cluster1\",\"threshold\":80.0,\"evaluationTime\":1,\"fromAwsMetric\":false}]";
        mockWebServer.enqueue(new MockResponse().setBody(responseBody));
        List<RodimusAutoScalingAlarm> alarms = sut.getClusterAlarms(TEST_CLUSTER);

        assertEquals(1, alarms.size());
        assertEquals("alarm1", alarms.get(0).getAlarmId());
        assertEquals("cpu", alarms.get(0).getMetricSource());
        assertEquals("scaleUp", alarms.get(0).getActionType());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanAlarmsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        assertThrows(ClientErrorException.class, () -> sut.getClusterAlarms(TEST_CLUSTER));
    }

    @Test
    void testGetClusterInfoPublicIdsBeanScheduledActionsOk() throws Exception {
        String responseBody =
                "[{\"clusterName\":\"cluster1\",\"actionId\":\"action1\",\"schedule\":\"cron(0 18 * * ? *)\",\"capacity\":2}]";
        mockWebServer.enqueue(new MockResponse().setBody(responseBody));
        List<RodimusScheduledAction> actions = sut.getClusterScheduledActions(TEST_CLUSTER);

        assertEquals(1, actions.size());
        assertEquals("action1", actions.get(0).getActionId());
        assertEquals("cron(0 18 * * ? *)", actions.get(0).getSchedule());
        assertEquals(2, actions.get(0).getCapacity());
    }

    @Test
    void testGetClusterInfoPublicIdsBeanScheduledActionsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(403));
        assertThrows(
                ClientErrorException.class, () -> sut.getClusterScheduledActions(TEST_CLUSTER));
    }

    @Test
    void testDeleteClusterScalingPolicyOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        assertDoesNotThrow(() -> sut.deleteClusterScalingPolicy(TEST_CLUSTER, "policy1"));
    }

    @Test
    void testDeleteClusterScalingPolicyClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        assertThrows(
                ClientErrorException.class,
                () -> sut.deleteClusterScalingPolicy(TEST_CLUSTER, "policy1"));
    }

    @Test
    void testPostClusterScalingPoliciesOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        RodimusAutoScalingPolicies policies = new RodimusAutoScalingPolicies();
        assertDoesNotThrow(() -> sut.postClusterScalingPolicies(TEST_CLUSTER, policies));
    }

    @Test
    void testPostClusterScalingPoliciesClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        RodimusAutoScalingPolicies policies = new RodimusAutoScalingPolicies();
        assertThrows(
                ClientErrorException.class,
                () -> sut.postClusterScalingPolicies(TEST_CLUSTER, policies));
    }

    @Test
    void testDeleteClusterAlarmOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        assertDoesNotThrow(() -> sut.deleteClusterAlarm(TEST_CLUSTER, "alarm1"));
    }

    @Test
    void testDeleteClusterAlarmClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        assertThrows(
                ClientErrorException.class, () -> sut.deleteClusterAlarm(TEST_CLUSTER, "alarm1"));
    }

    @Test
    void testCreateClusterAlarmsOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        RodimusAutoScalingAlarm alarm = new RodimusAutoScalingAlarm();
        alarm.setAlarmId("alarm1");
        alarm.setMetricSource("cpu");
        alarm.setActionType("scaleUp");
        alarm.setGroupName("cluster1");
        alarm.setThreshold(90.0);
        alarm.setEvaluationTime(1);
        alarm.setFromAwsMetric(false);

        assertDoesNotThrow(
                () -> sut.createClusterAlarms(TEST_CLUSTER, Collections.singletonList(alarm)));
    }

    @Test
    void testCreateClusterAlarmsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        RodimusAutoScalingAlarm alarm = new RodimusAutoScalingAlarm();
        alarm.setAlarmId("alarm1");
        alarm.setMetricSource("cpu");
        alarm.setActionType("scaleUp");
        alarm.setGroupName("cluster1");
        alarm.setThreshold(90.0);
        alarm.setEvaluationTime(1);
        alarm.setFromAwsMetric(false);

        assertThrows(
                ClientErrorException.class,
                () -> sut.createClusterAlarms(TEST_CLUSTER, Collections.singletonList(alarm)));
    }

    @Test
    void testDeleteClusterScheduledActionOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        assertDoesNotThrow(() -> sut.deleteClusterScheduledAction(TEST_CLUSTER, "action1"));
    }

    @Test
    void testDeleteClusterScheduledActionClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        assertThrows(
                ClientErrorException.class,
                () -> sut.deleteClusterScheduledAction(TEST_CLUSTER, "action1"));
    }

    @Test
    void testPostClusterScheduledActionsOk() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        RodimusScheduledAction action = new RodimusScheduledAction();
        action.setClusterName(TEST_CLUSTER);
        action.setActionId("action1");
        action.setSchedule("cron(0 20 * * ? *)");
        action.setCapacity(2);

        assertDoesNotThrow(
                () ->
                        sut.postClusterScheduledActions(
                                TEST_CLUSTER, Collections.singletonList(action)));
    }

    @Test
    void testPostClusterScheduledActionsClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));
        RodimusScheduledAction action = new RodimusScheduledAction();
        action.setClusterName(TEST_CLUSTER);
        action.setActionId("action1");
        action.setSchedule("cron(0 20 * * ? *)");
        action.setCapacity(2);

        assertThrows(
                ClientErrorException.class,
                () ->
                        sut.postClusterScheduledActions(
                                TEST_CLUSTER, Collections.singletonList(action)));
    }

    static class ServerErrorDispatcher extends Dispatcher {
        @Override
        public MockResponse dispatch(RecordedRequest request) {
            return new MockResponse().setResponseCode(500);
        }
    }
}
