
<a name="definitions"></a>
## Definitions

<a name="agentbean"></a>
### AgentBean

|Name|Schema|
|---|---|
|**containerHealthStatus**  <br>*optional*|string|
|**deployId**  <br>*optional*|string|
|**deployStage**  <br>*optional*|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)|
|**envId**  <br>*optional*|string|
|**failCount**  <br>*optional*|integer (int32)|
|**firstDeploy**  <br>*optional*|boolean|
|**firstDeployDate**  <br>*optional*|integer (int64)|
|**hostId**  <br>*optional*|string|
|**hostName**  <br>*optional*|string|
|**lastErrno**  <br>*optional*|integer (int32)|
|**lastOperator**  <br>*optional*|string|
|**lastUpdateDate**  <br>*optional*|integer (int64)|
|**stageStartDate**  <br>*optional*|integer (int64)|
|**startDate**  <br>*optional*|integer (int64)|
|**state**  <br>*optional*|enum (NORMAL, PAUSED_BY_SYSTEM, PAUSED_BY_USER, RESET, RESET_BY_SYSTEM, DELETE, UNREACHABLE, STOP)|
|**status**  <br>*optional*|enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH)|


<a name="agenterrorbean"></a>
### AgentErrorBean

|Name|Schema|
|---|---|
|**envId**  <br>*optional*|string|
|**errorMessage**  <br>*optional*|string|
|**hostId**  <br>*optional*|string|
|**hostName**  <br>*optional*|string|


<a name="alarmbean"></a>
### AlarmBean

|Name|Schema|
|---|---|
|**alarmUrl**  <br>*optional*|string|
|**metricsUrl**  <br>*optional*|string|
|**name**  <br>*optional*|string|


<a name="buildbean"></a>
### BuildBean

|Name|Schema|
|---|---|
|**artifactUrl**  <br>*optional*|string|
|**branch**  <br>*optional*|string|
|**commit**  <br>*optional*|string|
|**commitDate**  <br>*optional*|integer (int64)|
|**commitInfo**  <br>*optional*|string|
|**commitShort**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**name**  <br>*optional*|string|
|**publishDate**  <br>*optional*|integer (int64)|
|**publishInfo**  <br>*optional*|string|
|**publisher**  <br>*optional*|string|
|**repo**  <br>*optional*|string|
|**type**  <br>*optional*|string|


<a name="buildtagbean"></a>
### BuildTagBean

|Name|Schema|
|---|---|
|**build**  <br>*optional*|[BuildBean](#buildbean)|
|**tag**  <br>*optional*|[TagBean](#tagbean)|


<a name="chatmessagebean"></a>
### ChatMessageBean

|Name|Schema|
|---|---|
|**from**  <br>*optional*|string|
|**message**  <br>*optional*|string|
|**to**  <br>*optional*|string|


<a name="commitbean"></a>
### CommitBean

|Name|Schema|
|---|---|
|**author**  <br>*optional*|string|
|**date**  <br>*optional*|integer (int64)|
|**info**  <br>*optional*|string|
|**message**  <br>*optional*|string|
|**sha**  <br>*optional*|string|
|**title**  <br>*optional*|string|


<a name="confighistorybean"></a>
### ConfigHistoryBean

|Name|Schema|
|---|---|
|**changeId**  <br>*optional*|string|
|**configChange**  <br>*optional*|string|
|**createTime**  <br>*optional*|integer (int64)|
|**id**  <br>*optional*|string|
|**operator**  <br>*optional*|string|
|**type**  <br>*optional*|string|


<a name="deploybean"></a>
### DeployBean

|Name|Schema|
|---|---|
|**acceptanceStatus**  <br>*optional*|enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED)|
|**alias**  <br>*optional*|string|
|**buildId**  <br>*optional*|string|
|**description**  <br>*optional*|string|
|**envId**  <br>*optional*|string|
|**failTotal**  <br>*optional*|integer (int32)|
|**fromDeployId**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**lastUpdateDate**  <br>*optional*|integer (int64)|
|**operator**  <br>*optional*|string|
|**startDate**  <br>*optional*|integer (int64)|
|**state**  <br>*optional*|enum (RUNNING, FAILING, SUCCEEDING, SUCCEEDED, ABORTED)|
|**successDate**  <br>*optional*|integer (int64)|
|**successTotal**  <br>*optional*|integer (int32)|
|**total**  <br>*optional*|integer (int32)|
|**type**  <br>*optional*|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)|


<a name="deploycandidatesresponse"></a>
### DeployCandidatesResponse

|Name|Schema|
|---|---|
|**candidates**  <br>*optional*|< [PingResponseBean](#pingresponsebean) > array|


<a name="deployconstraintbean"></a>
### DeployConstraintBean

|Name|Schema|
|---|---|
|**constraintKey**  <br>*optional*|string|
|**constraintType**  <br>*optional*|enum (GROUP_BY_GROUP, ALL_GROUPS_IN_PARALLEL)|
|**id**  <br>*optional*|string|
|**lastUpdate**  <br>*optional*|integer (int64)|
|**maxParallel**  <br>*optional*|integer (int64)|
|**startDate**  <br>*optional*|integer (int64)|
|**state**  <br>*optional*|enum (INIT, PROCESSING, ERROR, FINISHED)|


<a name="deploygoalbean"></a>
### DeployGoalBean

|Name|Schema|
|---|---|
|**agentConfigs**  <br>*optional*|< string, string > map|
|**build**  <br>*optional*|[BuildBean](#buildbean)|
|**deployAlias**  <br>*optional*|string|
|**deployId**  <br>*optional*|string|
|**deployStage**  <br>*optional*|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)|
|**deployType**  <br>*optional*|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)|
|**envId**  <br>*optional*|string|
|**envName**  <br>*optional*|string|
|**firstDeploy**  <br>*optional*|boolean|
|**isDocker**  <br>*optional*|boolean|
|**scriptVariables**  <br>*optional*|< string, string > map|
|**stageName**  <br>*optional*|string|
|**stageType**  <br>*optional*|enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING)|


<a name="deployprogressbean"></a>
### DeployProgressBean

|Name|Schema|
|---|---|
|**agents**  <br>*optional*|< [AgentBean](#agentbean) > array|
|**missingHosts**  <br>*optional*|< string > array|
|**provisioningHosts**  <br>*optional*|< [HostBean](#hostbean) > array|


<a name="deployqueryresultbean"></a>
### DeployQueryResultBean

|Name|Schema|
|---|---|
|**deployTags**  <br>*optional*|< string, [TagBean](#tagbean) > map|
|**deploys**  <br>*optional*|< [DeployBean](#deploybean) > array|
|**total**  <br>*optional*|integer (int64)|
|**truncated**  <br>*optional*|boolean|


<a name="envwebhookbean"></a>
### EnvWebHookBean

|Name|Schema|
|---|---|
|**postDeployHooks**  <br>*optional*|< [WebHookBean](#webhookbean) > array|
|**preDeployHooks**  <br>*optional*|< [WebHookBean](#webhookbean) > array|


<a name="environbean"></a>
### EnvironBean

|Name|Schema|
|---|---|
|**acceptanceType**  <br>*optional*|enum (AUTO, MANUAL)|
|**advancedConfigId**  <br>*optional*|string|
|**alarmConfigId**  <br>*optional*|string|
|**allowPrivateBuild**  <br>*optional*|boolean|
|**branch**  <br>*optional*|string|
|**buildName**  <br>*optional*|string|
|**chatroom**  <br>*optional*|string|
|**clusterName**  <br>*optional*|string|
|**deployConstraintId**  <br>*optional*|string|
|**deployId**  <br>*optional*|string|
|**deployType**  <br>*optional*|enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)|
|**description**  <br>*optional*|string|
|**emailRecipients**  <br>*optional*|string|
|**ensureTrustedBuild**  <br>*optional*|boolean|
|**envName**  <br>*optional*|string|
|**envState**  <br>*optional*|enum (NORMAL, PAUSED, DISABLED)|
|**externalId**  <br>*optional*|string|
|**groupMentionRecipients**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**isDocker**  <br>*optional*|boolean|
|**isSOX**  <br>*optional*|boolean|
|**lastOperator**  <br>*optional*|string|
|**lastUpdate**  <br>*optional*|integer (int64)|
|**maxDeployDay**  <br>*optional*|integer (int32)|
|**maxDeployNum**  <br>*optional*|integer (int32)|
|**maxParallel**  <br>*optional*|integer (int32)|
|**maxParallelPct**  <br>*optional*|integer (int32)|
|**maxParallelRp**  <br>*optional*|integer (int32)|
|**metricsConfigId**  <br>*optional*|string|
|**notifyAuthors**  <br>*optional*|boolean|
|**overridePolicy**  <br>*optional*|enum (OVERRIDE, WARN)|
|**priority**  <br>*optional*|enum (NORMAL, HIGH, LOW, HIGHER, LOWER)|
|**scheduleId**  <br>*optional*|string|
|**scriptConfigId**  <br>*optional*|string|
|**stageName**  <br>*optional*|string|
|**stageType**  <br>*optional*|enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING)|
|**state**  <br>*optional*|enum (NORMAL, DISABLED)|
|**stuckThreshold**  <br>*optional*|integer (int32)|
|**successThreshold**  <br>*optional*|integer (int32)|
|**systemPriority**  <br>*optional*|integer (int32)|
|**terminationLimit**  <br>*optional*|integer (int32)|
|**watchRecipients**  <br>*optional*|string|
|**webhooksConfigId**  <br>*optional*|string|


<a name="grouprolesbean"></a>
### GroupRolesBean

|Name|Schema|
|---|---|
|**name**  <br>*optional*|string|
|**resource**  <br>*optional*|string|
|**role**  <br>*required*|enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)|
|**type**  <br>*optional*|enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD)|


<a name="hostbean"></a>
### HostBean

|Name|Schema|
|---|---|
|**accountId**  <br>*optional*|string|
|**canRetire**  <br>*optional*|integer (int32)|
|**createDate**  <br>*optional*|integer (int64)|
|**groupName**  <br>*optional*|string|
|**hostId**  <br>*optional*|string|
|**hostName**  <br>*optional*|string|
|**ip**  <br>*optional*|string|
|**lastUpdateDate**  <br>*optional*|integer (int64)|
|**pendingTerminate**  <br>*optional*|boolean|
|**state**  <br>*optional*|enum (PROVISIONED, ACTIVE, PENDING_TERMINATE, TERMINATING, TERMINATED, PENDING_TERMINATE_NO_REPLACE)|


<a name="hosttaginfo"></a>
### HostTagInfo

|Name|Schema|
|---|---|
|**hostId**  <br>*optional*|string|
|**hostName**  <br>*optional*|string|
|**tagName**  <br>*optional*|string|
|**tagValue**  <br>*optional*|string|


<a name="hotfixbean"></a>
### HotfixBean

|Name|Schema|
|---|---|
|**baseCommit**  <br>*optional*|string|
|**baseDeployId**  <br>*optional*|string|
|**commits**  <br>*optional*|string|
|**envName**  <br>*optional*|string|
|**errorMessage**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**jobName**  <br>*optional*|string|
|**jobNum**  <br>*optional*|string|
|**lastWorkedOn**  <br>*optional*|integer (int64)|
|**operator**  <br>*optional*|string|
|**progress**  <br>*optional*|integer (int32)|
|**repo**  <br>*optional*|string|
|**startDate**  <br>*optional*|integer (int64)|
|**state**  <br>*optional*|enum (INITIAL, PUSHING, BUILDING, SUCCEEDED, FAILED, ABORTED)|
|**timeout**  <br>*optional*|integer (int32)|


<a name="metricsconfigbean"></a>
### MetricsConfigBean

|Name|Schema|
|---|---|
|**specs**  <br>*optional*|< [MetricsSpecBean](#metricsspecbean) > array|
|**title**  <br>*optional*|string|
|**url**  <br>*optional*|string|


<a name="metricsspecbean"></a>
### MetricsSpecBean

|Name|Schema|
|---|---|
|**color**  <br>*optional*|string|
|**max**  <br>*optional*|number (double)|
|**min**  <br>*optional*|number (double)|


<a name="pingreportbean"></a>
### PingReportBean

|Name|Schema|
|---|---|
|**agentState**  <br>*optional*|string|
|**agentStatus**  <br>*optional*|enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH)|
|**containerHealthStatus**  <br>*optional*|string|
|**deployAlias**  <br>*optional*|string|
|**deployId**  <br>*optional*|string|
|**deployStage**  <br>*optional*|enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)|
|**envId**  <br>*optional*|string|
|**errorCode**  <br>*optional*|integer (int32)|
|**errorMessage**  <br>*optional*|string|
|**extraInfo**  <br>*optional*|< string, string > map|
|**failCount**  <br>*optional*|integer (int32)|


<a name="pingrequestbean"></a>
### PingRequestBean

|Name|Schema|
|---|---|
|**accountId**  <br>*optional*|string|
|**agentVersion**  <br>*optional*|string|
|**autoscalingGroup**  <br>*optional*|string|
|**availabilityZone**  <br>*optional*|string|
|**ec2Tags**  <br>*optional*|string|
|**groups**  <br>*optional*|< string > array|
|**hostId**  <br>*optional*|string|
|**hostIp**  <br>*optional*|string|
|**hostName**  <br>*optional*|string|
|**reports**  <br>*optional*|< [PingReportBean](#pingreportbean) > array|
|**stageType**  <br>*optional*|enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING)|


<a name="pingresponsebean"></a>
### PingResponseBean

|Name|Schema|
|---|---|
|**deployGoal**  <br>*optional*|[DeployGoalBean](#deploygoalbean)|
|**opCode**  <br>*optional*|enum (NOOP, DEPLOY, RESTART, DELETE, WAIT, ROLLBACK, STOP)|


<a name="promotebean"></a>
### PromoteBean

|Name|Description|Schema|
|---|---|---|
|**delay**  <br>*optional*|**Minimum value** : `0`|integer (int32)|
|**disablePolicy**  <br>*optional*||enum (MANUAL, AUTO)|
|**envId**  <br>*optional*||string|
|**failPolicy**  <br>*optional*||enum (CONTINUE, DISABLE, ROLLBACK)|
|**lastOperator**  <br>*optional*||string|
|**lastUpdate**  <br>*optional*||integer (int64)|
|**predStage**  <br>*optional*||string|
|**queueSize**  <br>*optional*|**Minimum value** : `1`  <br>**Maximum value** : `10`|integer (int32)|
|**schedule**  <br>*optional*||string|
|**type**  <br>*required*||enum (MANUAL, AUTO)|


<a name="ratingbean"></a>
### RatingBean

|Name|Schema|
|---|---|
|**author**  <br>*optional*|string|
|**createDate**  <br>*optional*|integer (int64)|
|**feedback**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**rating**  <br>*optional*|string|


<a name="schedulebean"></a>
### ScheduleBean

|Name|Schema|
|---|---|
|**cooldownTimes**  <br>*optional*|string|
|**currentSession**  <br>*optional*|integer (int32)|
|**hostNumbers**  <br>*optional*|string|
|**id**  <br>*optional*|string|
|**state**  <br>*optional*|enum (NOT_STARTED, RUNNING, COOLING_DOWN, FINAL)|
|**stateStartTime**  <br>*optional*|integer (int64)|
|**totalSessions**  <br>*optional*|integer (int32)|


<a name="tagbean"></a>
### TagBean

|Name|Schema|
|---|---|
|**comments**  <br>*optional*|string|
|**createdDate**  <br>*optional*|integer (int64)|
|**id**  <br>*optional*|string|
|**metaInfo**  <br>*optional*|string|
|**operator**  <br>*optional*|string|
|**targetId**  <br>*optional*|string|
|**targetType**  <br>*optional*|enum (BUILD, ENVIRONMENT, TELETRAAN)|
|**value**  <br>*optional*|enum (BAD_BUILD, GOOD_BUILD, ENABLE_ENV, DISABLE_ENV)|


<a name="tokenrolesbean"></a>
### TokenRolesBean

|Name|Schema|
|---|---|
|**expireDate**  <br>*optional*|integer (int64)|
|**name**  <br>*optional*|string|
|**resource**  <br>*optional*|string|
|**role**  <br>*required*|enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)|
|**token**  <br>*optional*|string|
|**type**  <br>*optional*|enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD)|


<a name="userrolesbean"></a>
### UserRolesBean

|Name|Schema|
|---|---|
|**name**  <br>*optional*|string|
|**resource**  <br>*optional*|string|
|**role**  <br>*required*|enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)|
|**type**  <br>*optional*|enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD)|


<a name="webhookbean"></a>
### WebHookBean

|Name|Schema|
|---|---|
|**body**  <br>*optional*|string|
|**headers**  <br>*optional*|string|
|**method**  <br>*optional*|string|
|**url**  <br>*optional*|string|
|**version**  <br>*optional*|string|




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

### EnvType

  Copyright 2020 Pinterest, Inc.

 

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

### DeployConstraintType

  GROUP_BY_GROUP:

       randomly choose X num of hosts from one group, and ONLY deploy this group,

       when all the hosts in this group finish, proceed to the next group

  ALL_GROUPS_IN_PARALLEL:

       randomly choose X num of hosts from EACH group, and deploy at the same time

### TeletraanPrincipalRole

  Copyright (c) 2024, Pinterest Inc. All rights reserved.

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
