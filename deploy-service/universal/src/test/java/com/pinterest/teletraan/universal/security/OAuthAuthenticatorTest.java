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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OAuthAuthenticatorTest {
    private static final String GROUP_PATH = "/group/";
    private static final String USER_PATH = "/user/";
    private static final String USER_NAME = "devinlundberg";
    private static final String EXAMPLE_URL = "http://example.com/";
    private static final String EXAMPLE_BAD_URL = "http // as";
    private static final String USER_DATA =
            String.join(
                    "",
                    "{\"user\": {",
                    "\"username\": \"devinlundberg\",",
                    "\"first_name\": \"Devin\",",
                    "\"last_name\": \"Lundberg\",",
                    "\"pinterest_url\": \"http://www.pinterest.com/devin60070\",",
                    "\"manager\": \"uid=raj,ou=people,dc=pinterest,dc=com\",",
                    "\"department\": \"2120 Technical Operations\",",
                    "\"employee_type\": \"fulltime\",",
                    "\"email\": \"devinlundberg@pinterest.com\"}}");
    private static final String GROUP_DATA = "{\"groups\": [\"g1\", \"g2\"]}";
    private static final List<String> GROUPS = Arrays.asList("g1", "g2");
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @ParameterizedTest
    @MethodSource("invalidConstructorArguments")
    void testConstructor_invalidArguments(
            String userDataUrl, String groupDataUrl, Class<Exception> expectedException) {
        assertThrows(expectedException, () -> new OAuthAuthenticator(userDataUrl, groupDataUrl));
    }

    @ParameterizedTest
    @MethodSource("validConstructorArguments")
    void testConstructor_validArguments(String userDataUrl, String groupDataUrl) {
        assertDoesNotThrow(() -> new OAuthAuthenticator(userDataUrl, groupDataUrl));
    }

    @Test
    void testAuthenticate_failure() throws MalformedURLException {
        OAuthAuthenticator sut =
                new OAuthAuthenticator(mockWebServer.url(USER_PATH).toString(), null);
        mockWebServer.enqueue(new MockResponse().setBody("random body"));
        Optional<UserPrincipal> userPrincipal = sut.authenticate("");
        assertFalse(userPrincipal.isPresent());
    }

    @Test
    void testAuthenticate_withValidUserData() throws MalformedURLException {
        OAuthAuthenticator sut =
                new OAuthAuthenticator(mockWebServer.url(USER_PATH).toString(), null);
        mockWebServer.enqueue(new MockResponse().setBody(USER_DATA));
        Optional<UserPrincipal> userPrincipal = sut.authenticate("");
        assertTrue(userPrincipal.isPresent());
        assertTrue(userPrincipal.get().getGroups().isEmpty());
        assertEquals(USER_NAME, userPrincipal.get().getName());
    }

    @Test
    void testAuthenticate_withValidUserDataAndGroupData() throws MalformedURLException {
        OAuthAuthenticator sut =
                new OAuthAuthenticator(
                        mockWebServer.url(USER_PATH).toString(),
                        mockWebServer.url(GROUP_PATH).toString());
        mockWebServer.enqueue(new MockResponse().setBody(USER_DATA));
        mockWebServer.enqueue(new MockResponse().setBody(GROUP_DATA));
        Optional<UserPrincipal> userPrincipal = sut.authenticate("");
        assertTrue(userPrincipal.isPresent());
        assertEquals(GROUPS, userPrincipal.get().getGroups());
    }

    @Test
    void testAuthenticate_withValidUserDataAndInvalidGroupData() throws MalformedURLException {
        OAuthAuthenticator sut =
                new OAuthAuthenticator(
                        mockWebServer.url(USER_PATH).toString(),
                        mockWebServer.url(GROUP_PATH).toString());
        mockWebServer.enqueue(new MockResponse().setBody(USER_DATA));
        mockWebServer.enqueue(new MockResponse().setBody("bad data"));
        Optional<UserPrincipal> userPrincipal = sut.authenticate("");
        assertTrue(userPrincipal.isPresent());
        assertTrue(userPrincipal.get().getGroups().isEmpty());
    }

    static Stream<Arguments> invalidConstructorArguments() {
        return Stream.of(
                Arguments.of(null, null, IllegalArgumentException.class),
                Arguments.of(" ", null, IllegalArgumentException.class),
                Arguments.of(EXAMPLE_BAD_URL, null, MalformedURLException.class),
                Arguments.of(EXAMPLE_URL, EXAMPLE_BAD_URL, MalformedURLException.class),
                Arguments.of(EXAMPLE_URL, EXAMPLE_BAD_URL, MalformedURLException.class));
    }

    static Stream<Arguments> validConstructorArguments() {
        return Stream.of(
                Arguments.of(EXAMPLE_URL, EXAMPLE_URL),
                Arguments.of(EXAMPLE_URL, null),
                Arguments.of(EXAMPLE_URL, " "));
    }
}
