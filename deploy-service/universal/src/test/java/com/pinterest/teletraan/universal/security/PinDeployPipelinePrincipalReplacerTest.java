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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.common.collect.ImmutableList;
import com.pinterest.teletraan.universal.security.bean.EnvoyCredentials;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import com.pinterest.teletraan.universal.security.bean.TeletraanPrincipal;
import com.pinterest.teletraan.universal.security.bean.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PinDeployPipelinePrincipalReplacerTest {
    private static final String PIPELINE_ID_1 = "pipeline123";
    private static final String PINDEPLOY_SPIFFE_1 = "spiffe://example.org/service1";
    private static final String PINDEPLOY_SPIFFE_2 = "spiffe://example.org/service2";
    private static final ImmutableList<String> pinDeploySpiffeIds =
            ImmutableList.of(PINDEPLOY_SPIFFE_1, PINDEPLOY_SPIFFE_2);
    private PinDeployPipelinePrincipalReplacer sut;

    @BeforeEach
    void setUp() {
        sut = new PinDeployPipelinePrincipalReplacer(pinDeploySpiffeIds);
    }

    @ParameterizedTest
    @ValueSource(strings = {PINDEPLOY_SPIFFE_1, PINDEPLOY_SPIFFE_2})
    void replaceShouldReturnModifiedPrincipalWhenConditionsMet(String spiffe) {
        ServicePrincipal principal = new ServicePrincipal(spiffe);
        EnvoyCredentials credentials = new EnvoyCredentials(null, spiffe, null, PIPELINE_ID_1);

        TeletraanPrincipal result = sut.replace(principal, credentials);

        assertInstanceOf(ServicePrincipal.class, result);
        assertEquals(spiffe + "/" + PIPELINE_ID_1, ((ServicePrincipal) result).getName());
    }

    @Test
    void replaceShouldReturnOriginalPrincipalWhenNotServicePrincipal() {
        TeletraanPrincipal principal = new UserPrincipal("user", null);
        EnvoyCredentials credentials = new EnvoyCredentials("user", null, null);

        TeletraanPrincipal result = sut.replace(principal, credentials);

        assertSame(principal, result);
    }

    @Test
    void replaceShouldReturnOriginalPrincipalWhenSpiffeIdNotInList() {
        String unknownSpiffe = "spiffe://example.org/unknown";
        ServicePrincipal principal = new ServicePrincipal(unknownSpiffe);
        EnvoyCredentials credentials =
                new EnvoyCredentials(null, unknownSpiffe, null, PIPELINE_ID_1);

        TeletraanPrincipal result = sut.replace(principal, credentials);

        assertSame(principal, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {PINDEPLOY_SPIFFE_1, PINDEPLOY_SPIFFE_2})
    void replaceShouldReturnOriginalPrincipalWhenPipelineIdIsEmpty(String spiffe) {
        ServicePrincipal principal = new ServicePrincipal(spiffe);
        EnvoyCredentials credentials = new EnvoyCredentials(null, spiffe, null, null);

        TeletraanPrincipal result = sut.replace(principal, credentials);

        assertSame(principal, result);
    }

    @Test
    void replaceShouldReturnOriginalPrincipalWhenPrincipalIsNull() {
        EnvoyCredentials credentials = new EnvoyCredentials(null, PINDEPLOY_SPIFFE_1, null, null);
        TeletraanPrincipal result = sut.replace(null, credentials);

        assertNull(result);
    }
}
