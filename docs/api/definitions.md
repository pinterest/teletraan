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


### DeployProgressBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|agents||false|AgentBean array||
|missingHosts||false|string array||


### DeployQueryResultBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|deploys||false|DeployBean array||
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


### SpecBean
|Name|Description|Required|Schema|Default|
|----|----|----|----|----|
|id||false|string||
|info||false|object||


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

### AcceptanceStatus

  - PENDING_DEPLOY

     Deploy has not completed yet

  OUSTANDING:

     Deploy has not completed yet

### AcceptanceType

  AUTO:

       Deploy will be automatically accepted once deploy is SUCCEEDING

 

   MANUAL:

       Deploy will only be accepted by external process explicit action

### AgentState

  NORMAL:

       Normal state

  PAUSED_BY_STSTEM:

       Agent being paused by system automatically

  PAUSED_BY_USER:

       Agent being paused by user manually

  RESET:

       Agent should retry last failure

  DELETE:

       Agent should delete its status file

  UNREACHABLE:

       Agent has not reported for certain time

  STOP:

       Agent is shutting down the service

 

### AgentStatus

### ASGStatus

### DeployPriority

### DeployStage

  UNKNOWN:

       Reserved by system when deploy stage is unknown

  PRE_DOWNLOAD:

       Before deploy agent download the build

  DOWNLOADING:

       Deploy agent is downloading the build

  POST_DOWNLOAD:

       After deploy agent download the build

  STAGING:

       Deploy agent is working on prepare the build for service restart

  PRE_RESTART:

       Before deploy agent restart the service

  RESTARTING:

       Deploy agent is restarting the service

  POST_RESTART:

       After deploy agent restart the service

  SERVING_BUILD:

       Service is serving traffic

  STOPPING:

       Deploy Agent is shutting down the service

  STOPPED:

       Complete shutting down the service

### DeployState

### DeployType

  REGULAR:

       Regular deploy

  HOTFIX:

       Special deploy which should go fast

  ROLLBACK:

       Special deploy to redeploy certain previous build

  RESTART:

       Special deploy to redeploy current build

  STOP:

       Special deploy to stop service

### EnvState

  NORMAL:

       Normal environment state

  PAUSED:

       Environment current deploy is on pause

  DISABLED:

       Environment is disabled, current deploy should be on pause

### HostState

  PROVISIONED:

       Host is being provisioned

  ACTIVE:

       Host is ready to deploy

  PENDING_TERMINATE:

       Host is pending terminate

  TERMINATING:

       Host if being terminated

  TERMINATED:

       Host is terminated

### HotfixState

### OpCode

  NOOP:

       No action needed

  DEPLOY:

       Agent needs to restart service

  RESTART:

       Agent needs to restart service

  DELETE:

       Agent needs to delete its own status file for this env

  WAIT:

       Agent needs to sleep for certain time

  ROLLBACK:

       Agent needs to rollback

  STOP:

       Agent needs to shutdown service

### PromoteDisablePolicy

  MANUAL:

     Auto promote can only be disabled manually

  AUTO:

       Auto promote will be disabled automatically once there is a manual deploy

### PromoteFailPolicy

### PromoteType

  MANUAL:

      Deploy is promoted from pred stage to next stage manually

  AUTO:

      Deploy is automatically promoted from pred stage to next stage

### Resource

### Role

  READER:

       Default role, everyone who is able to use Teletraan has READER access.

  PINGER:

       Role required to ping server.

  PUBLISHER:

       Role required to publish artifacts.

  OPERATOR:

       Role where user can modify a specific environment's config and

       perform deploy related actions.

  ADMIN:

       Role that has the same environment specific privileges as OPERATOR

       plus the ability specify new OPERATORS and ADMINs for said environment.

       When a new environment is created the creating user is the designated the

       first ADMIN.
