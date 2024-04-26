### System Group Roles

<a name="create_7"></a>
#### POST /v1/system/group_roles

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Body**|**body**  <br>*optional*|[GroupRolesBean](#grouprolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyresource_3"></a>
#### GET /v1/system/group_roles

##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [GroupRolesBean](#grouprolesbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbynameandresource_3"></a>
#### GET /v1/system/group_roles/{groupName}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**groupName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[GroupRolesBean](#grouprolesbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_16"></a>
#### PUT /v1/system/group_roles/{groupName}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**groupName**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[GroupRolesBean](#grouprolesbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_9"></a>
#### DELETE /v1/system/group_roles/{groupName}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**groupName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="tags_resource"></a>
