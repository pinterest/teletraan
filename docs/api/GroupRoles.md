### Group Roles

Group Roles related APIs

#### Get all environment group roles
```
GET /v1/envs/{envName}/group_roles
```

##### Description

Returns a list of GroupRoles objects for the given environment name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|GroupRolesBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Create a group role for an environment
```
POST /v1/envs/{envName}/group_roles
```

##### Description

Creates a new GroupRoles object for a given environment name.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName|Environment name.|true|string||
|BodyParameter|body|GroupRolesBean object.|true|GroupRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Deletes a group role from an environment
```
DELETE /v1/envs/{envName}/group_roles/{groupName}
```

##### Description

Deletes a GroupRoles object by given group and environment names.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|groupName||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get group role by group and environment name
```
GET /v1/envs/{envName}/group_roles/{groupName}
```

##### Description

Returns a GroupRoles object containing for given group and environment names.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|groupName||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|GroupRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

#### Update an environment's group role
```
PUT /v1/envs/{envName}/group_roles/{groupName}
```

##### Description

Updates a GroupRoles object for given group and environment names with given GroupRoles object.

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|envName||true|string||
|PathParameter|groupName||true|string||
|BodyParameter|body||false|GroupRolesBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|GroupRolesBean|


##### Consumes

* application/json

##### Produces

* application/json

