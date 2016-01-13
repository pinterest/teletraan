### Agents

Deploy agent information APIs

#### Get Deploy Agent Host Info
```
GET /v1/agents/{hostName}
```

##### Description

Returns a list of all the deploy agent objects running on the specified host

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostName|Host name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|AgentBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get deploy agents
```
GET /v1/envs/{envName}/{stageName}/agents
```

##### Description

Returns a list of all the deploy agent objects for a given environment name and stage name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|AgentBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Reset failed deploys
```
PUT /v1/envs/{envName}/{stageName}/agents/reset_failed_agents/{deployId}
```

##### Description

Resets failing deploys given an environment name, stage name, and deploy id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|PathParameter|deployId|Deploy id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get deploy agent error
```
GET /v1/envs/{envName}/{stageName}/agents/errors/{hostName}
```

##### Description

Returns an AgentError object given an environment name, stage name, and host name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|PathParameter|hostName|Host name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|AgentErrorBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update host agent
```
PUT /v1/envs/{envName}/{stageName}/agents/{hostId}
```

##### Description

Updates host agent specified by given environment name, stage name, and host id with given agent object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|PathParameter|hostId|Host id|true|string||
|BodyParameter|body|Agent object to update with|true|AgentBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

