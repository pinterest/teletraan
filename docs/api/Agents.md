### Agents
Deploy agent information APIs


<a name="getcountbyenvname"></a>
#### GET /v1/agents/env/{envId}/total

##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envId**  <br>*required*|Env Id|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|integer (int64)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getcounttotalhosts"></a>
#### GET /v1/agents/hostcount

##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|integer (int64)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyid"></a>
#### GET /v1/agents/id/{hostId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**hostId**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [AgentBean](#agentbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="updatebyid"></a>
#### PUT /v1/agents/id/{hostId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**hostId**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[AgentBean](#agentbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get"></a>
#### Get Deploy Agent Host Info
```
GET /v1/agents/{hostName}
```


##### Description
Returns a list of all the deploy agent objects running on the specified host


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**hostName**  <br>*required*|Host name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [AgentBean](#agentbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getallagents"></a>
#### Get deploy agents
```
GET /v1/envs/{envName}/{stageName}/agents
```


##### Description
Returns a list of all the deploy agent objects for a given environment name and stage name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [AgentBean](#agentbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="countservingagents"></a>
#### GET /v1/envs/{envName}/{stageName}/agents/count

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**actionType**  <br>*required*|enum (SERVING, SERVING_AND_NORMAL, FIRST_DEPLOY, FAILED_FIRST_DEPLOY)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|integer (int64)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getagenterror"></a>
#### Get deploy agent error
```
GET /v1/envs/{envName}/{stageName}/agents/errors/{hostName}
```


##### Description
Returns an AgentError object given an environment name, stage name, and host name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**hostName**  <br>*required*|Host name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[AgentErrorBean](#agenterrorbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="resetfaileddeploys"></a>
#### Reset failed deploys
```
PUT /v1/envs/{envName}/{stageName}/agents/reset_failed_agents/{deployId}
```


##### Description
Resets failing deploys given an environment name, stage name, and deploy id


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**deployId**  <br>*required*|Deploy id|string|
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


<a name="update_3"></a>
#### Update host agent
```
PUT /v1/envs/{envName}/{stageName}/agents/{hostId}
```


##### Description
Updates host agent specified by given environment name, stage name, and host id with given agent object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**hostId**  <br>*required*|Host id|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|
|**Body**|**body**  <br>*required*|Agent object to update with|[AgentBean](#agentbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="builds_resource"></a>
