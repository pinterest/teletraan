### Builds
BUILD information APIs


<a name="publish"></a>
#### Publish a build
```
POST /v1/builds
```


##### Description
Publish a build given a build object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|BUILD object|[BuildBean](#buildbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_1"></a>
#### GET /v1/builds

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**after**  <br>*optional*|integer (int64)|
|**Query**|**before**  <br>*optional*|integer (int64)|
|**Query**|**branch**  <br>*optional*|string|
|**Query**|**commit**  <br>*optional*|string|
|**Query**|**name**  <br>*optional*|string|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [BuildBean](#buildbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getcurrentbuildswithgroupname"></a>
#### GET /v1/builds/current

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**group**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [BuildBean](#buildbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbuildnames"></a>
#### GET /v1/builds/names

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**filter**  <br>*optional*|string|
|**Query**|**size**  <br>*optional*|integer (int32)|
|**Query**|**start**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbranches"></a>
#### Get branches
```
GET /v1/builds/names/{name}/branches
```


##### Description
Returns a list of the repository branches associated with a given build name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**name**  <br>*required*|BUILD name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbuildswithtags"></a>
#### Get build info along with the build tag info for a given build name
```
GET /v1/builds/tags
```


##### Description
Return a bean object containing the build and the build tag


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**after**  <br>*optional*|integer (int64)|
|**Query**|**before**  <br>*optional*|integer (int64)|
|**Query**|**branch**  <br>*optional*|string|
|**Query**|**commit**  <br>*optional*|string|
|**Query**|**name**  <br>*optional*|string|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[BuildTagBean](#buildtagbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_2"></a>
#### Get build info
```
GET /v1/builds/{id}
```


##### Description
Returns a build object given a build id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|BUILD id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[BuildBean](#buildbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete"></a>
#### Delete a build
```
DELETE /v1/builds/{id}
```


##### Description
Deletes a build given a build id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|BUILD id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getwithtag"></a>
#### Get build info with its tags
```
GET /v1/builds/{id}/tags
```


##### Description
Returns a build object given a build id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|BUILD id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[BuildTagBean](#buildtagbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="commits_resource"></a>
