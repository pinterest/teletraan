### Deploys
Deploy info APIs


<a name="search"></a>
#### GET /v1/deploys

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**acceptanceStatus**  <br>*optional*|< enum (PENDING_DEPLOY, OUTSTANDING, PENDING_ACCEPT, ACCEPTED, REJECTED, TERMINATED) > array(multi)|
|**Query**|**after**  <br>*optional*|integer (int64)|
|**Query**|**before**  <br>*optional*|integer (int64)|
|**Query**|**branch**  <br>*optional*|string|
|**Query**|**commit**  <br>*optional*|string|
|**Query**|**commitDate**  <br>*optional*|integer (int64)|
|**Query**|**deployState**  <br>*optional*|< enum (RUNNING, FAILING, SUCCEEDING, SUCCEEDED, ABORTED) > array(multi)|
|**Query**|**deployType**  <br>*optional*|< enum (REGULAR, HOTFIX, ROLLBACK, RESTART, STOP) > array(multi)|
|**Query**|**envId**  <br>*optional*|< string > array(multi)|
|**Query**|**oldestFirst**  <br>*optional*|boolean|
|**Query**|**operator**  <br>*optional*|< string > array(multi)|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|
|**Query**|**repo**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployQueryResultBean](#deployqueryresultbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="dailycount"></a>
#### Get deploys per day
```
GET /v1/deploys/dailycount
```


##### Description
Get total numbers of deploys on the current day


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|integer (int64)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_4"></a>
#### Get deploy info
```
GET /v1/deploys/{id}
```


##### Description
Returns a deploy object given a deploy id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|Deploy id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployBean](#deploybean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_1"></a>
#### Update deploy
```
PUT /v1/deploys/{id}
```


##### Description
Update deploy given a deploy id and a deploy object. Current only acceptanceStatus and description are allowed to change.


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|Deploy id|string|
|**Body**|**body**  <br>*required*|Partially populated deploy object|[DeployBean](#deploybean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_2"></a>
#### Delete deploy info
```
DELETE /v1/deploys/{id}
```


##### Description
Delete deploy info given a deploy id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|Deploy id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="create"></a>
#### Create a deploy
```
POST /v1/envs/{envName}/{stageName}/deploys
```


##### Description
Creates a deploy given an environment name, stage name, build id and description


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Query**|**buildId**  <br>*required*|Build id|string|
|**Query**|**deliveryType**  <br>*optional*|Delivery type|string|
|**Query**|**description**  <br>*required*|Description|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_8"></a>
#### Get deploy info by environment
```
GET /v1/envs/{envName}/{stageName}/deploys/current
```


##### Description
Returns a deploy info object given an environment name and stage name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployBean](#deploybean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="action"></a>
#### Take deploy action
```
POST /v1/envs/{envName}/{stageName}/deploys/current/actions
```


##### Description
Take an action on a deploy such as RESTART or PAUSE


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Query**|**actionType**  <br>*required*|ActionType enum selection|enum (PROMOTE, RESTART, ROLLBACK, PAUSE, RESUME)|
|**Query**|**description**  <br>*required*|Description|string|
|**Query**|**fromDeployId**  <br>*required*|Lower bound deploy id|string|
|**Query**|**toDeployId**  <br>*required*|Upper bound deploy id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getmissinghosts"></a>
#### Get missing hosts for stage
```
GET /v1/envs/{envName}/{stageName}/deploys/current/missing-hosts
```


##### Description
Returns a list of missing hosts given an environment and stage


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="updateprogress"></a>
#### Update deploy progress
```
PUT /v1/envs/{envName}/{stageName}/deploys/current/progress
```


##### Description
Updates a deploy's progress given an environment name and stage name and returns a deploy progress object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployProgressBean](#deployprogressbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_6"></a>
#### Take a deploy action
```
PUT /v1/envs/{envName}/{stageName}/deploys/hostactions
```


##### Description
Take an action on a deploy using host information


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Query**|**actionType**  <br>*required*|Agent object to update with|enum (PAUSED_BY_USER, RESET, NORMAL)|
|**Body**|**body**  <br>*optional*||< string > array|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="environments_resource"></a>
