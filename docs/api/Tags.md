### Tags

Tagging APIs

#### Get tags applied on a target id
```
GET /v1/tags/targets/{id}
```

##### Description

Return a list of TagBean objects

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get tags with the given value
```
GET /v1/tags/values/{value}
```

##### Description

Return a list of TagBean object with given value

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|value||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a tag
```
POST /v1/tags
```

##### Description

Create a tag on an object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|Tag object|true|TagBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get tags with a given id
```
GET /v1/tags/{id}
```

##### Description

Return a TagBean objects

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|TagBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete a tag
```
DELETE /v1/tags/{id}
```

##### Description

Deletes a build given a tag id

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id|tag id|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get tags applied on a target id
```
GET /v1/tags/targets/{id}/latest
```

##### Description

Return a list of TagBean objects

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|id||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|object array|


##### Consumes

* application/json

##### Produces

* application/json

