### Ratings

<a name="create_6"></a>
#### POST /v1/ratings

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Body**|**body**  <br>*optional*|[RatingBean](#ratingbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getall_2"></a>
#### GET /v1/ratings

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [RatingBean](#ratingbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_8"></a>
#### DELETE /v1/ratings/{id}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**id**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="checkuserfeedbackstatus"></a>
#### GET /v1/ratings/{userName}/is_eligible

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**userName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|boolean|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="schedules_resource"></a>
