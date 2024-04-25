### User Roles
User Roles related APIs


<a name="create_3"></a>
#### Create a user for an environment
```
POST /v1/envs/{envName}/user_roles
```


##### Description
Creates a new UserRoles object for a given environment name.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Body**|**body**  <br>*required*|UserRolesBean object.|[UserRolesBean](#userrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource_2"></a>
#### Get all environment user roles
```
GET /v1/envs/{envName}/user_roles
```


##### Description
Returns a list of UserRoles objects for the given environment name.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [UserRolesBean](#userrolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource_2"></a>
#### Get user role by user and environment name
```
GET /v1/envs/{envName}/user_roles/{userName}
```


##### Description
Returns a UserRoles object containing for given user and environment names.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Path**|**userName**  <br>*required*|User name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[UserRolesBean](#userrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_12"></a>
#### Update a user's environment role
```
PUT /v1/envs/{envName}/user_roles/{userName}
```


##### Description
Updates a UserRoles object for given user and environment names with given UserRoles object.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Path**|**userName**  <br>*required*|User name.|string|
|**Body**|**body**  <br>*optional*||[UserRolesBean](#userrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[UserRolesBean](#userrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_7"></a>
#### Deletes a user's roles from an environment
```
DELETE /v1/envs/{envName}/user_roles/{userName}
```


##### Description
Deletes a UserRoles object by given user and environment names.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Host name.|string|
|**Path**|**userName**  <br>*required*||string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="create_6"></a>
#### Create a new system level user
```
POST /v1/system/user_roles
```


##### Description
Creates a system level user for given UserRoles object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|UserRolesBean object.|[UserRolesBean](#userrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource_4"></a>
#### Get all system level user role objects
```
GET /v1/system/user_roles
```


##### Description
Returns a list of all system level UserRoles objects


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [UserRolesBean](#userrolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource_4"></a>
#### Get system level user role objects by user name
```
GET /v1/system/user_roles/{userName}
```


##### Description
Returns a system level UserRoles objects containing info for given user name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**userName**  <br>*required*|Name of user|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[UserRolesBean](#userrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_15"></a>
#### Update a system level user's role
```
PUT /v1/system/user_roles/{userName}
```


##### Description
Updates a system level user's role given specified user name and replacement UserRoles object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**userName**  <br>*required*|Name of user.|string|
|**Body**|**body**  <br>*required*|UserRolesBean object|[UserRolesBean](#userrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[UserRolesBean](#userrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_9"></a>
#### Delete a system level user
```
DELETE /v1/system/user_roles/{userName}
```


##### Description
Deletes a system level user by specified user name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**userName**  <br>*required*|User name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`



