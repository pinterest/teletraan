### Hosts
#### DELETE /v1/envs/{envName}/{stageName}/hosts
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|stageName||true|string||
|BodyParameter|body||false|string array||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get hosts for env stage
```
GET /v1/envs/{envName}/{stageName}/hosts
```

##### Description

Returns a Collections of hosts given an environment and stage

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get host details for stage and host name
```
GET /v1/envs/{envName}/{stageName}/hosts/{hostName}
```

##### Description

Returns a host given an environment, stage and host name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name|true|string||
|PathParameter|stageName|Stage name|true|string||
|PathParameter|hostName|Host name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostBean array|


##### Consumes

* application/json

##### Produces

* application/json

