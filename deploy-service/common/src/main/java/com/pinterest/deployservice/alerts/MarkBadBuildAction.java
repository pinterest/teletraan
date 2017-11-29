package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MarkBadBuildAction marks a build as bad
 */
public class MarkBadBuildAction extends AlertAction {

  public final static Logger LOG = LoggerFactory.getLogger(MarkBadBuildAction.class);

  @Override
  public Object perform( AlertContext context,EnvironBean environ, DeployBean lastDeploy, int actionWindowInSeconds,
                        String operator) throws Exception {

    TagBean tagBean = new TagBean();
    tagBean.setTarget_id(lastDeploy.getBuild_id());
    tagBean.setTarget_type(TagTargetType.BUILD);
    tagBean.setValue(TagValue.BAD_BUILD);
    tagBean.setComments("Mark build as bad with alert trigger");
    context.getTagHandler().createTag(tagBean, operator);
    return tagBean;
  }
}
