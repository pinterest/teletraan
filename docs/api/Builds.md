### Builds

Build information APIs

#### GET /v1/builds
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|commit||false|string||
|QueryParameter|name||false|string||
|QueryParameter|branch||false|string||
|QueryParameter|pageIndex||false|integer (int32)||
|QueryParameter|pageSize||false|integer (int32)||
|QueryParameter|before||false|integer (int64)||
|QueryParameter|after||false|integer (int64)||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|BuildBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Publish a build
```
POST /v1/builds
```

##### Description

Publish a build given a build object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|Build object|true|BuildBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get branches
```
GET /v1/builds/names/{name}/branches
```

##### Description

Returns a list of the repository branches associated with a given build name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|name|Build name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get build info
```
GET /v1/builds/{id}
```

##### Description

Returns a build object given a build id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Build id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|BuildBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete a build
```
DELETE /v1/builds/{id}
```

##### Description

Deletes a build given a build id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|Build id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### GET /v1/builds/names
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|filter||false|string||
|QueryParameter|start||false|integer (int32)||
|QueryParameter|size||false|integer (int32)||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string array|


##### Consumes

* application/json

##### Produces

* application/json

