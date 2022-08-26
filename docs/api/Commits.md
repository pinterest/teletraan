### Commits

Commit info APIs

#### GET /v1/commits
##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|QueryParameter|scm||false|string||
|QueryParameter|repo||false|string||
|QueryParameter|startSha||false|string||
|QueryParameter|endSha||false|string||
|QueryParameter|size||false|integer (int32)||
|QueryParameter|path||false|string||


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
GET /v1/commits/{scm}/{repo}/{sha}
```

##### Description

Returns a commit object given a repo and commit sha

##### Parameters
|Type|Name|Description|Required|Schema|Default|
|----|----|----|----|----|----|
|PathParameter|scm|Commit's scm type, either github or phabricator|true|string||
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

