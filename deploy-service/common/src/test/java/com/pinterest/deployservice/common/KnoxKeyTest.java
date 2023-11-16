/**
 * Copyright 2022 Pinterest, Inc.
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

package com.pinterest.deployservice.common;

import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl;
import com.pinterest.deployservice.knox.Knox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.*;

public class KnoxKeyTest {

    private static enum Answer {
        NULL, EXCEPTION, ARRAY, LATENCY
    };

    private static String msgUnauthException = "HTTP request failed, status = 401, content = Unauthorized";
    private static String postAnswerTag = "{\"i-001\":{\"Name\": \"devapp-example1\"},\"i-002\":{\"Name\": \"devrestricted-example2\"}}";
    private static String postAnswerArray = "[\"i-001\",\"i-002\"]";
    private static String getAnswerValue = "{\"launchLatencyTh\": 10}";

    private RodimusManager rodimusManager = null;
    private Knox mockKnox;
    private HTTPClient mockHttpClient;
    private List<Answer> answerList;
    private byte[][] testKey = new byte[2][];
    private String postAnswerReturn = null;

    @Before
    public void setUp() throws Exception {
        // Load testKeys
        testKey[0] = "aaa".getBytes(); // auth error
        testKey[1] = "bbb".getBytes(); // auth ok

        // Create mock for Knox
        mockKnox = Mockito.mock(Knox.class);

        // Create mock for httpClient
        mockHttpClient = Mockito.mock(HTTPClient.class);

        rodimusManager = new RodimusManagerImpl("http://localhost", "teletraan:test", false, "", "");

        // Allocate answerList
        answerList = new ArrayList<Answer>();
    }

    // ### terminateHostsByClusterName tests ###

    @Test
    public void terminateHostsByClusterName_Ok() throws Exception {
        // All working as expected

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        when(this.mockHttpClient.delete(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class)))
            .thenAnswer(invocation -> this.deleteAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        try {
            this.rodimusManager.terminateHostsByClusterName("cluster", Collections.singletonList("i-001"));
        } catch (Exception e) {
            Assert.assertTrue("Unexpected exception: " + e, false);
        }

        final Answer[] expected = {Answer.NULL};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void terminateHostsByClusterName_ErrorOk() throws Exception {
        // Token does not work, refresh and retry, second try works

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[1]);

        when(this.mockHttpClient.delete(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.deleteAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
            this.rodimusManager.terminateHostsByClusterName("cluster", Collections.singletonList("i-001"));
        });
        Assert.assertTrue(exception.getMessage().contains(msgUnauthException));

        try {
            this.rodimusManager.terminateHostsByClusterName("cluster", Collections.singletonList("i-001"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final List<Answer> expected = Arrays.asList(Answer.EXCEPTION, Answer.NULL);
        assertArrayEquals(expected.toArray(), answerList.toArray());
    }

    @Test
    public void terminateHostsByClusterName_MultipleError() throws Exception {
        // Token does not work, refresh does not offer new token

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[0]);

        when(this.mockHttpClient.delete(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.deleteAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        for (int i = 1; i <= 2; i++) {
            Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
                this.rodimusManager.terminateHostsByClusterName("cluster", Collections.singletonList("i-001"));
            });

            Assert.assertTrue(exception.getMessage().contains(msgUnauthException));
        }

        final List<Answer> expected = Arrays.asList(Answer.EXCEPTION, Answer.EXCEPTION);
        assertArrayEquals( expected.toArray(), answerList.toArray());
    }

    // ### getTerminatedHosts tests ###

    @Test
    public void getTerminatedHosts_Ok() throws Exception {
        // All working as expected

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerArray;

        try {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            Assert.assertTrue("Unexpected exception: " + e, false);
        }

        final Answer[] expected = {Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getTerminatedHosts_ErrorOk() throws Exception {
        // Token does not work, refresh and retry, second try works

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[1]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerArray;

        Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
        });
        Assert.assertTrue(exception.getMessage().contains(msgUnauthException));

        try {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            Assert.assertTrue("Unexpected exception: " + e, false);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getTerminatedHosts_MultipleError() throws Exception {
        // Token does not work, refresh does not offer new token

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[0]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerArray;

        for (int i = 1; i <= 2; i++) {
            Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
                this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
            });

            Assert.assertTrue(exception.getMessage().contains(msgUnauthException));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### getClusterInstanceLaunchGracePeriod tests

    @Test
    public void getClusterInstanceLaunchGracePeriod_Ok() throws Exception {
        // All working as expected

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        when(this.mockHttpClient.get(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.getAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        long res = 0;
        try {
            res = this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
            Assert.assertEquals(res, (long) 10);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.LATENCY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getClusterInstanceLaunchGracePeriod_test() throws Exception {
        // getClusterInstanceLaunchGracePeriod
        // Token does not work, refresh and retry, second try works

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[1]);

        when(this.mockHttpClient.get(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.getAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerArray;

        Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
            this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
        });
        Assert.assertTrue(exception.getMessage().contains("HTTP request failed, status"));

        try {
            this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.LATENCY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getClusterInstanceLaunchGracePeriod_MultipleError() throws Exception {
        // getClusterInstanceLaunchGracePeriod
        // Token does not work, refresh does not offer new token

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[0]);

        when(this.mockHttpClient.get(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.getAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        for (int i = 1; i <= 2; i++) {
            Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
                this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
            });

            Assert.assertTrue(exception.getMessage().contains("HTTP request failed, status"));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### getEC2Tags tests ###

    @Test
    public void getEC2Tags_Ok() throws Exception {
        // All working as expected

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerTag;

        try {
            rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getEC2Tags_ErrorOk() throws Exception {
        // Token does not work, refresh and retry, second try works

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[1]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerTag;

        Map<String, Map<String, String>> res = null;

        Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
            this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
        });
        Assert.assertTrue(exception.getMessage().contains("HTTP request failed, status"));

        try {
            res = this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            Assert.assertTrue("Unexpected exception: " + e, false);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    public void getEC2Tags_MultipleError() throws Exception {
        // Token does not work, refresh does not offer new token

        when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0], this.testKey[0]);

        when(this.mockHttpClient.post(
                Mockito.any(String.class),
                Mockito.any(String.class),
                Mockito.any(Map.class),
                Mockito.any(Integer.class))).thenAnswer(invocation -> this.postAnswer(invocation));

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient);

        this.postAnswerReturn = this.postAnswerTag;

        for (int i = 1; i <= 2; i++) {
            Exception exception = Assert.assertThrows(DeployInternalException.class, () -> {
                this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
            });

            Assert.assertTrue(exception.getMessage().contains("HTTP request failed, status"));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // =======================================================
    // to test: methods: terminateHostsByClusterName         1
    //                   getTerminatedHosts                  2
    //                   getClusterInstanceLaunchGracePeriod 3
    //                   getEc2Tags                          4
    //          actions: token ok                                        1234
    //                     (everyday operation)
    //                   token error and retry ok                        1234
    //                     (expected behaviour when token changes)
    //                   token error and no new token given              1234
    //                     (do not try more than once with same token)
    //                   token error and new token does not work neither 1234
    //                     (do not try indefinitely)
    // =======================================================

    // ### HELPER METHODS ###

    private void mockClasses(RodimusManager rodimusMngr, Knox mokKnox, HTTPClient mokHttpClient) throws Exception {

        // Modify fsKnox to use our mock
        Field classKnox = rodimusMngr.getClass().getDeclaredField("fsKnox");
        classKnox.setAccessible(true);
        classKnox.set(rodimusMngr, mokKnox);
        classKnox.setAccessible(false);

        // Modify httpClient to use our mock
        Field classHttpClient = rodimusMngr.getClass().getDeclaredField("httpClient");
        classHttpClient.setAccessible(true);
        classHttpClient.set(rodimusMngr, mokHttpClient);
        classHttpClient.setAccessible(false);
    }

    private String getToken(Map<String, String> headers) {
        // Get token out of Map of headers

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            // DEBUG System.out.println("headers-> " + entry.getKey() + ":" +
            // entry.getValue());
            if (entry.getKey() == "Authorization")
                return entry.getValue();
        }
        return null;
    }

    private Object deleteAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "DELETE" answer method
        Object[] args = invocation.getArguments();

        Map<String, String> headers = (Map<String, String>) args[2];
        String token = getToken(headers);

        if (Objects.equals(token, "token bbb")) {
            this.answerList.add(Answer.NULL);
            return null;
        } else {
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

    private Object postAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "POST" answer method
        Object[] args = invocation.getArguments();

        Map<String, String> headers = (Map<String, String>) args[2];
        String token = getToken(headers);

        if (Objects.equals(token, "token bbb")) {
            this.answerList.add(Answer.ARRAY);
            return this.postAnswerReturn;
        } else {
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

    private Object getAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "GET" answer method
        Object[] args = invocation.getArguments();

        Map<String, String> headers = (Map<String, String>) args[3];
        String token = getToken(headers);

        if (Objects.equals(token, "token bbb")) {
            this.answerList.add(Answer.LATENCY);
            return this.getAnswerValue;
        } else {
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

}