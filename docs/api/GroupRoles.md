### Group Roles
Group Roles related APIs


<a name="create_1"></a>
#### Create a group role for an environment
```
POST /v1/envs/{envName}/group_roles
```


##### Description
Creates a new GroupRoles object for a given environment name.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Body**|**body**  <br>*required*|GroupRolesBean object.|[GroupRolesBean](#grouprolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource"></a>
#### Get all environment group roles
```
GET /v1/envs/{envName}/group_roles
```


##### Description
Returns a list of GroupRoles objects for the given environment name.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [GroupRolesBean](#grouprolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource"></a>
#### Get group role by group and environment name
```
GET /v1/envs/{envName}/group_roles/{groupName}
```


##### Description
Returns a GroupRoles object containing for given group and environment names.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**groupName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[GroupRolesBean](#grouprolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_7"></a>
#### Update an environment's group role
```
PUT /v1/envs/{envName}/group_roles/{groupName}
```


##### Description
Updates a GroupRoles object for given group and environment names with given GroupRoles object.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**groupName**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[GroupRolesBean](#grouprolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[GroupRolesBean](#grouprolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_4"></a>
#### Deletes a group role from an environment
```
DELETE /v1/envs/{envName}/group_roles/{groupName}
```


##### Description
Deletes a GroupRoles object by given group and environment names.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**groupName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="hosts_resource"></a>
