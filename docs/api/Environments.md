### Environments

Environment info APIs

#### Get webhooks object
```
GET /v1/envs/{envName}/{stageName}/web_hooks
```

##### Description

Returns a pre/post webhooks object by given environment and stage names

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|EnvWebHookBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update webhooks
```
PUT /v1/envs/{envName}/{stageName}/web_hooks
```

##### Description

Updates pre/deploy webhooks by given environment and stage names with given webhooks object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body||false|EnvWebHookBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get all environment objects
```
GET /v1/envs
```

##### Description

Returns a list of environment objects related to the given environment name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|envName|Environment name|true|string||
|QueryParameter|groupName||false|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|EnvironBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create environment
```
POST /v1/envs
```

##### Description

Creates a new environment given an environment object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|Environment object to create in database|true|EnvironBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get an environment
```
GET /v1/envs/{envName}/{stageName}
```

##### Description

Returns an environment object given environment and stage names

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|EnvironBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete an environment
```
DELETE /v1/envs/{envName}/{stageName}
```

##### Description

Deletes an environment given a environment and stage names

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Update an environment
```
PUT /v1/envs/{envName}/{stageName}
```

##### Description

Update an environment given environment and stage names with a environment object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body|Desired Environment object with updates|true|EnvironBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get environment metrics
```
GET /v1/envs/{envName}/{stageName}/metrics
```

##### Description

Returns a list of MetricsConfig object containing details for environment metrics gauges given an environment name and stage name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|MetricsConfigBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Update environment metrics
```
PUT /v1/envs/{envName}/{stageName}/metrics
```

##### Description

Updates an environment's metrics configs given an environment name, stage name, and list of MetricsConfig objects to update with

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body|List of MetricsConfigBean objects|true|MetricsConfigBean array||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get agent configs
```
GET /v1/envs/{envName}/{stageName}/agent_configs
```

##### Description

Returns a name,value map of environment agent configs given an environment name and stage name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object|


##### Consumes

* application/json

##### Produces

* application/json

#### Update agent configs
```
PUT /v1/envs/{envName}/{stageName}/agent_configs
```

##### Description

Updates environment agent configs given an environment name and stage name with a map of name,value agent configs

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body|Map of configs to update with|true|object||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get script configs
```
GET /v1/envs/{envName}/{stageName}/script_configs
```

##### Description

Returns name value pairs of script configs for given environment and stage names

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object|


##### Consumes

* application/json

##### Produces

* application/json

#### Update script configs
```
PUT /v1/envs/{envName}/{stageName}/script_configs
```

##### Description

Updates script configs given environment and stage names with given name:value map of new script configs

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body|New configs to update with|true|object||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object|


##### Consumes

* application/json

##### Produces

* application/json

#### Get promote info
```
GET /v1/envs/{envName}/{stageName}/promotes
```

##### Description

Returns a promote info object given environment and stage names

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|PromoteBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update promote info
```
PUT /v1/envs/{envName}/{stageName}/promotes
```

##### Description

Updates promote info given environment and stage names by given promote info object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|BodyParameter|body|Promote object to update with|true|PromoteBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### GET /v1/envs/names
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|nameFilter||false|string||
|QueryParameter|pageIndex||false|integer (int32)||
|QueryParameter|pageSize||false|integer (int32)||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get environment object
```
GET /v1/envs/{id}
```

##### Description

Returns an environment object given an environment id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Environment id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|EnvironBean|


##### Consumes

* application/json

##### Produces

* application/json

