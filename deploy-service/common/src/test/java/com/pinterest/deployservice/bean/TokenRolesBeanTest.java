/**
 * Copyright (c) 2016-2026 Pinterest, Inc.
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
package com.pinterest.deployservice.bean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for BUG-285640.
 *
 * <p>The {@code token} field on {@link TokenRolesBean} carries a bearer secret that the auth filter
 * compares verbatim. It must never appear in any HTTP response (only the one-shot {@link
 * CreatedTokenRolesResponse} returned by the creation endpoint may include it) and must never leak
 * into log output via {@code toString()}.
 */
public class TokenRolesBeanTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static TokenRolesBean populatedBean() {
        TokenRolesBean bean = new TokenRolesBean();
        bean.setScript_name("ci-runner");
        bean.setResource_id("test-env");
        bean.setResource_type(AuthZResource.Type.ENV);
        bean.setRole(TeletraanPrincipalRole.OPERATOR);
        bean.setExpire_date(1_700_000_000_000L);
        bean.setToken("super-secret-bearer-value");
        return bean;
    }

    @Test
    void tokenIsNeverSerializedToJson() throws Exception {
        String json = MAPPER.writeValueAsString(populatedBean());
        JsonNode root = MAPPER.readTree(json);

        assertFalse(
                root.has("token"),
                "TokenRolesBean must not serialize the `token` field; got: " + json);
        assertFalse(
                json.contains("super-secret-bearer-value"),
                "Raw token value leaked into JSON: " + json);

        // The other fields must continue to serialize as before, so we don't break clients.
        assertEquals("ci-runner", root.path("name").asText());
        assertEquals("test-env", root.path("resource").asText());
        assertEquals("ENV", root.path("type").asText());
        assertEquals("OPERATOR", root.path("role").asText());
        assertEquals(1_700_000_000_000L, root.path("expireDate").asLong());
    }

    @Test
    void tokenCanStillBeDeserializedFromInboundJson() throws Exception {
        String inbound =
                "{\"name\":\"ci-runner\",\"resource\":\"test-env\",\"type\":\"ENV\","
                        + "\"role\":\"OPERATOR\",\"token\":\"inbound-secret\","
                        + "\"expireDate\":1700000000000}";

        TokenRolesBean bean = MAPPER.readValue(inbound, TokenRolesBean.class);

        // Deserialization remains symmetric so existing service-internal code paths
        // (e.g. PUT update flows) keep working; only the serialization side is redacted.
        assertEquals("ci-runner", bean.getScript_name());
        assertEquals("inbound-secret", bean.getToken());
        assertEquals(TeletraanPrincipalRole.OPERATOR, bean.getRole());
    }

    @Test
    void toStringDoesNotIncludeTheTokenValue() {
        String rendered = populatedBean().toString();

        // ReflectionToStringBuilder formats every included field as "<name>=<value>", so the
        // sentinel "token=" is what we look for. We can't blanket-ban the substring "token"
        // because the class name itself (TokenRolesBean) contains it.
        assertFalse(
                rendered.contains("token="),
                "toString() must not include the token field; got: " + rendered);
        assertFalse(
                rendered.contains("super-secret-bearer-value"),
                "Raw token value leaked into toString(): " + rendered);
        // Non-secret fields should still be present so log output remains useful.
        assertTrue(rendered.contains("ci-runner"));
        assertTrue(rendered.contains("test-env"));
        assertTrue(rendered.contains("OPERATOR"));
    }

    @Test
    void createdTokenRolesResponseDoesExposeTokenExactlyOnce() throws Exception {
        String json =
                MAPPER.writeValueAsString(
                        new CreatedTokenRolesResponse(populatedBean(), "fresh-bearer"));
        JsonNode root = MAPPER.readTree(json);

        assertEquals("fresh-bearer", root.path("token").asText());
        assertEquals("ci-runner", root.path("name").asText());
        assertEquals("ENV", root.path("type").asText());
        assertEquals("OPERATOR", root.path("role").asText());
    }
}
