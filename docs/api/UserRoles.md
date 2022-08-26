### User Roles

User Roles related APIs

#### Delete a system level user
```
DELETE /v1/system/user_roles/{userName}
```

##### Description

Deletes a system level user by specified user name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|userName|User name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get system level user role objects by user name
```
GET /v1/system/user_roles/{userName}
```

##### Description

Returns a system level UserRoles objects containing info for given user name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|userName|Name of user|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update a system level user's role
```
PUT /v1/system/user_roles/{userName}
```

##### Description

Updates a system level user's role given specified user name and replacement UserRoles object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|userName|Name of user.|true|string||
|BodyParameter|body|UserRolesBean object|true|UserRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Get all system level user role objects
```
GET /v1/system/user_roles
```

##### Description

Returns a list of all system level UserRoles objects

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a new system level user
```
POST /v1/system/user_roles
```

##### Description

Creates a system level user for given UserRoles object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|UserRolesBean object.|true|UserRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get all environment user roles
```
GET /v1/envs/{envName}/user_roles
```

##### Description

Returns a list of UserRoles objects for the given environment name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a user for an environment
```
POST /v1/envs/{envName}/user_roles
```

##### Description

Creates a new UserRoles object for a given environment name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|BodyParameter|body|UserRolesBean object.|true|UserRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Deletes a user's roles from an environment
```
DELETE /v1/envs/{envName}/user_roles/{userName}
```

##### Description

Deletes a UserRoles object by given user and environment names.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Host name.|true|string||
|PathParameter|userName||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get user role by user and environment name
```
GET /v1/envs/{envName}/user_roles/{userName}
```

##### Description

Returns a UserRoles object containing for given user and environment names.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|PathParameter|userName|User name.|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update a user's environment role
```
PUT /v1/envs/{envName}/user_roles/{userName}
```

##### Description

Updates a UserRoles object for given user and environment names with given UserRoles object.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|PathParameter|userName|User name.|true|string||
|BodyParameter|body||false|UserRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|UserRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

