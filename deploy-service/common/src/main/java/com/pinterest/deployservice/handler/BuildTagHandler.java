/**
 * Copyright (c) 2016 Pinterest, Inc.
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

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.TagDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildTagHandler extends TagHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BuildTagHandler.class);
    private final TagDAO tagDAO;
    private final BuildDAO buildDAO;

    public BuildTagHandler(ServiceContext context) {
        this.tagDAO = context.getTagDAO();
        this.buildDAO = context.getBuildDAO();
    }

    @Override
    public TagBean createTag(TagBean tag, String operator) throws Exception {
        BuildBean build = this.buildDAO.getById(tag.getTarget_id());
        if (build != null) {
            tag.setTarget_id(build.getBuild_name());
            tag.setId(CommonUtils.getBase64UUID());
            tag.setTarget_type(TagTargetType.BUILD);
            tag.serializeTagMetaInfo(build);
            tag.setOperator(operator);
            tag.setCreated_date(System.currentTimeMillis());

            tagDAO.insert(tag);
            LOG.info(
                    "Successfully tagged {} on build {} by {}. Tag id is {}",
                    tag.getValue(),
                    tag.getTarget_id(),
                    tag.getOperator(),
                    tag.getId());

            return tagDAO.getById(tag.getId());
        } else {
            throw new DeployInternalException("Cannot find build {}", tag.getTarget_id());
        }
    }
}
