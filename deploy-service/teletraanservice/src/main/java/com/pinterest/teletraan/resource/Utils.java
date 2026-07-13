/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * T006 — Reject invisible / bidi / zero-width unicode characters in config string values.
     *
     * <p>These characters (U+200B..U+200F, U+2028, U+2029, U+FEFF) are commonly pasted from
     * rich-text editors and cause opaque 500s in downstream serialization without any actionable
     * signal for the user. Validate pre-persist and fail fast with 422 + structured WARN.
     */
    public static void rejectDisallowedUnicode(
            Map<String, String> configs, String principal, String resource) {
        if (configs == null) {
            return;
        }
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (isDisallowedInvisible(c)) {
                    LOG.warn(
                            "Config rejected — disallowed invisible unicode codepoint field={} codepoint=U+{} offset={} principal={} resource={}",
                            key,
                            String.format("%04X", (int) c),
                            i,
                            principal,
                            resource);
                    String msg =
                            String.format(
                                    "Config value for field '%s' contains disallowed invisible "
                                            + "character U+%04X at offset %d. Remove and retry.",
                                    key, (int) c, i);
                    throw new WebApplicationException(
                            msg, Response.status(422).entity(msg).build());
                }
            }
        }
    }

    private static boolean isDisallowedInvisible(char c) {
        // Zero-width + bidi + line/paragraph-separator + BOM
        return (c >= 0x200B && c <= 0x200F) // zero-width + LRM/RLM
                || c == 0x2028 // LINE SEPARATOR
                || c == 0x2029 // PARAGRAPH SEPARATOR
                || c == 0xFEFF; // BYTE ORDER MARK / zero-width no-break space
    }

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envName, String stageName)
            throws Exception {
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        if (environBean == null) {
            throw new WebApplicationException(
                    String.format("Environment %s/%s does not exist.", envName, stageName),
                    Response.Status.NOT_FOUND);
        }
        return environBean;
    }

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envId) throws Exception {
        EnvironBean environBean = environDAO.getById(envId);
        if (environBean == null) {
            throw new WebApplicationException(
                    String.format("Environment %s does not exist.", envId),
                    Response.Status.NOT_FOUND);
        }
        return environBean;
    }

    public static DeployBean getDeploy(DeployDAO deployDAO, String deployId) throws Exception {
        DeployBean deployBean = deployDAO.getById(deployId);
        if (deployBean == null) {
            throw new WebApplicationException(
                    String.format("Deploy %s does not exist.", deployId),
                    Response.Status.NOT_FOUND);
        }
        return deployBean;
    }

    public static void trimMapValues(Map<String, String> configs) throws Exception {

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            entry.setValue(entry.getValue().trim());
        }
    }
}
