### Commits
Commit info APIs


<a name="getcommits"></a>
#### GET /v1/commits

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**endSha**  <br>*optional*|string|
|**Query**|**path**  <br>*optional*|string|
|**Query**|**repo**  <br>*optional*|string|
|**Query**|**scm**  <br>*optional*|string|
|**Query**|**size**  <br>*optional*|integer (int32)|
|**Query**|**startSha**  <br>*optional*|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|< [CommitBean](#commitbean) > array|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="getcommit"></a>
#### Get commit infos
```
GET /v1/commits/{scm}/{repo}/{sha}
```


##### Description
Returns a commit object given a repo and commit sha


##### Parameters

|Type|Name|Description|Schema|
|---|---|---|---|
|**Path**|**repo**  <br>*required*|Commit's repo|string|
|**Path**|**scm**  <br>*required*|Commit's scm type, either github or phabricator|string|
|**Path**|**sha**  <br>*required*|Commit SHA|string|


##### Responses

|HTTP Code|Description|Schema|
|---|---|---|
|**200**|successful operation|[CommitBean](#commitbean)|


##### Consumes

* `application/json`


##### Produces

* `application/json`


<a name="deploy-constraints_resource"></a>
