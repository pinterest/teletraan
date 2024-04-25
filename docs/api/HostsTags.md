### Hosts Tags
Hosts Tags related APIs


<a name="get_10"></a>
#### List all the hosts tags
```
GET /v1/envs/{envName}/{stageName}/host_tags
```


##### Description
Returns a list the host tags in an environment


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**ec2Tags**  <br>*optional*|boolean|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[HostTagInfo](#hosttaginfo)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_11"></a>
#### List all the hosts that are tagged with tagName in an environment, and group by tagValue
```
GET /v1/envs/{envName}/{stageName}/host_tags/{tagName}
```


##### Description
Returns a map group by tagValue and hosts tagged with tagName:tagValue in an environment


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Path**|**tagName**  <br>*required*|string|
|**Query**|**ec2Tags**  <br>*optional*|boolean|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[HostTagInfo](#hosttaginfo)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="removehosttags"></a>
#### DELETE /v1/envs/{envName}/{stageName}/host_tags/{tagName}

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Path**|**tagName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="hosts-and-systems_resource"></a>
