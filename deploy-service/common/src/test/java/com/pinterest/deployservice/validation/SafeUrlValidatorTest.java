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
package com.pinterest.deployservice.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SafeUrlValidatorTest {

    private SafeUrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SafeUrlValidator();
    }

    @Test
    void nullUrl_isValid() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void blankUrl_isValid() {
        assertTrue(validator.isValid("", null));
    }

    @Test
    void httpsPublicUrl_isValid() {
        assertTrue(validator.isValid("https://metrics.example.com/api/v1/query", null));
    }

    @Test
    void httpUrl_isInvalid() {
        assertFalse(validator.isValid("http://metrics.example.com/api/v1/query", null));
    }

    @Test
    void localhost_isInvalid() {
        assertFalse(validator.isValid("https://localhost/path", null));
    }

    @Test
    void loopbackIp_isInvalid() {
        assertFalse(validator.isValid("https://127.0.0.1/path", null));
    }

    @Test
    void privateIp10_isInvalid() {
        assertFalse(validator.isValid("https://10.0.0.1/path", null));
    }

    @Test
    void privateIp172_isInvalid() {
        assertFalse(validator.isValid("https://172.16.0.1/path", null));
    }

    @Test
    void privateIp192_isInvalid() {
        assertFalse(validator.isValid("https://192.168.1.1/path", null));
    }

    @Test
    void linkLocalIp_isInvalid() {
        assertFalse(validator.isValid("https://169.254.169.254/latest/meta-data/", null));
    }

    @Test
    void internalHostname_isInvalid() {
        assertFalse(validator.isValid("https://service.internal/api", null));
    }

    @Test
    void localHostname_isInvalid() {
        assertFalse(validator.isValid("https://myhost.local/api", null));
    }

    @Test
    void malformedUrl_isInvalid() {
        assertFalse(validator.isValid("not a url", null));
    }

    @Test
    void ftpScheme_isInvalid() {
        assertFalse(validator.isValid("ftp://example.com/file", null));
    }

    @Test
    void fileScheme_isInvalid() {
        assertFalse(validator.isValid("file:///etc/passwd", null));
    }

    @Test
    void noHost_isInvalid() {
        assertFalse(validator.isValid("https:///path", null));
    }

    @Test
    void ipv6Loopback_isInvalid() {
        assertFalse(validator.isValid("https://[::1]/path", null));
    }
}
