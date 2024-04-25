### Script Tokens
Internal script tokens APIs


<a name="create_2"></a>
#### Create an environment script token
```
POST /v1/envs/{envName}/token_roles
```


##### Description
Creates an environment script token with given environment name and TokenRoles object.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Body**|**body**  <br>*required*|TokenRolesBean object.|[TokenRolesBean](#tokenrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource_1"></a>
#### Get environment TokenRoles objects
```
GET /v1/envs/{envName}/token_roles
```


##### Description
Returns all the TokenRoles objects for a given environment.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [TokenRolesBean](#tokenrolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource_1"></a>
#### Get TokenRoles object by script and environment names
```
GET /v1/envs/{envName}/token_roles/{scriptName}
```


##### Description
Returns a TokenRoles object given a script and environment name.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Path**|**scriptName**  <br>*required*|Script name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[TokenRolesBean](#tokenrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_12"></a>
#### Update an envrionment's script token
```
PUT /v1/envs/{envName}/token_roles/{scriptName}
```


##### Description
Update a specific environment script token given environment and script names.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Path**|**scriptName**  <br>*required*|Script name.|string|
|**Body**|**body**  <br>*optional*||[TokenRolesBean](#tokenrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_6"></a>
#### Delete an environment script token
```
DELETE /v1/envs/{envName}/token_roles/{scriptName}
```


##### Description
Deletes a script token by given environment and script name.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name.|string|
|**Path**|**scriptName**  <br>*required*|Script name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="create_8"></a>
#### Create a system script token
```
POST /v1/system/token_roles
```


##### Description
Creates a specified system wide TokenRole and returns a Response object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|TokenRolesBean object.|[TokenRolesBean](#tokenrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource_4"></a>
#### Get system script tokens
```
GET /v1/system/token_roles
```


##### Description
Returns all system TokenRoles objects


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [TokenRolesBean](#tokenrolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource_4"></a>
#### Get system TokenRoles object by script name
```
GET /v1/system/token_roles/{scriptName}
```


##### Description
Returns a TokenRoles object for given script name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**scriptName**  <br>*required*|Script name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[TokenRolesBean](#tokenrolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_17"></a>
#### Update a system script token
```
PUT /v1/system/token_roles/{scriptName}
```


##### Description
Updates a TokenRoles object by given script name and replacement TokenRoles object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**scriptName**  <br>*required*|Script name.|string|
|**Body**|**body**  <br>*required*|TokenRolesBean object.|[TokenRolesBean](#tokenrolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_10"></a>
#### Delete a system wide script token
```
DELETE /v1/system/token_roles/{scriptName}
```


##### Description
Deletes a system wide TokenRoles object by specified script name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**scriptName**  <br>*required*|Script name.|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="system-group-roles_resource"></a>
