# Create a repository script token

Repository script tokens let an automation read commits from one source repository.
They have a `READ` role and expire after 180 days. Existing environment tokens and
system reader tokens do not inherit repository access.
Repository-token expiry is checked even when expiry checking is disabled for
legacy environment and system tokens.

The caller needs `MANAGE_SCRIPT_TOKEN` on the repository's `SCM_REPOSITORY`
resource. A repository `READ` grant does not permit token creation, listing,
inspection or revocation. Policy deployments should grant repository token
management only to the repository's designated administrators.

Create a token with a script name and the repository query parameters:

```http
POST /v1/scm_repositories/token_roles?scm=Github&repo=pinterest%2Fteletraan
Authorization: token <repository-administrator-credential>
Content-Type: application/json

{"name": "release-commit-reader"}
```

The response has HTTP status `201` and includes the bearer secret in `token`.
Save it immediately in your automation's secret store. Subsequent requests cannot
retrieve the secret. The server assigns the exact repository scope, `READ` role,
random secret and expiry; values supplied for those fields in the request body
are ignored. Script names contain 1 to 64 letters, digits, underscores or hyphens.

Use the token to read commits from the same repository:

```http
GET /v1/commits?scm=Github&repo=pinterest%2Fteletraan&startSha=main
Authorization: token <created-repository-token>
```

The token cannot read other repositories or manage tokens. An omitted or empty
`scm` query uses the service's configured default SCM. GitHub owner and repository
names are case-insensitive; configured SCM aliases remain separate resources.

Repository administrators can list metadata, inspect one token or revoke it:

```http
GET /v1/scm_repositories/token_roles?scm=Github&repo=pinterest%2Fteletraan
GET /v1/scm_repositories/token_roles/release-commit-reader?scm=Github&repo=pinterest%2Fteletraan
DELETE /v1/scm_repositories/token_roles/release-commit-reader?scm=Github&repo=pinterest%2Fteletraan
```

There is no update endpoint. To rotate a token, create a replacement under a new
script name, update the automation's secret, and revoke the old token. Revocation
affects only the named token in the specified repository. Repository tokens are
rechecked against the token store on every request, even when authentication
caching is configured, so expired or revoked tokens cannot remain valid in that cache.
