### ExternalAlerts
#### The alert response
```
POST /v1/envs/{envName}/{stageName}/alerts
```

##### Description

Return the alert checking result

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|stageName||true|string||
|QueryParameter|actionWindow||false|integer (int32)||
|QueryParameter|actions||false|string||
|BodyParameter|body||false|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/x-www-form-urlencoded

##### Produces

* application/json

