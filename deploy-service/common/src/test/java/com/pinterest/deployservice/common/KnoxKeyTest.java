/**
 * Copyright (c) 2022-2023 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

@SuppressWarnings("unchecked")
class KnoxKeyTest {

    private static enum Answer {
        NULL,
        EXCEPTION,
        ARRAY,
        LATENCY
    }

    private static final String msgUnauthException =
            "HTTP request failed, status = 401, content = Unauthorized";
    private static final String postAnswerTag =
            "{\"i-001\":{\"Name\": \"devapp-example1\"},\"i-002\":{\"Name\": \"devrestricted-example2\"}}";
    private static final String postAnswerArray = "[\"i-001\",\"i-002\"]";

    private RodimusManager rodimusManager = null;
    private KnoxKeyReader mockKnoxKeyReader;
    private List<Answer> answerList;
    private String[] testKey = new String[2];

    @BeforeEach
    void setUp() throws Exception {
        // Load testKeys
        testKey[0] = "aaa"; // auth error
        testKey[1] = "bbb"; // auth ok

        // Create mock for Knox
        mockKnoxKeyReader = Mockito.mock(KnoxKeyReader.class);

        // Create mock for httpClient

        rodimusManager =
                new RodimusManagerImpl("http://localhost", "teletraan:test", false, "", "");

        // Allocate answerList
        answerList = new ArrayList<Answer>();
        mockClasses(rodimusManager, mockKnoxKeyReader);
    }




    @Test
    void terminateHostsByClusterName_MultipleError() {
        // Token does not work
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[0]);

        for (int i = 1; i <= 2; i++) {
            Exception exception =
                    assertThrows(
                            DeployInternalException.class,
                            () -> {
                                this.rodimusManager.terminateHostsByClusterName(
                                        "cluster", Collections.singletonList("i-001"));
                            });

            assertTrue(exception.getMessage().contains(msgUnauthException));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### getTerminatedHosts tests ###

    @Test
    void getTerminatedHosts_Ok() throws Exception {
        // All working as expected
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[1]);
        this.postAnswerReturn = postAnswerArray;

        try {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getTerminatedHosts_ErrorOk() throws Exception {
        // Token does not work, refresh and retry, second try works
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[1]);
        this.postAnswerReturn = postAnswerArray;

        Exception exception =
                assertThrows(
                        DeployInternalException.class,
                        () -> {
                            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
                        });
        assertTrue(exception.getMessage().contains(msgUnauthException));

        try {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getTerminatedHosts_MultipleError() {
        // Token does not work, refresh does not offer new token
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[0]);
        this.postAnswerReturn = postAnswerArray;

        for (int i = 1; i <= 2; i++) {
            Exception exception =
                    assertThrows(
                            DeployInternalException.class,
                            () -> {
                                this.rodimusManager.getTerminatedHosts(
                                        Arrays.asList("i-001", "i-002"));
                            });

            assertTrue(exception.getMessage().contains(msgUnauthException));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### getClusterInstanceLaunchGracePeriod tests

    @Test
    void getClusterInstanceLaunchGracePeriod_Ok() throws Exception {
        // All working as expected
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[1]);
        long res = 0;
        try {
            res = this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
            assertEquals(res, (long) 10);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.LATENCY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getClusterInstanceLaunchGracePeriod_test() throws Exception {
        // Token does not work, refresh and retry, second try works
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[1]);
        this.postAnswerReturn = postAnswerArray;

        Exception exception =
                assertThrows(
                        DeployInternalException.class,
                        () -> {
                            this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
                        });
        assertTrue(exception.getMessage().contains("HTTP request failed, status"));

        try {
            this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.LATENCY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getClusterInstanceLaunchGracePeriod_MultipleError() {
        // Token does not work, refresh does not offer new token
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[0]);

        for (int i = 1; i <= 2; i++) {
            Exception exception =
                    assertThrows(
                            DeployInternalException.class,
                            () -> {
                                this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
                            });

            assertTrue(exception.getMessage().contains("HTTP request failed, status"));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### getEC2Tags tests ###

    @Test
    void getEC2Tags_Ok() throws Exception {
        // All working as expected

        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[1]);
        this.postAnswerReturn = postAnswerTag;

        try {
            rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getEC2Tags_ErrorOk() throws Exception {
        // Token does not work, refresh and retry, second try works
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[1]);
        this.postAnswerReturn = postAnswerTag;

        Exception exception =
                assertThrows(
                        DeployInternalException.class,
                        () -> {
                            this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
                        });
        assertTrue(exception.getMessage().contains("HTTP request failed, status"));

        try {
            this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.ARRAY};
        assertArrayEquals(expected, answerList.toArray());
    }

    @Test
    void getEC2Tags_MultipleError() {
        // Token does not work, refresh does not offer new token
        when(this.mockKnoxKeyReader.getKey()).thenReturn(this.testKey[0], this.testKey[0]);
        this.postAnswerReturn = postAnswerTag;

        for (int i = 1; i <= 2; i++) {
            Exception exception =
                    assertThrows(
                            DeployInternalException.class,
                            () -> {
                                this.rodimusManager.getEc2Tags(Arrays.asList("i-001", "i-002"));
                            });

            assertTrue(exception.getMessage().contains("HTTP request failed, status"));
        }

        final Answer[] expected = {Answer.EXCEPTION, Answer.EXCEPTION};
        assertArrayEquals(expected, answerList.toArray());
    }

    // ### HELPER METHODS ###

    private void mockClasses(
            RodimusManager rodimusMngr, KnoxKeyReader mokKnox)
            throws Exception {
        // Modify fsKnox to use our mock
        Field classKnox = rodimusMngr.getClass().getDeclaredField("knoxKeyReader");
        classKnox.setAccessible(true);
        classKnox.set(rodimusMngr, mokKnox);
        classKnox.setAccessible(false);
    }
}
