### Deploys

Deploy info APIs

#### GET /v1/deploys
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|envId||false|multi string array||
|QueryParameter|operator||false|multi string array||
|QueryParameter|deployType||false|multi enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP) array||
|QueryParameter|deployState||false|multi enum (RUNNING, FAILING, SUCCEEDING, SUCCEEDED, ABORTED) array||
|QueryParameter|acceptanceStatus||false|multi enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED) array||
|QueryParameter|commit||false|string||
|QueryParameter|repo||false|string||
|QueryParameter|branch||false|string||
|QueryParameter|commitDate||false|integer (int64)||
|QueryParameter|before||false|integer (int64)||
|QueryParameter|after||false|integer (int64)||
|QueryParameter|pageIndex||false|integer (int32)||
|QueryParameter|pageSize||false|integer (int32)||
|QueryParameter|oldestFirst||false|boolean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|DeployQueryResultBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Get deploy info by environment
```
GET /v1/envs/{envName}/{stageName}/deploys/current
```

##### Description

Returns a deploy info object given an environment name and stage name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|DeployBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Get deploy info
```
GET /v1/deploys/{id}
```

##### Description

Returns a deploy object given a deploy id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Deploy id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|DeployBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete deploy info
```
DELETE /v1/deploys/{id}
```

##### Description

Delete deploy info given a deploy id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Deploy id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Update deploy info
```
PUT /v1/deploys/{id}
```

##### Description

Update deploy info given a deploy id and AcceptanceStatus

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Deploy id|true|string||
|QueryParameter|acceptanceStatus|AcceptanceStatus enum option|true|enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED)||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Update deploy progress
```
PUT /v1/envs/{envName}/{stageName}/deploys/current/progress
```

##### Description

Updates a deploy's progress given an environment name and stage name and returns a deploy progress object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|DeployProgressBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a deploy
```
POST /v1/envs/{envName}/{stageName}/deploys
```

##### Description

Creates a deploy given an environment name, stage name, build id and description

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|QueryParameter|buildId|Build id|true|string||
|QueryParameter|description|Description|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Take deploy action
```
POST /v1/envs/{envName}/{stageName}/deploys/current/actions
```

##### Description

Take an action on a deploy such as RESTART or PAUSE

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|QueryParameter|actionType|ActionType enum selection|true|enum (PROMOTE, RESTART, ROLLBACK, PAUSE, RESUME)||
|QueryParameter|fromDeployId|Lower bound deploy id|true|string||
|QueryParameter|toDeployId|Upper bound deploy id|true|string||
|QueryParameter|description|Description|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

