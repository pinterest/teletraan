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
package com.pinterest.deployservice.rodimus;


import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RodimusManager {
    void terminateHostsByClusterName(String clusterName, Collection<String> hostIds) throws Exception;

    void terminateHostsByClusterName(String clusterName, Collection<String> hostIds, Boolean replaceHost)
    throws Exception;

    Collection<String> getTerminatedHosts(Collection<String> hostIds) throws Exception;

    Long getClusterInstanceLaunchGracePeriod(String clusterName) throws Exception;

    Map<String, Map<String, String>> getEc2Tags(Collection<String> hostIds) throws Exception;

    Long getClusterCapacity(String clusterName) throws Exception;
}
