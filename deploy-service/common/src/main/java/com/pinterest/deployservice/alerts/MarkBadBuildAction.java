/**
 * Copyright (c) 2017 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MarkBadBuildAction marks a build as bad */
public class MarkBadBuildAction extends AlertAction {

    public static final Logger LOG = LoggerFactory.getLogger(MarkBadBuildAction.class);

    @Override
    public Object perform(
            AlertContext context,
            EnvironBean environ,
            DeployBean lastDeploy,
            int actionWindowInSeconds,
            String operator)
            throws Exception {

        TagBean tagBean = new TagBean();
        tagBean.setTarget_id(lastDeploy.getBuild_id());
        tagBean.setTarget_type(TagTargetType.BUILD);
        tagBean.setValue(TagValue.BAD_BUILD);
        tagBean.setComments("Mark build as bad with alert trigger");
        context.getTagHandler().createTag(tagBean, operator);
        return tagBean;
    }
}
