/**
 * Copyright (c) 2016-2018 Pinterest, Inc.
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

import com.pinterest.deployservice.bean.HostTagBean;
import com.pinterest.deployservice.bean.HostTagInfo;
import com.pinterest.deployservice.bean.UpdateStatement;
import java.util.List;

public interface HostTagDAO {
    void insertOrUpdate(HostTagBean hostTagBean) throws Exception;

    UpdateStatement genInsertOrUpdate(HostTagBean hostTagBean);

    HostTagBean get(String hostId, String key) throws Exception;

    void deleteAllByEnvId(String envId, String tagName) throws Exception;

    void deleteAllByEnvIdAndHostIds(String envId, List<String> hostIds) throws Exception;

    void deleteByHostId(String hostId) throws Exception;

    List<HostTagBean> getAllByEnvIdAndTagName(String envId, String tagName) throws Exception;

    List<HostTagInfo> getHostsByEnvIdAndTagName(String envId, String tagName) throws Exception;

    List<HostTagInfo> getHostsByEnvId(String envId) throws Exception;

    long countHostsByEnvIdAndTags(String envId, String tagName, List<String> tagValues)
            throws Exception;

    List<String> getAllPrerequisiteTagValuesByEnvIdAndTagName(
            String envId, String tagName, String tagValue) throws Exception;
}
