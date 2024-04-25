### Tags
Tagging APIs


<a name="create_7"></a>
#### Create a tag
```
POST /v1/tags
```


##### Description
Create a tag on an object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|Tag object|[TagBean](#tagbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbytargetid"></a>
#### Get tags applied on a target id
```
GET /v1/tags/targets/{id}
```


##### Description
Return a list of TagBean objects


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< object > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getlatestbytargetid"></a>
#### Get tags applied on a target id
```
GET /v1/tags/targets/{id}/latest
```


##### Description
Return a list of TagBean objects


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< object > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyvalue"></a>
#### Get tags with the given value
```
GET /v1/tags/values/{value}
```


##### Description
Return a list of TagBean object with given value


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**value**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< object > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyid_2"></a>
#### Get tags with a given id
```
GET /v1/tags/{id}
```


##### Description
Return a TagBean objects


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[TagBean](#tagbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_10"></a>
#### Delete a tag
```
DELETE /v1/tags/{id}
```


##### Description
Deletes a build given a tag id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|tag id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="user-roles_resource"></a>
