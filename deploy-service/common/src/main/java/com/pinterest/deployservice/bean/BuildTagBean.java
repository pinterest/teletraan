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
package com.pinterest.deployservice.bean;

/**
 * A wrapper class combining build and tag. It serves two purpose:
 *
 * <p>1. An in memory representation of a Tagging build.
 *
 * <p>2. The return result for the service endpoing /tags/builds. That is identical to /builds but
 * put the tagging info on the builds
 */
public class BuildTagBean {
    private TagBean tag;

    private BuildBean build;

    public BuildBean getBuild() {
        return build;
    }

    public void setBuild(BuildBean build) {
        this.build = build;
    }

    public TagBean getTag() {
        return tag;
    }

    public void setTag(TagBean tag) {
        this.tag = tag;
    }

    public BuildTagBean(BuildBean build, TagBean tag) {
        this.build = build;
        this.tag = tag;
    }

    public static BuildTagBean createFromTagBean(TagBean tag) throws Exception {
        BuildBean build = tag.deserializeTagMetaInfo(BuildBean.class);

        return new BuildTagBean(build, tag);
    }
}
