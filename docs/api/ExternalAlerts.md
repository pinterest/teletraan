### ExternalAlerts

<a name="alertstriggered"></a>
#### The alert response
```
POST /v1/envs/{envName}/{stageName}/alerts
```


##### Description
Return the alert checking result


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|
|**Query**|**actionWindow**  <br>*optional*|integer (int32)|
|**Query**|**actions**  <br>*optional*|string|
|**Body**|**body**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/x-www-form-urlencoded`


##### Produces

* `application/json`


<a name="group-roles_resource"></a>
