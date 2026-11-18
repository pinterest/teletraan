### Commits
Commit info APIs

Both endpoints require the existing `READ` permission. Before requesting SCM data,
Teletraan checks that at least one retained build records the requested repository
in the selected configured SCM. Repositories without a matching build return
HTTP 403 before any SCM request, including for administrators. Missing, ambiguous
or malformed GitHub repository parameters return HTTP 400.

The check is independent of the requested branch or commit: comparisons, pagination
and hotfix cherry-picking can read new commits once the repository has a build.
SCM defaults and configured case matching are honored; distinct configured SCM
aliases remain separate. GitHub repository names match case-insensitively.

Published build metadata defines this allowlist. Any authenticated reader can read
commits for a repository with a build; publishers can add repositories by publishing
build metadata. This workaround does not enforce per-user repository ownership.
Deleting the last build for a repository removes its commit access.

Deploy the service update directly. No database migration, new repository grants,
token provisioning, or changes to the GitHub App credential are required.


<a name="getcommits"></a>
#### GET /v1/commits

##### Parameters

|Type|Name|Schema|
|---|---|---|
|**Query**|**endSha**  <br>*optional*|string|
|**Query**|**path**  <br>*optional*|string|
|**Query**|**repo**  <br>*required*|string|
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
