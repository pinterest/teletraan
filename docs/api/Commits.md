### Commits

Commit info APIs

#### GET /v1/commits
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|repo||false|string||
|QueryParameter|startSha||false|string||
|QueryParameter|endSha||false|string||
|QueryParameter|size||false|integer (int32)||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|CommitBean array|


##### Consumes

* application/json

##### Produces

* application/json

#### Get commit infos
```
GET /v1/commits/{repo}/{sha}
```

##### Description

Returns a commit object given a repo and commit sha

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|repo|Commit's repo|true|string||
|PathParameter|sha|Commit SHA|true|string||


##### Responses
|HTTP Code|Description|Schema|
|----|----|----|
|200|successful operation|CommitBean|


##### Consumes

* application/json

##### Produces

* application/json

