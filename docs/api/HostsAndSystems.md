### Hosts And Systems
Host info APIs


<a name="addhost"></a>
#### POST /v1/hosts

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Body**|**body**  <br>*optional*|[HostBean](#hostbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getbyid"></a>
#### GET /v1/hosts/id/{hostId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**hostId**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HostBean](#hostbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="updatehost"></a>
#### PUT /v1/hosts/{hostId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**hostId**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|[HostBean](#hostbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="stophost"></a>
#### DELETE /v1/hosts/{hostId}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**hostId**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get"></a>
#### Get host info objects by host name
```
GET /v1/hosts/{hostName}
```


##### Description
Returns a list of host info objects given a host name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**hostName**  <br>*required*|Host name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HostBean](#hostbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="gethosts"></a>
#### Get all host info
```
GET /v1/system/get_host/{hostName}
```


##### Description
Returns a list of host info objects given a host name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**hostName**  <br>*required*|Host name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HostBean](#hostbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="ping"></a>
#### Ping operation for agent 
```
POST /v1/system/ping
```


##### Description
Returns a deploy goal object given a ping request object


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|Ping request object|[PingRequestBean](#pingrequestbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[PingResponseBean](#pingresponsebean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getdeploycandidates"></a>
#### Get a set of deploy candidates to deploy
```
POST /v1/system/ping/alldeploycandidates
```


##### Description
Returns a list of build bean


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|Ping request object|[PingRequestBean](#pingrequestbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployCandidatesResponse](#deploycandidatesresponse)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getscmlinktemplate"></a>
#### Get SCM commit link template
```
GET /v1/system/scm_link_template
```


##### Description
Returns a Source Control Manager specific commit link template.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**scm**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|string|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getscmurl"></a>
#### Get SCM url
```
GET /v1/system/scm_url
```


##### Description
Returns a Source Control Manager Url.


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**scm**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|string|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="sendchatmessage"></a>
#### Send chat message
```
POST /v1/system/send_chat_message
```


##### Description
Sends a chatroom message given a ChatMessageRequest to configured chat client


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Body**|**body**  <br>*required*|ChatMessageRequest object|[ChatMessageBean](#chatmessagebean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="script-tokens_resource"></a>
