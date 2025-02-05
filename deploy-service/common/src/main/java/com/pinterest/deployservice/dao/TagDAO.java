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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.TagBean;
import com.pinterest.deployservice.bean.TagTargetType;
import com.pinterest.deployservice.bean.TagValue;
import java.util.List;

public interface TagDAO {

    void insert(TagBean bean) throws Exception;

    void delete(String id) throws Exception;

    TagBean getById(String id) throws Exception;

    List<TagBean> getByTargetId(String target_id) throws Exception;

    List<TagBean> getByTargetIdAndType(String target_name, TagTargetType target_type)
            throws Exception;

    List<TagBean> getLatestByTargetIdAndType(
            String target_name, TagTargetType target_type, int size) throws Exception;

    List<TagBean> getByValue(TagValue value) throws Exception;

    TagBean getLatestByTargetId(String targetId) throws Exception;
}
