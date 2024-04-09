/**
 * Copyright 2016 Pinterest, Inc.
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

import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envName,
        String stageName) throws Exception {
        EnvironBean environBean = environDAO.getByStage(envName, stageName);
        if (environBean == null) {
            throw new WebApplicationException(String.format("Environment %s/%s does not exist.", envName, stageName),
                    Response.Status.NOT_FOUND);
        }
        return environBean;
    }

    public static EnvironBean getEnvStage(EnvironDAO environDAO, String envId) throws Exception {
        EnvironBean environBean = environDAO.getById(envId);
        if (environBean == null) {
            throw new WebApplicationException(String.format("Environment %s does not exist.", envId),
                    Response.Status.NOT_FOUND);
        }
        return environBean;
    }

    public static DeployBean getDeploy(DeployDAO deployDAO, String deployId) throws Exception {
        DeployBean deployBean = deployDAO.getById(deployId);
        if (deployBean == null) {
            throw new WebApplicationException(String.format("Deploy %s does not exist.", deployId),
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
