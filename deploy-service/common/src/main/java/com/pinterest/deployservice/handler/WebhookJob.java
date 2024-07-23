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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.WebHookBean;
import com.pinterest.deployservice.common.HTTPClient;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class WebhookJob implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(WebhookJob.class);
    private List<WebHookBean> webhooks;
    private DeployBean deployBean;
    private EnvironBean envBean;
    private HTTPClient httpClient;
    private final int RETRIES = 3;

    public WebhookJob(List<WebHookBean> webhooks, DeployBean deployBean, EnvironBean envBean) {
        this.webhooks = webhooks;
        this.deployBean = deployBean;
        this.envBean = envBean;
        httpClient = new HTTPClient();
    }

    public Void call() {
        for (WebHookBean webhook : webhooks) {
            // TODO use method, version and headers, use jersey or apache http client
            // TODO we transform $TELETRAAN_NAME into the actual values, currently we support
            // $TELETRAAN_DEPLOY_ID, $TELETRAAN_DEPLOY_START, $TELETRAAN_NUMERIC_DEPLOY_STATE
            // We should support more such as $TELETRAAN_COMMIT, $TELETRAAN_ENV_NAME etc.
            String deployId = deployBean.getDeploy_id();
            String numericDeployState = String.valueOf(deployBean.getState().ordinal());
            String deployStart = String.valueOf(deployBean.getStart_date());
            String url = webhook.getUrl();
            url = url
                .replaceAll("\\$TELETRAAN_DEPLOY_ID", deployId)
                .replaceAll("\\$TELETRAAN_DEPLOY_START", deployStart)
                .replaceAll("\\$TELETRAAN_NUMERIC_DEPLOY_STATE", numericDeployState);
            LOG.info("Url after transform is {}", url);

            String headerString = webhook.getHeaders();
            if (!StringUtils.isEmpty(headerString)) {
                headerString = headerString
                    .replaceAll("\\$TELETRAAN_DEPLOY_ID", deployId)
                    .replaceAll("\\$TELETRAAN_DEPLOY_START", deployStart)
                    .replaceAll("\\$TELETRAAN_NUMERIC_DEPLOY_STATE", numericDeployState);
            }
            LOG.info("Header string after transform is {}", headerString);

            String bodyString = webhook.getBody();
            if (!StringUtils.isEmpty(bodyString)) {
                bodyString = bodyString
                    .replaceAll("\\$TELETRAAN_DEPLOY_ID", deployId)
                    .replaceAll("\\$TELETRAAN_DEPLOY_START", deployStart)
                    .replaceAll("\\$TELETRAAN_NUMERIC_DEPLOY_STATE", numericDeployState);
            }
            LOG.info("Body string after transform is {}", bodyString);

            Map<String, String> headers = null;
            if (!StringUtils.isEmpty(headerString)) {
                headers = Splitter.on(';').trimResults().withKeyValueSeparator(":").split(webhook.getHeaders());
            }

            String method = webhook.getMethod();
            if (StringUtils.isEmpty(method)) {
                method = "POST";
            }

            try {
                // Supports http GET, PUT, POST, DELETE
                if (method.equalsIgnoreCase("GET")) {
                    httpClient.get(url, null, null, headers, RETRIES);
                } else if (method.equalsIgnoreCase("POST")) {
                    httpClient.post(url, bodyString, headers, RETRIES);
                } else if (method.equalsIgnoreCase("PUT")) {
                    httpClient.put(url, bodyString, headers, RETRIES);
                } else if (method.equalsIgnoreCase("DELETE")) {
                    httpClient.delete(url, bodyString, headers, RETRIES);
                } else {
                    LOG.error("Current http method " + method + " is not supported!");
                }
                LOG.info("Successfully completed Webhook call " + url);
            } catch (Throwable t) {
                LOG.error("Failed to call webhook " + url, t);
            }
        }
        return null;
    }
}
