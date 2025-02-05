/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.BuildTagBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.TagDAO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildTagsManagerImplTest {

    @Test
    void testgetEffectiveTag() throws Exception {
        BuildTagsManagerImpl manager = new BuildTagsManagerImpl(mock(TagDAO.class));
        List<BuildBean> builds = genCommits(10, "build1", "master", 1000000);
        List<BuildTagBean> tagBeanList = new ArrayList<>();

        // Test when there is no tag
        assertNull(manager.getEffectiveBuildTag(tagBeanList, builds.get(0)));
        assertNull(manager.getEffectiveBuildTag(tagBeanList, builds.get(1)));

        // Tag on 0, 2, and 5
        BuildTagBean t0 = genBuildTagBean(builds.get(0), TagValue.BAD_BUILD);
        BuildTagBean t2 = genBuildTagBean(builds.get(2), TagValue.BAD_BUILD);
        BuildTagBean t5 = genBuildTagBean(builds.get(5), TagValue.BAD_BUILD);
        tagBeanList.add(t0);
        tagBeanList.add(t2);
        tagBeanList.add(t5);

        assertEquals(
                t0.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(0)).getId());
        assertEquals(
                t0.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(1)).getId());
        assertEquals(
                t2.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(2)).getId());
        assertEquals(
                t2.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(3)).getId());
        assertEquals(
                t2.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(4)).getId());
        assertEquals(
                t5.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(5)).getId());
        assertEquals(
                t5.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(6)).getId());
        assertEquals(
                t5.getTag().getId(),
                manager.getEffectiveBuildTag(tagBeanList, builds.get(7)).getId());
    }

    @Test
    void testGetEffectiveTagsWithBuilds() throws Exception {
        TagDAO tagDAO = mock(TagDAO.class);
        BuildTagsManagerImpl manager = new BuildTagsManagerImpl(tagDAO);

        // One repo with two labels
        List<BuildBean> builds1 = genCommits(10, "service_one", "master", 1000000);
        List<TagBean> tagBeanList1 = new ArrayList<>();
        tagBeanList1.add(genTagOnBuild(builds1.get(1), TagValue.BAD_BUILD));
        tagBeanList1.add(genTagOnBuild(builds1.get(5), TagValue.GOOD_BUILD));

        // Same repo different branches
        List<BuildBean> builds2 = genCommits(10, "service_one", "hotfix", 2000000);

        // Another repo with one label
        List<BuildBean> builds3 = genCommits(10, "service_two", "master", 3000000);
        List<TagBean> tagBeanList3 = new ArrayList<>();
        tagBeanList3.add(genTagOnBuild(builds3.get(9), TagValue.BAD_BUILD));

        when(tagDAO.getLatestByTargetIdAndType(
                        "service_one", TagTargetType.BUILD, BuildTagsManagerImpl.MAXCHECKTAGS))
                .thenReturn(tagBeanList1);
        when(tagDAO.getLatestByTargetIdAndType(
                        "service_two", TagTargetType.BUILD, BuildTagsManagerImpl.MAXCHECKTAGS))
                .thenReturn(tagBeanList3);

        // Labels on service_one/master
        assertNull(manager.getEffectiveBuildTag(builds1.get(0)));
        assertEquals(TagValue.BAD_BUILD, manager.getEffectiveBuildTag(builds1.get(1)).getValue());
        assertEquals(TagValue.BAD_BUILD, manager.getEffectiveBuildTag(builds1.get(2)).getValue());
        assertEquals(TagValue.BAD_BUILD, manager.getEffectiveBuildTag(builds1.get(4)).getValue());
        assertEquals(TagValue.GOOD_BUILD, manager.getEffectiveBuildTag(builds1.get(5)).getValue());
        assertEquals(TagValue.GOOD_BUILD, manager.getEffectiveBuildTag(builds1.get(6)).getValue());

        // Labels on service_one/hotfix
        assertNull(manager.getEffectiveBuildTag(builds2.get(1)));
        assertNull(manager.getEffectiveBuildTag(builds2.get(3)));
        assertNull(manager.getEffectiveBuildTag(builds2.get(5)));

        // Labels on service_two/master
        assertNull(manager.getEffectiveBuildTag(builds3.get(1)));
        assertNull(manager.getEffectiveBuildTag(builds3.get(8)));
        assertEquals(TagValue.BAD_BUILD, manager.getEffectiveBuildTag(builds3.get(9)).getValue());
    }

    @Test
    void testsSortAndDedupTags() throws Exception {
        List<BuildTagBean> tagBeanList = new ArrayList<>();
        List<BuildBean> builds = genCommits(10, "service_one", "master", 1000000);
        for (BuildBean build : builds) {
            tagBeanList.add(genBuildTagBean(build, TagValue.GOOD_BUILD));
        }

        // All good builds
        List<BuildTagBean> test = new ArrayList<>(tagBeanList);
        List<BuildTagBean> result = BuildTagsManagerImpl.sortAndDedupTags(test);
        assertEquals(10, result.size());
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                    result.get(i).getBuild().getCommit_date()
                            < result.get(i + 1).getBuild().getCommit_date());
        }

        // Tag BAD in the middle  build
        testWithBadbuild(tagBeanList, 0);
        testWithBadbuild(tagBeanList, 5);
        testWithBadbuild(tagBeanList, 9);
    }

    private void testWithBadbuild(List<BuildTagBean> input, int position) throws Exception {
        List<BuildTagBean> test = new ArrayList<>(input);
        BuildTagBean t = genBuildTagBean(input.get(position).getBuild(), TagValue.BAD_BUILD);
        test.add(t);

        List<BuildTagBean> result = BuildTagsManagerImpl.sortAndDedupTags(test);
        assertEquals(10, result.size());
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(
                    result.get(i).getBuild().getCommit_date()
                            < result.get(i + 1).getBuild().getCommit_date());
        }
        assertEquals(result.get(position).getTag().getValue(), TagValue.BAD_BUILD);
    }

    private List<BuildBean> genCommits(
            int count, String build_name, String branch, int baseCommit) {
        List<BuildBean> ret = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            BuildBean build = new BuildBean();
            build.setBuild_id(Integer.toString(i + baseCommit));
            build.setBuild_name(build_name);
            build.setCommit_date(now + i);
            build.setScm("git");
            build.setScm_branch(branch);
            build.setScm_commit(Integer.toString(i + baseCommit));
            ret.add(build);
        }
        return ret;
    }

    private BuildTagBean genBuildTagBean(BuildBean build, TagValue value) throws Exception {
        BuildTagBean ret = new BuildTagBean(build, genTagOnBuild(build, value));
        ret.getTag().setCreated_date(build.getCommit_date() + 10000);
        return ret;
    }

    private TagBean genTagOnBuild(BuildBean build, TagValue value) throws Exception {
        TagBean tag = new TagBean();
        tag.setId(CommonUtils.getBase64UUID());
        tag.setTarget_id(build.getBuild_name());
        tag.setTarget_type(TagTargetType.BUILD);
        tag.setValue(value);
        tag.serializeTagMetaInfo(build);
        return tag;
    }
}
