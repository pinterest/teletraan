/*
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

package com.pinterest.deployservice.buildtags;


import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.TagDAO;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.mockito.Mockito.*;

public class BuildTagsManagerImplTest {

    @Test
    public void testgetEffectiveTag() throws Exception{
        BuildTagsManagerImpl manager = new BuildTagsManagerImpl(mock(TagDAO.class), mock(EnvironDAO.class));
        List<BuildBean> builds = genCommits(10,"build1","master",1000000);
        List<BuildTagBean> tagBeanList = new ArrayList<>();

        //Test when there is no tag
        Assert.assertNull(manager.getEffectiveBuildTag(tagBeanList,builds.get(0)));
        Assert.assertNull(manager.getEffectiveBuildTag(tagBeanList,builds.get(1)));

        //Tag on 0, 2, and 5
        BuildTagBean t0 = genBuildTagBean(builds.get(0), TagValue.BadBuild);
        BuildTagBean t2 = genBuildTagBean(builds.get(2), TagValue.BadBuild);
        BuildTagBean t5 = genBuildTagBean(builds.get(5), TagValue.BadBuild);
        tagBeanList.add(t0);
        tagBeanList.add(t2);
        tagBeanList.add(t5);

        Assert.assertEquals(t0.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(0)).getId());
        Assert.assertEquals(t0.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(1)).getId());
        Assert.assertEquals(t2.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(2)).getId());
        Assert.assertEquals(t2.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(3)).getId());
        Assert.assertEquals(t2.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(4)).getId());
        Assert.assertEquals(t5.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(5)).getId());
        Assert.assertEquals(t5.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(6)).getId());
        Assert.assertEquals(t5.getTag().getId(), manager.getEffectiveBuildTag(tagBeanList,builds.get(7)).getId());

    }

    @Test
    public void testGetEffectiveTagsWithBuilds() throws Exception{
        TagDAO tagDAO = mock(TagDAO.class);
        EnvironDAO environDAO = mock(EnvironDAO.class);
        BuildTagsManagerImpl manager = new BuildTagsManagerImpl(tagDAO, environDAO);

        //One repo with two labels
        List<BuildBean> builds1 = genCommits(10,"service_one","master",1000000);
        List<TagBean> tagBeanList1 = new ArrayList<>();
        tagBeanList1.add(genTagOnBuild(builds1.get(1), TagValue.BadBuild));
        tagBeanList1.add(genTagOnBuild(builds1.get(5), TagValue.GoodBuild));

        //Same repo different branches
        List<BuildBean> builds2 = genCommits(10,"service_one","hotfix",2000000);

        //Another repo with one label
        List<BuildBean> builds3 = genCommits(10,"service_two","master",3000000);
        List<TagBean> tagBeanList3 = new ArrayList<>();
        tagBeanList3.add(genTagOnBuild(builds3.get(9), TagValue.BadBuild));

        when(tagDAO.getByTargetName("service_one", TagTargetType.Build)).thenReturn(tagBeanList1);
        when(tagDAO.getByTargetName("service_two", TagTargetType.Build)).thenReturn(tagBeanList3);

        //Labels on service_one/master
        Assert.assertNull(manager.getEffectiveBuildTag(builds1.get(0)));
        Assert.assertEquals(TagValue.BadBuild, manager.getEffectiveBuildTag(builds1.get(1)).getValue());
        Assert.assertEquals(TagValue.BadBuild, manager.getEffectiveBuildTag(builds1.get(2)).getValue());
        Assert.assertEquals(TagValue.BadBuild, manager.getEffectiveBuildTag(builds1.get(4)).getValue());
        Assert.assertEquals(TagValue.GoodBuild, manager.getEffectiveBuildTag(builds1.get(5)).getValue());
        Assert.assertEquals(TagValue.GoodBuild, manager.getEffectiveBuildTag(builds1.get(6)).getValue());

        //Labels on service_one/hotfix
        Assert.assertNull(manager.getEffectiveBuildTag(builds2.get(1)));
        Assert.assertNull(manager.getEffectiveBuildTag(builds2.get(3)));
        Assert.assertNull(manager.getEffectiveBuildTag(builds2.get(5)));

        //Labels on service_two/master
        Assert.assertNull(manager.getEffectiveBuildTag(builds3.get(1)));
        Assert.assertNull(manager.getEffectiveBuildTag(builds3.get(8)));
        Assert.assertEquals(TagValue.BadBuild, manager.getEffectiveBuildTag(builds3.get(9)).getValue());
    }

    private List<BuildBean> genCommits(int count, String build_name,String branch, int baseCommit){
        List<BuildBean> ret = new ArrayList<>();
        long now = System.currentTimeMillis();
        for(int i=0;i<count;i++){
            BuildBean build = new BuildBean();
            build.setBuild_id(Integer.toString(i+baseCommit));
            build.setBuild_name(build_name);
            build.setCommit_date(now+i);
            build.setScm("git");
            build.setScm_branch(branch);
            build.setScm_commit(Integer.toString(i+baseCommit));
            ret.add(build);
        }
        return ret;
    }

    private BuildTagBean genBuildTagBean(BuildBean build, TagValue value){
        return new BuildTagBean(build, genTagOnBuild(build, value));
    }

    private TagBean genTagOnBuild(BuildBean build, TagValue value) {
        TagBean tag = new TagBean();
        tag.setId(CommonUtils.getBase64UUID());
        tag.setTarget_id(build.getBuild_id());
        tag.setTarget_type(TagTargetType.Build);
        tag.setValue(value);
        tag.setTarget_name(build.getBuild_name());
        tag.serializeTagMetaInfo(build);
        return tag;
    }
}
