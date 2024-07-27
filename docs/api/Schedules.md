### Schedules

<a name="overridesession"></a>
#### PUT /v1/schedules/{envName}/{stageName}/override

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**sessionNumber**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="updateschedule"></a>
#### PUT /v1/schedules/{envName}/{stageName}/schedules

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[ScheduleBean](#schedulebean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getschedule"></a>
#### GET /v1/schedules/{envName}/{stageName}/{scheduleId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**scheduleId**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[ScheduleBean](#schedulebean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="script-tokens_resource"></a>
