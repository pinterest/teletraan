### Specs

Spec info APIs

#### Get all security groups
```
GET /v1/specs/security_groups
```

##### Description

Returns a list of all security groups

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get instance types
```
GET /v1/specs/instance_types
```

##### Description

Returns a list of all instance types

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|string array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get all subnets
```
GET /v1/specs/subnets
```

##### Description

Returns a list of all spec subnet info objects

##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|SpecBean array|


##### Consumes

* application/json

##### Produces

* application/json

