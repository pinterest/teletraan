### Hosts

<a name="get"></a>
#### Get hosts for env stage
```
GET /v1/envs/{envName}/{stageName}/hosts
```


##### Description
Returns a Collections of hosts given an environment and stage


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HostBean](#hostbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="stopserviceonhost"></a>
#### DELETE /v1/envs/{envName}/{stageName}/hosts

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Body**|**body**  <br>*optional*|< string > array|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="gethostbyhostname"></a>
#### Get host details for stage and host name
```
GET /v1/envs/{envName}/{stageName}/hosts/{hostName}
```


##### Description
Returns a host given an environment, stage and host name


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*|Environment name|string|
|**Path**|**hostName**  <br>*required*|Host name|string|
|**Path**|**stageName**  <br>*required*|Stage name|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [HostBean](#hostbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="hosts-tags_resource"></a>
