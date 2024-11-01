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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.pinterest.deployservice.common.DeployInternalException;

class RodimusManagerImplTest {
    private RodimusManagerImpl sut;
    private static MockWebServer mockWebServer;
    private String TEST_URL = "testUrl";

    @BeforeAll
    public static void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    public void setUpEach() throws Exception {
        sut = new RodimusManagerImpl(mockWebServer.url(TEST_URL).toString(), null, false, "", "");
    }

    @Test
    void nullKnoxKey_defaultKeyIsUsed() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("[]"));
        sut.getTerminatedHosts(Collections.singletonList("testHost"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("token defaultKeyContent", request.getHeader("Authorization"));
    }

    @Test
    void invalidKnoxKey_exceptionThrown() throws Exception {
        RodimusManagerImpl sut =
                new RodimusManagerImpl(
                        mockWebServer.url(TEST_URL).toString(),
                        "invalidRodimusKnoxKey",
                        false,
                        "",
                        "");
        assertThrows(
                IllegalStateException.class,
                () -> sut.getTerminatedHosts(Collections.singletonList("")));
    }

    @Test
    void terminateHostsByClusterName_Ok() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        try {
            sut.terminateHostsByClusterName(
                    "cluster", Collections.singletonList("i-001"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

        @Test
    void terminateHostsByClusterName_ErrorOk() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        Exception exception =
                assertThrows(
                        IOException.class,
                        () -> {
                            sut.terminateHostsByClusterName(
                                    "cluster", Collections.singletonList("i-001"));
                        });
        assertTrue(exception.getMessage().contains("unauthenticated"));
    }
}
