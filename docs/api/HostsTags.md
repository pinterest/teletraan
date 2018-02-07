### Hosts Tags
#### List all the hosts tags
```
GET /v1/envs/{envName}/{stageName}/host_tags
```

##### Description

Returns a map group by tagValue and hosts tagged with tagName:tagValue in an environment

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|stageName||true|string||
|QueryParameter|ec2Tags||false|boolean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostTagInfo|


##### Consumes

* application/json

##### Produces

* application/json

#### List all the hosts that are tagged with tagName in an environment, and group by tagValue
```
GET /v1/envs/{envName}/{stageName}/host_tags/{tagName}
```

##### Description

Returns a map group by tagValue and hosts tagged with tagName:tagValue in an environment

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|stageName||true|string||
|PathParameter|tagName||true|string||
|QueryParameter|ec2Tags||false|boolean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostTagInfo|


##### Consumes

* application/json

##### Produces

* application/json

#### DELETE /v1/envs/{envName}/{stageName}/host_tags/{tagName}
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|stageName||true|string||
|PathParameter|tagName||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

