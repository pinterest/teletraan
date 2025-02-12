<a name="definitions"></a>

## Definitions

<a name="agentbean"></a>

### AgentBean

| Name                                     | Schema                                                                                                                                               |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **containerHealthStatus** <br>_optional_ | string                                                                                                                                               |
| **deployId** <br>_optional_              | string                                                                                                                                               |
| **deployStage** <br>_optional_           | enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)           |
| **envId** <br>_optional_                 | string                                                                                                                                               |
| **failCount** <br>_optional_             | integer (int32)                                                                                                                                      |
| **firstDeploy** <br>_optional_           | boolean                                                                                                                                              |
| **firstDeployDate** <br>_optional_       | integer (int64)                                                                                                                                      |
| **hostId** <br>_optional_                | string                                                                                                                                               |
| **hostName** <br>_optional_              | string                                                                                                                                               |
| **lastErrno** <br>_optional_             | integer (int32)                                                                                                                                      |
| **lastOperator** <br>_optional_          | string                                                                                                                                               |
| **lastUpdateDate** <br>_optional_        | integer (int64)                                                                                                                                      |
| **stageStartDate** <br>_optional_        | integer (int64)                                                                                                                                      |
| **startDate** <br>_optional_             | integer (int64)                                                                                                                                      |
| **state** <br>_optional_                 | enum (NORMAL, PAUSED_BY_SYSTEM, PAUSED_BY_USER, RESET, RESET_BY_SYSTEM, DELETE, UNREACHABLE, STOP)                                                   |
| **status** <br>_optional_                | enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH) |

<a name="agenterrorbean"></a>

### AgentErrorBean

| Name                            | Schema |
| ------------------------------- | ------ |
| **envId** <br>_optional_        | string |
| **errorMessage** <br>_optional_ | string |
| **hostId** <br>_optional_       | string |
| **hostName** <br>_optional_     | string |

<a name="alarmbean"></a>

### AlarmBean

| Name                          | Schema |
| ----------------------------- | ------ |
| **alarmUrl** <br>_optional_   | string |
| **metricsUrl** <br>_optional_ | string |
| **name** <br>_optional_       | string |

<a name="buildbean"></a>

### BuildBean

| Name                           | Schema          |
| ------------------------------ | --------------- |
| **artifactUrl** <br>_optional_ | string          |
| **branch** <br>_optional_      | string          |
| **commit** <br>_optional_      | string          |
| **commitDate** <br>_optional_  | integer (int64) |
| **commitInfo** <br>_optional_  | string          |
| **commitShort** <br>_optional_ | string          |
| **id** <br>_optional_          | string          |
| **name** <br>_optional_        | string          |
| **publishDate** <br>_optional_ | integer (int64) |
| **publishInfo** <br>_optional_ | string          |
| **publisher** <br>_optional_   | string          |
| **repo** <br>_optional_        | string          |
| **type** <br>_optional_        | string          |

<a name="buildtagbean"></a>

### BuildTagBean

| Name                     | Schema                  |
| ------------------------ | ----------------------- |
| **build** <br>_optional_ | [BuildBean](#buildbean) |
| **tag** <br>_optional_   | [TagBean](#tagbean)     |

<a name="chatmessagebean"></a>

### ChatMessageBean

| Name                       | Schema |
| -------------------------- | ------ |
| **from** <br>_optional_    | string |
| **message** <br>_optional_ | string |
| **to** <br>_optional_      | string |

<a name="commitbean"></a>

### CommitBean

| Name                       | Schema          |
| -------------------------- | --------------- |
| **author** <br>_optional_  | string          |
| **date** <br>_optional_    | integer (int64) |
| **info** <br>_optional_    | string          |
| **message** <br>_optional_ | string          |
| **sha** <br>_optional_     | string          |
| **title** <br>_optional_   | string          |

<a name="confighistorybean"></a>

### ConfigHistoryBean

| Name                            | Schema          |
| ------------------------------- | --------------- |
| **changeId** <br>_optional_     | string          |
| **configChange** <br>_optional_ | string          |
| **createTime** <br>_optional_   | integer (int64) |
| **id** <br>_optional_           | string          |
| **operator** <br>_optional_     | string          |
| **type** <br>_optional_         | string          |

<a name="deploybean"></a>

### DeployBean

| Name                                | Schema                                                                             |
| ----------------------------------- | ---------------------------------------------------------------------------------- |
| **acceptanceStatus** <br>_optional_ | enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED) |
| **alias** <br>_optional_            | string                                                                             |
| **buildId** <br>_optional_          | string                                                                             |
| **description** <br>_optional_      | string                                                                             |
| **envId** <br>_optional_            | string                                                                             |
| **failTotal** <br>_optional_        | integer (int32)                                                                    |
| **fromDeployId** <br>_optional_     | string                                                                             |
| **id** <br>_optional_               | string                                                                             |
| **lastUpdateDate** <br>_optional_   | integer (int64)                                                                    |
| **operator** <br>_optional_         | string                                                                             |
| **startDate** <br>_optional_        | integer (int64)                                                                    |
| **state** <br>_optional_            | enum (RUNNING, FAILING, SUCCEEDING, SUCCEEDED, ABORTED)                            |
| **successDate** <br>_optional_      | integer (int64)                                                                    |
| **successTotal** <br>_optional_     | integer (int32)                                                                    |
| **total** <br>_optional_            | integer (int32)                                                                    |
| **type** <br>_optional_             | enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)                                    |

<a name="deploycandidatesresponse"></a>

### DeployCandidatesResponse

| Name                          | Schema                                          |
| ----------------------------- | ----------------------------------------------- |
| **candidates** <br>_optional_ | < [PingResponseBean](#pingresponsebean) > array |

<a name="deployconstraintbean"></a>

### DeployConstraintBean

| Name                              | Schema                                        |
| --------------------------------- | --------------------------------------------- |
| **constraintKey** <br>_optional_  | string                                        |
| **constraintType** <br>_optional_ | enum (GROUP_BY_GROUP, ALL_GROUPS_IN_PARALLEL) |
| **id** <br>_optional_             | string                                        |
| **lastUpdate** <br>_optional_     | integer (int64)                               |
| **maxParallel** <br>_optional_    | integer (int64)                               |
| **startDate** <br>_optional_      | integer (int64)                               |
| **state** <br>_optional_          | enum (INIT, PROCESSING, ERROR, FINISHED)      |

<a name="deploygoalbean"></a>

### DeployGoalBean

| Name                               | Schema                                                                                                                                     |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| **agentConfigs** <br>_optional_    | < string, string > map                                                                                                                     |
| **build** <br>_optional_           | [BuildBean](#buildbean)                                                                                                                    |
| **deployAlias** <br>_optional_     | string                                                                                                                                     |
| **deployId** <br>_optional_        | string                                                                                                                                     |
| **deployStage** <br>_optional_     | enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED) |
| **deployType** <br>_optional_      | enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)                                                                                            |
| **envId** <br>_optional_           | string                                                                                                                                     |
| **envName** <br>_optional_         | string                                                                                                                                     |
| **firstDeploy** <br>_optional_     | boolean                                                                                                                                    |
| **isDocker** <br>_optional_        | boolean                                                                                                                                    |
| **scriptVariables** <br>_optional_ | < string, string > map                                                                                                                     |
| **stageName** <br>_optional_       | string                                                                                                                                     |
| **stageType** <br>_optional_       | enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING)                                                                          |

<a name="deployprogressbean"></a>

### DeployProgressBean

| Name                                 | Schema                            |
| ------------------------------------ | --------------------------------- |
| **agents** <br>_optional_            | < [AgentBean](#agentbean) > array |
| **missingHosts** <br>_optional_      | < string > array                  |
| **provisioningHosts** <br>_optional_ | < [HostBean](#hostbean) > array   |

<a name="deployqueryresultbean"></a>

### DeployQueryResultBean

| Name                          | Schema                              |
| ----------------------------- | ----------------------------------- |
| **deployTags** <br>_optional_ | < string, [TagBean](#tagbean) > map |
| **deploys** <br>_optional_    | < [DeployBean](#deploybean) > array |
| **total** <br>_optional_      | integer (int64)                     |
| **truncated** <br>_optional_  | boolean                             |

<a name="envwebhookbean"></a>

### EnvWebHookBean

| Name                               | Schema                                |
| ---------------------------------- | ------------------------------------- |
| **postDeployHooks** <br>_optional_ | < [WebHookBean](#webhookbean) > array |
| **preDeployHooks** <br>_optional_  | < [WebHookBean](#webhookbean) > array |

<a name="environbean"></a>

### EnvironBean

| Name                                      | Schema                                                            |
| ----------------------------------------- | ----------------------------------------------------------------- |
| **acceptanceType** <br>_optional_         | enum (AUTO, MANUAL)                                               |
| **advancedConfigId** <br>_optional_       | string                                                            |
| **alarmConfigId** <br>_optional_          | string                                                            |
| **allowPrivateBuild** <br>_optional_      | boolean                                                           |
| **branch** <br>_optional_                 | string                                                            |
| **buildName** <br>_optional_              | string                                                            |
| **chatroom** <br>_optional_               | string                                                            |
| **clusterName** <br>_optional_            | string                                                            |
| **deployConstraintId** <br>_optional_     | string                                                            |
| **deployId** <br>_optional_               | string                                                            |
| **deployType** <br>_optional_             | enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP)                   |
| **description** <br>_optional_            | string                                                            |
| **emailRecipients** <br>_optional_        | string                                                            |
| **ensureTrustedBuild** <br>_optional_     | boolean                                                           |
| **envName** <br>_optional_                | string                                                            |
| **envState** <br>_optional_               | enum (NORMAL, PAUSED, DISABLED)                                   |
| **externalId** <br>_optional_             | string                                                            |
| **groupMentionRecipients** <br>_optional_ | string                                                            |
| **id** <br>_optional_                     | string                                                            |
| **isDocker** <br>_optional_               | boolean                                                           |
| **isSOX** <br>_optional_                  | boolean                                                           |
| **lastOperator** <br>_optional_           | string                                                            |
| **lastUpdate** <br>_optional_             | integer (int64)                                                   |
| **maxDeployDay** <br>_optional_           | integer (int32)                                                   |
| **maxDeployNum** <br>_optional_           | integer (int32)                                                   |
| **maxParallel** <br>_optional_            | integer (int32)                                                   |
| **maxParallelPct** <br>_optional_         | integer (int32)                                                   |
| **maxParallelRp** <br>_optional_          | integer (int32)                                                   |
| **metricsConfigId** <br>_optional_        | string                                                            |
| **notifyAuthors** <br>_optional_          | boolean                                                           |
| **overridePolicy** <br>_optional_         | enum (OVERRIDE, WARN)                                             |
| **priority** <br>_optional_               | enum (NORMAL, HIGH, LOW, HIGHER, LOWER)                           |
| **scheduleId** <br>_optional_             | string                                                            |
| **scriptConfigId** <br>_optional_         | string                                                            |
| **stageName** <br>_optional_              | string                                                            |
| **stageType** <br>_optional_              | enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING) |
| **state** <br>_optional_                  | enum (NORMAL, DISABLED)                                           |
| **stuckThreshold** <br>_optional_         | integer (int32)                                                   |
| **successThreshold** <br>_optional_       | integer (int32)                                                   |
| **systemPriority** <br>_optional_         | integer (int32)                                                   |
| **terminationLimit** <br>_optional_       | integer (int32)                                                   |
| **watchRecipients** <br>_optional_        | string                                                            |
| **webhooksConfigId** <br>_optional_       | string                                                            |

<a name="grouprolesbean"></a>

### GroupRolesBean

| Name                        | Schema                                                                                      |
| --------------------------- | ------------------------------------------------------------------------------------------- |
| **name** <br>_optional_     | string                                                                                      |
| **resource** <br>_optional_ | string                                                                                      |
| **role** <br>_required_     | enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)             |
| **type** <br>_optional_     | enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD) |

<a name="hostbean"></a>

### HostBean

| Name                                | Schema                                                                                               |
| ----------------------------------- | ---------------------------------------------------------------------------------------------------- |
| **accountId** <br>_optional_        | string                                                                                               |
| **canRetire** <br>_optional_        | integer (int32)                                                                                      |
| **createDate** <br>_optional_       | integer (int64)                                                                                      |
| **groupName** <br>_optional_        | string                                                                                               |
| **hostId** <br>_optional_           | string                                                                                               |
| **hostName** <br>_optional_         | string                                                                                               |
| **ip** <br>_optional_               | string                                                                                               |
| **lastUpdateDate** <br>_optional_   | integer (int64)                                                                                      |
| **pendingTerminate** <br>_optional_ | boolean                                                                                              |
| **state** <br>_optional_            | enum (PROVISIONED, ACTIVE, PENDING_TERMINATE, TERMINATING, TERMINATED, PENDING_TERMINATE_NO_REPLACE) |

<a name="hosttaginfo"></a>

### HostTagInfo

| Name                        | Schema |
| --------------------------- | ------ |
| **hostId** <br>_optional_   | string |
| **hostName** <br>_optional_ | string |
| **tagName** <br>_optional_  | string |
| **tagValue** <br>_optional_ | string |

<a name="hotfixbean"></a>

### HotfixBean

| Name                            | Schema                                                        |
| ------------------------------- | ------------------------------------------------------------- |
| **baseCommit** <br>_optional_   | string                                                        |
| **baseDeployId** <br>_optional_ | string                                                        |
| **commits** <br>_optional_      | string                                                        |
| **envName** <br>_optional_      | string                                                        |
| **errorMessage** <br>_optional_ | string                                                        |
| **id** <br>_optional_           | string                                                        |
| **jobName** <br>_optional_      | string                                                        |
| **jobNum** <br>_optional_       | string                                                        |
| **lastWorkedOn** <br>_optional_ | integer (int64)                                               |
| **operator** <br>_optional_     | string                                                        |
| **progress** <br>_optional_     | integer (int32)                                               |
| **repo** <br>_optional_         | string                                                        |
| **startDate** <br>_optional_    | integer (int64)                                               |
| **state** <br>_optional_        | enum (INITIAL, PUSHING, BUILDING, SUCCEEDED, FAILED, ABORTED) |
| **timeout** <br>_optional_      | integer (int32)                                               |

<a name="metricsconfigbean"></a>

### MetricsConfigBean

| Name                     | Schema                                        |
| ------------------------ | --------------------------------------------- |
| **specs** <br>_optional_ | < [MetricsSpecBean](#metricsspecbean) > array |
| **title** <br>_optional_ | string                                        |
| **url** <br>_optional_   | string                                        |

<a name="metricsspecbean"></a>

### MetricsSpecBean

| Name                     | Schema          |
| ------------------------ | --------------- |
| **color** <br>_optional_ | string          |
| **max** <br>_optional_   | number (double) |
| **min** <br>_optional_   | number (double) |

<a name="pingreportbean"></a>

### PingReportBean

| Name                                     | Schema                                                                                                                                               |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **agentState** <br>_optional_            | string                                                                                                                                               |
| **agentStatus** <br>_optional_           | enum (SUCCEEDED, UNKNOWN, AGENT_FAILED, RETRYABLE_AGENT_FAILED, SCRIPT_FAILED, ABORTED_BY_SERVICE, SCRIPT_TIMEOUT, TOO_MANY_RETRY, RUNTIME_MISMATCH) |
| **containerHealthStatus** <br>_optional_ | string                                                                                                                                               |
| **deployAlias** <br>_optional_           | string                                                                                                                                               |
| **deployId** <br>_optional_              | string                                                                                                                                               |
| **deployStage** <br>_optional_           | enum (UNKNOWN, PRE_DOWNLOAD, DOWNLOADING, POST_DOWNLOAD, STAGING, PRE_RESTART, RESTARTING, POST_RESTART, SERVING_BUILD, STOPPING, STOPPED)           |
| **envId** <br>_optional_                 | string                                                                                                                                               |
| **errorCode** <br>_optional_             | integer (int32)                                                                                                                                      |
| **errorMessage** <br>_optional_          | string                                                                                                                                               |
| **extraInfo** <br>_optional_             | < string, string > map                                                                                                                               |
| **failCount** <br>_optional_             | integer (int32)                                                                                                                                      |

<a name="pingrequestbean"></a>

### PingRequestBean

| Name                                | Schema                                                            |
| ----------------------------------- | ----------------------------------------------------------------- |
| **accountId** <br>_optional_        | string                                                            |
| **agentVersion** <br>_optional_     | string                                                            |
| **autoscalingGroup** <br>_optional_ | string                                                            |
| **availabilityZone** <br>_optional_ | string                                                            |
| **ec2Tags** <br>_optional_          | string                                                            |
| **groups** <br>_optional_           | < string > array                                                  |
| **hostId** <br>_optional_           | string                                                            |
| **hostIp** <br>_optional_           | string                                                            |
| **hostName** <br>_optional_         | string                                                            |
| **reports** <br>_optional_          | < [PingReportBean](#pingreportbean) > array                       |
| **stageType** <br>_optional_        | enum (DEFAULT, PRODUCTION, CONTROL, CANARY, LATEST, DEV, STAGING) |

<a name="pingresponsebean"></a>

### PingResponseBean

| Name                          | Schema                                                     |
| ----------------------------- | ---------------------------------------------------------- |
| **deployGoal** <br>_optional_ | [DeployGoalBean](#deploygoalbean)                          |
| **opCode** <br>_optional_     | enum (NOOP, DEPLOY, RESTART, DELETE, WAIT, ROLLBACK, STOP) |

<a name="promotebean"></a>

### PromoteBean

| Name                             | Description                                          | Schema                             |
| -------------------------------- | ---------------------------------------------------- | ---------------------------------- |
| **delay** <br>_optional_         | **Minimum value** : `0`                              | integer (int32)                    |
| **disablePolicy** <br>_optional_ |                                                      | enum (MANUAL, AUTO)                |
| **envId** <br>_optional_         |                                                      | string                             |
| **failPolicy** <br>_optional_    |                                                      | enum (CONTINUE, DISABLE, ROLLBACK) |
| **lastOperator** <br>_optional_  |                                                      | string                             |
| **lastUpdate** <br>_optional_    |                                                      | integer (int64)                    |
| **predStage** <br>_optional_     |                                                      | string                             |
| **queueSize** <br>_optional_     | **Minimum value** : `1` <br>**Maximum value** : `10` | integer (int32)                    |
| **schedule** <br>_optional_      |                                                      | string                             |
| **type** <br>_required_          |                                                      | enum (MANUAL, AUTO)                |

<a name="ratingbean"></a>

### RatingBean

| Name                          | Schema          |
| ----------------------------- | --------------- |
| **author** <br>_optional_     | string          |
| **createDate** <br>_optional_ | integer (int64) |
| **feedback** <br>_optional_   | string          |
| **id** <br>_optional_         | string          |
| **rating** <br>_optional_     | string          |

<a name="schedulebean"></a>

### ScheduleBean

| Name                              | Schema                                           |
| --------------------------------- | ------------------------------------------------ |
| **cooldownTimes** <br>_optional_  | string                                           |
| **currentSession** <br>_optional_ | integer (int32)                                  |
| **hostNumbers** <br>_optional_    | string                                           |
| **id** <br>_optional_             | string                                           |
| **state** <br>_optional_          | enum (NOT_STARTED, RUNNING, COOLING_DOWN, FINAL) |
| **stateStartTime** <br>_optional_ | integer (int64)                                  |
| **totalSessions** <br>_optional_  | integer (int32)                                  |

<a name="tagbean"></a>

### TagBean

| Name                           | Schema                                                                 |
| ------------------------------ | ---------------------------------------------------------------------- |
| **comments** <br>_optional_    | string                                                                 |
| **createdDate** <br>_optional_ | integer (int64)                                                        |
| **id** <br>_optional_          | string                                                                 |
| **metaInfo** <br>_optional_    | string                                                                 |
| **operator** <br>_optional_    | string                                                                 |
| **targetId** <br>_optional_    | string                                                                 |
| **targetType** <br>_optional_  | enum (BUILD, ENVIRONMENT, TELETRAAN)                                   |
| **value** <br>_optional_       | enum (BAD_BUILD, GOOD_BUILD, CERTIFIED_BUILD, ENABLE_ENV, DISABLE_ENV) |

<a name="tokenrolesbean"></a>

### TokenRolesBean

| Name                          | Schema                                                                                      |
| ----------------------------- | ------------------------------------------------------------------------------------------- |
| **expireDate** <br>_optional_ | integer (int64)                                                                             |
| **name** <br>_optional_       | string                                                                                      |
| **resource** <br>_optional_   | string                                                                                      |
| **role** <br>_required_       | enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)             |
| **token** <br>_optional_      | string                                                                                      |
| **type** <br>_optional_       | enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD) |

<a name="userrolesbean"></a>

### UserRolesBean

| Name                        | Schema                                                                                      |
| --------------------------- | ------------------------------------------------------------------------------------------- |
| **name** <br>_optional_     | string                                                                                      |
| **resource** <br>_optional_ | string                                                                                      |
| **role** <br>_required_     | enum (READ, READER, PINGER, PUBLISHER, EXECUTE, WRITE, DELETE, OPERATOR, ADMIN)             |
| **type** <br>_optional_     | enum (ENV, GROUP, SYSTEM, ENV_STAGE, PLACEMENT, BASE_IMAGE, SECURITY_ZONE, IAM_ROLE, BUILD) |

<a name="webhookbean"></a>

### WebHookBean

| Name                       | Schema |
| -------------------------- | ------ |
| **body** <br>_optional_    | string |
| **headers** <br>_optional_ | string |
| **method** <br>_optional_  | string |
| **url** <br>_optional_     | string |
| **version** <br>_optional_ | string |

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
