### Hosts And Systems

Host info APIs

#### POST /v1/hosts
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body||false|HostBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Ping operation for agent 
```
POST /v1/system/ping
```

##### Description

Returns a deploy goal object given a ping request object

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|Ping request object|true|PingRequestBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|PingResponseBean|


##### Consumes

* application/json

##### Produces

* application/json

#### PUT /v1/hosts/{hostId}
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostId||true|string||
|BodyParameter|body||false|HostBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### DELETE /v1/hosts/{hostId}
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostId||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get host info objects by host name
```
GET /v1/hosts/{hostName}
```

##### Description

Returns a list of host info objects given a host name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostName|Host name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get SCM commit link template
```
GET /v1/system/scm_link_template
```

##### Description

Returns a Source Control Manager specific commit link template.

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string|


##### Consumes

* application/json

##### Produces

* application/json

#### Send chat message
```
POST /v1/system/send_chat_message
```

##### Description

Sends a chatroom message given a ChatMessageRequest to configured chat client

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|ChatMessageRequest object|true|ChatMessageBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|default|successful operation|No Content|


##### Consumes

* application/json

##### Produces

* application/json

#### Get a set of deploy candidates to deploy
```
POST /v1/system/ping/alldeploycandidates
```

##### Description

Returns a list of build bean

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|BodyParameter|body|Ping request object|true|PingRequestBean||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|DeployCandidatesResponse|


##### Consumes

* application/json

##### Produces

* application/json

#### Get SCM url
```
GET /v1/system/scm_url
```

##### Description

Returns a Source Control Manager Url.

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string|


##### Consumes

* application/json

##### Produces

* application/json

#### Get all host info
```
GET /v1/system/get_host/{hostName}
```

##### Description

Returns a list of host info objects given a host name

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostName|Host name|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### GET /v1/hosts/id/{hostId}
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|hostId||true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|HostBean array|


##### Consumes

* application/json

##### Produces

* application/json

