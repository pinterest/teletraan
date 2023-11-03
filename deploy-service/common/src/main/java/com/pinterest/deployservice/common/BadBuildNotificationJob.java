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
package com.pinterest.deployservice.common;


import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvType;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.buildtags.BuildTagsManager;
import com.pinterest.deployservice.buildtags.BuildTagsManagerImpl;
import com.pinterest.deployservice.dao.TagDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.handler.CommonHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.List;

public final class BadBuildNotificationJob implements Callable<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(BadBuildNotificationJob.class);
    private String build_name;
    private String operator;
    private String deployBoardUrlPrefix;
    private TagDAO tagDAO;
    private EnvironDAO environDAO;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private CommonHandler commonHandler;
    private BuildTagsManager manager;


    public BadBuildNotificationJob(String build_name, String operator, String deployBoardUrlPrefix, 
                    TagDAO tagDAO, EnvironDAO environDAO, DeployDAO deployDAO, BuildDAO buildDAO, CommonHandler commonHandler) {
        this.build_name = build_name;
        this.operator = operator;
        this.deployBoardUrlPrefix = deployBoardUrlPrefix;
        this.tagDAO = tagDAO;
        this.environDAO = environDAO;
        this.deployDAO = deployDAO;
        this.buildDAO = buildDAO;
        this.commonHandler = commonHandler;
        this.manager = new BuildTagsManagerImpl(tagDAO);
    }

    public Void call() {
        try {
            // get environs by build name
            List<EnvironBean> envBeans = this.environDAO.getByBuildName(this.build_name);
            
            for (EnvironBean environ : envBeans) {
                // Only notify for production environments
                if (environ.getStage_type() == EnvType.DEFAULT || environ.getStage_type() == EnvType.PRODUCTION) {
                    DeployBean deployBean = this.deployDAO.getById(environ.getDeploy_id());
                    BuildBean buildBean = this.buildDAO.getById(deployBean.getBuild_id());
                    TagBean tag = this.manager.getEffectiveBuildTag(buildBean);
                    if (tag.getValue() == TagValue.BAD_BUILD) {
                        String WebLink = this.deployBoardUrlPrefix + String.format("/env/%s/%s/deploy/", 
                                                environ.getEnv_name(), environ.getStage_name());
                        String message = String.format("Environment {} is running a build which is tagged as {} by Operator. For details: {}", 
                                                        environ.getEnv_name(), tag.getValue(), operator, WebLink);
                        if (!StringUtils.isEmpty(environ.getChatroom())) {
                            LOG.info(String.format("Send message to %s", environ.getChatroom()));
                            commonHandler.sendChatMessage(Constants.SYSTEM_OPERATOR, environ.getChatroom(), message, "yellow");
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.error(String.format("Failed to send bad build notifications for build {}", build_name), t);
        }
        return null;
    }
}
