### Environments
Environment info APIs


<a name="create_4"></a>
#### Create environment
```
POST /v1/envs
```


##### Description
Creates a new environment given an environment object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|Environment object to create in database|[EnvironBean](#environbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getall"></a>
#### Get all environment objects
```
GET /v1/envs
```


##### Description
Returns a list of environment objects related to the given environment name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Query**|**envName**  <br>*required*|Environment name|string|
|**Query**|**groupName**  <br>*optional*||string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [EnvironBean](#environbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="action_2"></a>
#### POST /v1/envs/actions

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**actionType**  <br>*required*|enum (ENABLE, DISABLE)|
|**Query**|**description**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getids"></a>
#### GET /v1/envs/ids

##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_17"></a>
#### GET /v1/envs/names

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**nameFilter**  <br>*optional*|string|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getallsidecars"></a>
#### Get all sidecar environment objects
```
GET /v1/envs/sidecars
```


##### Description
Returns a list of sidecar environment objects


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [EnvironBean](#environbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_15"></a>
#### Get an environment
```
GET /v1/envs/{envName}/{stageName}
```


##### Description
Returns an environment object given environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[EnvironBean](#environbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_10"></a>
#### Update an environment
```
PUT /v1/envs/{envName}/{stageName}
```


##### Description
Update an environment given environment and stage names with a environment object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|Desired Environment object with updates|[EnvironBean](#environbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_5"></a>
#### Delete an environment
```
DELETE /v1/envs/{envName}/{stageName}
```


##### Description
Deletes an environment given a environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="action_1"></a>
#### POST /v1/envs/{envName}/{stageName}/actions

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**actionType**  <br>*required*|enum (ENABLE, DISABLE)|
|**Query**|**description**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_5"></a>
#### Get agent configs
```
GET /v1/envs/{envName}/{stageName}/agent_configs
```


##### Description
Returns a name,value map of environment agent configs given an environment name and stage name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string, string > map|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_2"></a>
#### Update agent configs
```
PUT /v1/envs/{envName}/{stageName}/agent_configs
```


##### Description
Updates environment agent configs given an environment name and stage name with a map of name,value agent configs


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|Map of configs to update with|< string, string > map|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="add"></a>
#### Create the capacities for Group and hosts
```
POST /v1/envs/{envName}/{stageName}/capacity
```


##### Description
Create the capacities for Group and hosts


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**capacityType**  <br>*optional*|enum (GROUP, HOST)|
|**Body**|**body**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_6"></a>
#### Get the capacities for Group and hosts
```
GET /v1/envs/{envName}/{stageName}/capacity
```


##### Description
Get the capacities for Group and hosts


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**capacityType**  <br>*optional*|enum (GROUP, HOST)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_4"></a>
#### Update the capacities for Group and hosts
```
PUT /v1/envs/{envName}/{stageName}/capacity
```


##### Description
Update the capacities for Group and hosts


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**capacityType**  <br>*optional*|enum (GROUP, HOST)|
|**Body**|**body**  <br>*optional*|< string > array|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_3"></a>
#### Delete the capacities for Group and hosts
```
DELETE /v1/envs/{envName}/{stageName}/capacity
```


##### Description
Delete the capacities for Group and hosts


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**capacityType**  <br>*optional*|enum (GROUP, HOST)|
|**Body**|**body**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="setexternalid"></a>
#### Sets the external_id on a stage
```
POST /v1/envs/{envName}/{stageName}/external_id
```


##### Description
Sets the external_id column on a stage given the environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|External id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[EnvironBean](#environbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_8"></a>
#### Get the config history for the environment
```
GET /v1/envs/{envName}/{stageName}/history
```


##### Description
Get the config history for the environment


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**pageIndex**  <br>*optional*|integer (int32)|
|**Query**|**pageSize**  <br>*optional*|integer (int32)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [ConfigHistoryBean](#confighistorybean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="updateissox"></a>
#### Update an environment/stage's isSox flag
```
PUT /v1/envs/{envName}/{stageName}/is-sox/{booleanValue}
```


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**booleanValue**  <br>*required*|Is sox flag|boolean|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_12"></a>
#### Get environment metrics
```
GET /v1/envs/{envName}/{stageName}/metrics
```


##### Description
Returns a list of MetricsConfig object containing details for environment metrics gauges given an environment name and stage name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [MetricsConfigBean](#metricsconfigbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_7"></a>
#### Update environment metrics
```
PUT /v1/envs/{envName}/{stageName}/metrics
```


##### Description
Updates an environment's metrics configs given an environment name, stage name, and list of MetricsConfig objects to update with


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|List of MetricsConfigBean objects|< [MetricsConfigBean](#metricsconfigbean) > array|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_13"></a>
#### Get promote info
```
GET /v1/envs/{envName}/{stageName}/promotes
```


##### Description
Returns a promote info object given environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[PromoteBean](#promotebean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_8"></a>
#### Update promote info
```
PUT /v1/envs/{envName}/{stageName}/promotes
```


##### Description
Updates promote info given environment and stage names by given promote info object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|Promote object to update with|[PromoteBean](#promotebean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_14"></a>
#### Get script configs
```
GET /v1/envs/{envName}/{stageName}/script_configs
```


##### Description
Returns name value pairs of script configs for given environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string, string > map|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_9"></a>
#### Update script configs
```
PUT /v1/envs/{envName}/{stageName}/script_configs
```


##### Description
Updates script configs given environment and stage names with given name:value map of new script configs


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|New configs to update with|< string, string > map|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< string, string > map|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_16"></a>
#### Get webhooks object
```
GET /v1/envs/{envName}/{stageName}/web_hooks
```


##### Description
Returns a pre/post webhooks object by given environment and stage names


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[EnvWebHookBean](#envwebhookbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="update_13"></a>
#### Update webhooks
```
PUT /v1/envs/{envName}/{stageName}/web_hooks
```


##### Description
Updates pre/deploy webhooks by given environment and stage names with given webhooks object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*optional*||[EnvWebHookBean](#envwebhookbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_18"></a>
#### Get environment object
```
GET /v1/envs/{id}
```


##### Description
Returns an environment object given an environment id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**id**  <br>*required*|Environment id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[EnvironBean](#environbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="externalalerts_resource"></a>
