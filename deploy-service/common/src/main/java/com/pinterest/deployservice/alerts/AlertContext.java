/**
 * Copyright (c) 2017-2025 Pinterest, Inc.
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
package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.handler.DeployHandlerInterface;
import com.pinterest.deployservice.handler.TagHandler;

/**
 * Context info passed to AlertAction, it should include all information AlertAction depends on. The
 * main purpose is for writing tests easier.
 */
public class AlertContext {
    private DeployHandlerInterface deployHandler;
    private TagHandler tagHandler;
    private DeployDAO deployDAO;

    public DeployDAO getDeployDAO() {
        return deployDAO;
    }

    public void setDeployDAO(DeployDAO deployDAO) {
        this.deployDAO = deployDAO;
    }

    public DeployHandlerInterface getDeployHandler() {
        return deployHandler;
    }

    public void setDeployHandler(DeployHandlerInterface deployHandler) {
        this.deployHandler = deployHandler;
    }

    public TagHandler getTagHandler() {
        return tagHandler;
    }

    public void setTagHandler(TagHandler tagHandler) {
        this.tagHandler = tagHandler;
    }
}
