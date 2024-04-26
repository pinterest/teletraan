### Deploy Constraints
Deploy constraints related APIs


<a name="update"></a>
#### POST /v1/envs/{envName}/{stageName}/deploy_constraint

##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**envName**  <br>*required*||string|
|**Path**|**stageName**  <br>*required*||string|
|**Body**|**body**  <br>*required*|Deploy Constraint Object to update in database|[DeployConstraintBean](#deployconstraintbean)|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="get_3"></a>
#### Get deploy constraint info
```
GET /v1/envs/{envName}/{stageName}/deploy_constraint
```


##### Description
Returns a deploy constraint object given a constraint id


##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[DeployConstraintBean](#deployconstraintbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="delete_1"></a>
#### DELETE /v1/envs/{envName}/{stageName}/deploy_constraint

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Path**|**envName**  <br>*required*|string|
|**Path**|**stageName**  <br>*required*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**default**|successful operation|No Content|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="deploys_resource"></a>
