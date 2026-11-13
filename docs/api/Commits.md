### Commits
Commit info APIs

Both endpoints temporarily require `MANAGE_SCRIPT_TOKEN` on `SYSTEM:*`, the
existing system-administrator permission. This contains access to Teletraan's
service-wide SCM credential while repository-specific authorization is deferred.
Baseline readers, publishers, operators and environment administrators receive
HTTP 403, including comparison, pagination and hotfix views using these endpoints.
Existing authorized system administrators continue using their current credentials.
Malformed GitHub repository names return HTTP 400 after authorization.

Deploy the service update directly; this workaround requires no database migration,
new repository grants, token provisioning, or changes to the GitHub App credential.


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
