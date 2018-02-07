### Script Tokens

Internal script tokens APIs

#### Get TokenRoles object by script and environment names
```
GET /v1/envs/{envName}/token_roles/{scriptName}
```

##### Description

Returns a TokenRoles object given a script and environment name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|PathParameter|scriptName|Script name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|TokenRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update an envrionment's script token
```
PUT /v1/envs/{envName}/token_roles/{scriptName}
```

##### Description

Update a specific environment script token given environment and script names.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|PathParameter|scriptName|Script name.|true|string||
|BodyParameter|body||false|TokenRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete an environment script token
```
DELETE /v1/envs/{envName}/token_roles/{scriptName}
```

##### Description

Deletes a script token by given environment and script name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|PathParameter|scriptName|Script name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get system script tokens
```
GET /v1/system/token_roles
```

##### Description

Returns all system TokenRoles objects

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|TokenRolesBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a system script token
```
POST /v1/system/token_roles
```

##### Description

Creates a specified system wide TokenRole and returns a Response object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|TokenRolesBean object.|true|TokenRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get environment TokenRoles objects
```
GET /v1/envs/{envName}/token_roles
```

##### Description

Returns all the TokenRoles objects for a given environment.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|TokenRolesBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create an environment script token
```
POST /v1/envs/{envName}/token_roles
```

##### Description

Creates an environment script token with given environment name and TokenRoles object.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|BodyParameter|body|TokenRolesBean object.|true|TokenRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get system TokenRoles object by script name
```
GET /v1/system/token_roles/{scriptName}
```

##### Description

Returns a TokenRoles object for given script name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|scriptName|Script name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|TokenRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update a system script token
```
PUT /v1/system/token_roles/{scriptName}
```

##### Description

Updates a TokenRoles object by given script name and replacement TokenRoles object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|scriptName|Script name.|true|string||
|BodyParameter|body|TokenRolesBean object.|true|TokenRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Delete a system wide script token
```
DELETE /v1/system/token_roles/{scriptName}
```

##### Description

Deletes a system wide TokenRoles object by specified script name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|scriptName|Script name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

