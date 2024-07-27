### Hotfixs

<a name="create_5"></a>
#### POST /v1/hotfixs

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Body**|**body**  <br>*optional*|[HotfixBean](#hotfixbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getall_1"></a>
#### GET /v1/hotfixs

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**envName**  <br>*optional*|string|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HotfixBean](#hotfixbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_21"></a>
#### GET /v1/hotfixs/{id}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[HotfixBean](#hotfixbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_15"></a>
#### PUT /v1/hotfixs/{id}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[HotfixBean](#hotfixbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="ratings_resource"></a>
