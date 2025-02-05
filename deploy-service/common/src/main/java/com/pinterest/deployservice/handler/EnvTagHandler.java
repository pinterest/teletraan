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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.TagDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvTagHandler extends TagHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EnvTagHandler.class);
    private static final String DEFAULT_TARGET_ID = "TELETRAAN";
    private final TagDAO tagDAO;

    public EnvTagHandler(ServiceContext serviceContext) {
        this.tagDAO = serviceContext.getTagDAO();
    }

    @Override
    public TagBean createTag(TagBean tagBean, String operator) throws Exception {
        if (tagBean.getTarget_type() == TagTargetType.TELETRAAN) {
            tagBean.setTarget_id(DEFAULT_TARGET_ID);
        }
        tagBean.setId(CommonUtils.getBase64UUID());
        tagBean.setOperator(operator);
        tagBean.setCreated_date(System.currentTimeMillis());
        tagDAO.insert(tagBean);
        LOG.info(
                String.format(
                        "Successfully tagged %s on Env %s by %s. Tag id is %s",
                        tagBean.getValue(), tagBean.getTarget_id(), operator, tagBean.getId()));
        return tagDAO.getById(tagBean.getId());
    }
}
