## Definitions
### AgentBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|hostId||false|string||
|hostName||false|string||
|envId||false|string||
|deployId||false|string||
|deployStage||false|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)||
|state||false|enum (NORMAL, PAUSED_BY_SYSTEM, PAUSED_BY_USER, RESET, DELETE, UNREACHABLE, STOP)||
|status||false|enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH)||
|startDate||false|integer (int64)||
|lastUpdateDate||false|integer (int64)||
|lastOperator||false|string||
|lastErrno||false|integer (int32)||
|failCount||false|integer (int32)||
|firstDeploy||false|boolean|false|
|firstDeployDate||false|integer (int64)||
|stageStartDate||false|integer (int64)||


### AgentErrorBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|hostId||false|string||
|hostName||false|string||
|envId||false|string||
|errorMessage||false|string||


### BuildBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|name||false|string||
|artifactUrl||false|string||
|type||false|string||
|repo||false|string||
|branch||false|string||
|commit||false|string||
|commitShort||false|string||
|commitInfo||false|string||
|commitDate||false|integer (int64)||
|publishInfo||false|string||
|publisher||false|string||
|publishDate||false|integer (int64)||


### BuildTagBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|tag||false|TagBean||
|build||false|BuildBean||


### ChatMessageBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|from||false|string||
|to||false|string||
|message||false|string||


### CommitBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|sha||false|string||
|author||false|string||
|date||false|integer (int64)||
|title||false|string||
|message||false|string||
|info||false|string||


### ConfigHistoryBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|changeId||false|string||
|createTime||false|integer (int64)||
|operator||false|string||
|type||false|string||
|configChange||false|string||


### DeployBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|alias||false|string||
|state||false|enum (RUNNING, FAILING, SUCCEEDING, SUCCEEDED, ABORTED)||
|operator||false|string||
|description||false|string||
|total||false|integer (int32)||
|id||false|string||
|envId||false|string||
|buildId||false|string||
|type||false|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)||
|startDate||false|integer (int64)||
|lastUpdateDate||false|integer (int64)||
|successTotal||false|integer (int32)||
|failTotal||false|integer (int32)||
|successDate||false|integer (int64)||
|acceptanceStatus||false|enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED)||
|fromDeployId||false|string||


### DeployCandidatesResponse
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|candidates||false|PingResponseBean array||


### DeployConstraintBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|constraintKey||false|string||
|maxParallel||false|integer (int64)||
|state||false|enum (INIT, PROCESSING, ERROR, FINISHED)||
|startDate||false|integer (int64)||
|lastUpdate||false|integer (int64)||


### DeployGoalBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|deployId||false|string||
|deployType||false|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)||
|envId||false|string||
|envName||false|string||
|stageName||false|string||
|deployStage||false|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)||
|build||false|BuildBean||
|deployAlias||false|string||
|agentConfigs||false|object||
|scriptVariables||false|object||
|firstDeploy||false|boolean|false|
|isDocker||false|boolean|false|


### DeployProgressBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|agents||false|AgentBean array||
|missingHosts||false|string array||
|provisioningHosts||false|HostBean array||


### DeployQueryResultBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|deploys||false|DeployBean array||
|deployTags||false|object||
|total||false|integer (int64)||
|truncated||false|boolean|false|


### EnvWebHookBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|preDeployHooks||false|WebHookBean array||
|postDeployHooks||false|WebHookBean array||


### EnvironBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|envName||false|string||
|stageName||false|string||
|envState||false|enum (NORMAL, PAUSED, DISABLED)||
|description||false|string||
|buildName||false|string||
|branch||false|string||
|chatroom||false|string||
|deployId||false|string||
|deployType||false|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)||
|maxParallel||false|integer (int32)||
|priority||false|enum (NORMAL, HIGH, LOW, HIGHER, LOWER)||
|systemPriority||false|integer (int32)||
|stuckThreshold||false|integer (int32)||
|successThreshold||false|integer (int32)||
|advancedConfigId||false|string||
|scriptConfigId||false|string||
|lastOperator||false|string||
|lastUpdate||false|integer (int64)||
|acceptanceType||false|enum (AUTO, MANUAL)||
|emailRecipients||false|string||
|notifyAuthors||false|boolean|false|
|watchRecipients||false|string||
|metricsConfigId||false|string||
|alarmConfigId||false|string||
|webhooksConfigId||false|string||
|maxDeployNum||false|integer (int32)||
|maxDeployDay||false|integer (int32)||
|isDocker||false|boolean|false|
|maxParallelPct||false|integer (int32)||
|state||false|enum (NORMAL, DISABLED)||
|clusterName||false|string||
|maxParallelRp||false|integer (int32)||
|overridePolicy||false|enum (OVERRIDE, WARN)||
|scheduleId||false|string||
|deployConstraintId||false|string||
|stageType||true|enum (production, canary, development)|production|


### GroupRolesBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|name||false|string||
|resource||false|string||
|type||false|enum (ENV, GROUP, SYSTEM)||
|role||true|enum (READER, PINGER, PUBLISHER, OPERATOR, ADMIN)||


### HostBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|hostName||false|string||
|groupName||false|string||
|ip||false|string||
|hostId||false|string||
|createDate||false|integer (int64)||
|lastUpdateDate||false|integer (int64)||
|state||false|enum (PROVISIONED, ACTIVE, PENDING_TERMINATE, TERMINATING, TERMINATED)||
|canRetire||false|integer (int32)||


### HostTagInfo
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|hostName||false|string||
|hostId||false|string||
|tagName||false|string||
|tagValue||false|string||


### MetricsConfigBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|title||false|string||
|url||false|string||
|specs||false|MetricsSpecBean array||


### MetricsSpecBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|min||false|number (double)||
|max||false|number (double)||
|color||false|string||


### PingReportBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|deployId||false|string||
|envId||false|string||
|deployStage||false|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)||
|agentStatus||false|enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH)||
|errorCode||false|integer (int32)||
|errorMessage||false|string||
|failCount||false|integer (int32)||
|extraInfo||false|object||
|deployAlias||false|string||


### PingRequestBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|hostId||false|string||
|hostName||false|string||
|hostIp||false|string||
|groups||false|string array||
|reports||false|PingReportBean array||


### PingResponseBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|opCode||false|enum (NOOP, DEPLOY, RESTART, DELETE, WAIT, ROLLBACK, STOP)||
|deployGoal||false|DeployGoalBean||


### PromoteBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|type||false|enum (MANUAL, AUTO)||
|schedule||false|string||
|delay||false|integer (int32)||
|envId||false|string||
|lastOperator||false|string||
|lastUpdate||false|integer (int64)||
|predStage||false|string||
|queueSize||false|integer (int32)||
|disablePolicy||false|enum (MANUAL, AUTO)||
|failPolicy||false|enum (CONTINUE, DISABLE, ROLLBACK)||


### TagBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|value||false|enum (BAD_BUILD, GOOD_BUILD, ENABLE_ENV, DISABLE_ENV)||
|targetType||false|enum (BUILD, ENVIRONMENT, TELETRAAN)||
|targetId||false|string||
|operator||false|string||
|createdDate||false|integer (int64)||
|comments||false|string||
|metaInfo||false|string||


### TokenRolesBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|name||false|string||
|resource||false|string||
|type||false|enum (ENV, GROUP, SYSTEM)||
|token||false|string||
|role||true|enum (READER, PINGER, PUBLISHER, OPERATOR, ADMIN)||
|expireDate||false|integer (int64)||


### UserRolesBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|name||false|string||
|resource||false|string||
|type||false|enum (ENV, GROUP, SYSTEM)||
|role||true|enum (READER, PINGER, PUBLISHER, OPERATOR, ADMIN)||


### WebHookBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|method||false|string||
|url||false|string||
|version||false|string||
|headers||false|string||
|body||false|string||



## Enums

### HostState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### AcceptanceStatus

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### TagTargetType

/

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

 

      http://www.apache.org/licenses/LICENSE-2.0

 

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### DeployState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### AgentStatus

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### HotfixState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### Resource

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### EnvState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### TagValue

/

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

 

      http://www.apache.org/licenses/LICENSE-2.0

 

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### PromoteType

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### AgentState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### EnvironState

/

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

 

       http://www.apache.org/licenses/LICENSE-2.0

 

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### TagSyncState

  INIT:

       the initial state, environment is ready for tag sync workers

  PROCESSING:

       host_tags table is not in-sync with host ec2 tags, and tag sync workers are currently working on it

  ERROR:

       failed to sync host_tags

  FINISHED:

       currently host_tags table is in-sync with host ec2 tags in this environment.

### PromoteDisablePolicy

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### DeployStage

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### ScheduleState

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### OpCode

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### PromoteFailPolicy

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### OverridePolicy

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### AcceptanceType

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### DeployType

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### Role

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.

### DeployPriority

  Copyright 2016 Pinterest, Inc.

 

  Licensed under the Apache License, Version 2.0 (the "License");

  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at

   

      http://www.apache.org/licenses/LICENSE-2.0

     

  Unless required by applicable law or agreed to in writing, software

  distributed under the License is distributed on an "AS IS" BASIS,

  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and

  limitations under the License.
