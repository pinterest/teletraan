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
package com.pinterest.deployservice.buildtags;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pinterest.deployservice.bean.BuildBean;
import com.pinterest.deployservice.bean.BuildTagBean;
import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.dao.TagDAO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Manager class that is responsible for managing the build tags */
public class BuildTagsManagerImpl implements BuildTagsManager {

    private static final Logger LOG = LoggerFactory.getLogger(BuildTagsManagerImpl.class);
    private TagDAO tagDAO;

    public static final int MAXCHECKTAGS = 1000;

    private LoadingCache<String, List<BuildTagBean>> currentTags =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build(
                            new CacheLoader<String, List<BuildTagBean>>() {
                                public List<BuildTagBean> load(
                                        String buildName) { // no checked exception
                                    try {
                                        return createFromTagBean(
                                                tagDAO.getLatestByTargetIdAndType(
                                                        buildName,
                                                        TagTargetType.BUILD,
                                                        MAXCHECKTAGS));
                                    } catch (Exception ex) {
                                        LOG.error(ExceptionUtils.getRootCauseMessage(ex));
                                        LOG.error(ExceptionUtils.getStackTrace(ex));
                                    }
                                    return new ArrayList<BuildTagBean>();
                                }
                            });;

    public BuildTagsManagerImpl(TagDAO t) {
        this.tagDAO = t;
    }

    @Override
    public List<BuildTagBean> getEffectiveTagsWithBuilds(List<BuildBean> builds) throws Exception {
        if (builds == null || builds.size() == 0) return new ArrayList<>();

        List<BuildTagBean> ret = new ArrayList<>();

        for (BuildBean build : builds) {
            LOG.debug("Get effective tags for build name {}", build.getBuild_name());
            ret.add(new BuildTagBean(build, getEffectiveBuildTag(build)));
        }

        return ret;
    }

    @Override
    public TagBean getEffectiveBuildTag(BuildBean build) throws Exception {
        Preconditions.checkNotNull(build);
        Preconditions.checkNotNull(build.getBuild_name());
        return getEffectiveBuildTag(this.currentTags.get(build.getBuild_name()), build);
    }

    public TagBean getEffectiveBuildTag(List<BuildTagBean> tags, BuildBean build) throws Exception {
        // This is the current matching algorithm, it is a naive algorithm.
        // We have a list of tags sorted by the commit_date ascending. We have a specific commit.
        // The matching
        // works as following:
        // 1. Binary search finding out the position i of build in the tags list where
        // list[i-1]<=build.commit<list[i].
        // This means all i-1 tags were created before (or equal to commit).
        // 2. Start from i-1 to 0. Getting the first tag if it has the same git,repo,branch of the
        // build. Then it should
        // be the one applied. If there is no such i or 0, no tag can be applied.

        // Create a dummy so that we can call Collections.binarysearch
        BuildTagBean dummy = new BuildTagBean(build, null);

        LOG.debug("Search build {} in {} tags", build.getBuild_id(), tags.size());
        int insert =
                Collections.binarySearch(
                        tags,
                        dummy,
                        new Comparator<BuildTagBean>() {
                            @Override
                            public int compare(BuildTagBean o1, BuildTagBean o2) {
                                return o1.getBuild()
                                        .getCommit_date()
                                        .compareTo(o2.getBuild().getCommit_date());
                            }
                        });
        LOG.debug("Binary search returns {}", insert);

        // insert will be the (-(insertion point)-1) if not found. Insertion point is the index of
        // the first element that is greater or .size() if all
        // elements is smaller. This sounds a bit weird. It is basically to ensure negative return
        // when not finding the key.
        // https://docs.oracle.com/javase/8/docs/api/java/util/Collections.html#binarySearch-java.util.List-T-java.util.Comparator-
        // https://stackoverflow.com/questions/31104031/what-is-the-reasoning-behind-java-util-collections-binarysearch-return-value-wh
        int idx = insert < 0 ? -1 * (insert + 1) - 1 : insert; // This is our search start

        TagBean ret = null;
        for (int i = idx; i >= 0; i--) {
            BuildBean tagBuild = tags.get(i).getBuild();
            if (tagBuild.getScm_commit().equals(build.getScm_commit())) {
                // Tag is on the exact commit
                LOG.debug(
                        "Found tag {} on the exact commit {}",
                        tags.get(i).getTag().getValue(),
                        tagBuild.getScm_commit());
                ret = tags.get(i).getTag();
                break;
            } else if (tagBuild.getScm().equals(build.getScm())
                    && tagBuild.getScm_branch().equals(build.getScm_branch())) {
                ret = tags.get(i).getTag();
                break;
            }
        }
        return ret;
    }

    private static List<BuildTagBean> createFromTagBean(List<TagBean> tags) throws Exception {
        List<BuildTagBean> ret = new ArrayList<>();

        for (TagBean tag : tags) {
            ret.add(BuildTagBean.createFromTagBean(tag));
        }
        return sortAndDedupTags(ret);
    }

    public static List<BuildTagBean> sortAndDedupTags(List<BuildTagBean> buildTagBeanList)
            throws Exception {
        // Sort by commit date for later search
        Collections.sort(
                buildTagBeanList,
                new Comparator<BuildTagBean>() {
                    @Override
                    public int compare(BuildTagBean o1, BuildTagBean o2) {
                        if (!o1.getBuild()
                                .getCommit_date()
                                .equals(o2.getBuild().getCommit_date())) {
                            return o1.getBuild()
                                    .getCommit_date()
                                    .compareTo(o2.getBuild().getCommit_date());
                        } else {
                            // Same commit. sort by created date
                            return o1.getTag()
                                    .getCreated_date()
                                    .compareTo(o2.getTag().getCreated_date());
                        }
                    }
                });

        // In some cases, one commit can have multiple tags applied. Let's only use the latest
        // created
        List<BuildTagBean> dedup = new ArrayList<>(buildTagBeanList.size());
        BuildTagBean prev = null;
        for (BuildTagBean buildTagBean : buildTagBeanList) {
            if (prev == null
                    || !StringUtils.equals(
                            prev.getBuild().getScm_commit(),
                            buildTagBean.getBuild().getScm_commit())) {
                dedup.add(buildTagBean);
            } else {
                // Overwrite as we already sort by commit and tag created date above
                dedup.set(dedup.size() - 1, buildTagBean);
            }
            prev = buildTagBean;
        }
        return dedup;
    }
}
