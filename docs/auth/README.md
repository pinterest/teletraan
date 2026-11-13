# Teletraan Auth Model YAML/JSON Schema

To help reducing error when manually editing the Teletraan acl.yaml file, a Json schema is crafted to help validation.

## Tag and PinDeploy mutations

Tag mutations require permission on the tagged resource. The server resolves build and
environment IDs to their authorization resources; deleting a tag uses its stored target.

| Endpoint | Action | Authorization resource |
| --- | --- | --- |
| `POST /v1/tags` | `WRITE` | Tagged build, environment/stage, or `SYSTEM` for a `TELETRAAN` tag |
| `DELETE /v1/tags/{id}` | `DELETE` | Stored tag's build, environment/stage, or `SYSTEM` |
| `POST /v1/pindeploy/enable` | `WRITE` or `MANAGE_PINDEPLOY` | Environment/stage specified by `envName` and `stageName` |
| `DELETE /v1/pindeploy/disable` | `DELETE` or `MANAGE_PINDEPLOY` | `SYSTEM` |

PinDeploy disable removes every mapping for the supplied pipeline, which can span
environments. It therefore requires system-wide permission. An environment-scoped
script token cannot disable a pipeline. Read-only access does not allow these mutations.

`MANAGE_PINDEPLOY` is a policy-only action accepted by the two PinDeploy mutation
endpoints. A policy can grant this action to the PinDeploy service without granting
general `WRITE` or `DELETE` access. Existing `WRITE`/`DELETE` permissions remain valid.
It is not a numeric script-token role: legacy user and script-token authorizers use
the existing `WRITE`/`DELETE` alternatives. Publish the corresponding authorization
policy before deploying the server change; see the [rollout guide](roll-out-mutation-authorization.md).

Build publication permission (`PUBLISHER`) does not authorize tag mutations. Existing
automation needs explicit `WRITE`/`DELETE` permission for its tag targets. Environment
ownership also does not imply permission to mutate build tags, which can affect
deployments across environments. Verify the intended users' and services' build
permissions in the deployment's authorization policy before rolling out this change.
Build-scoped script tokens match the build name, not its ID or artifact metadata.
The same scope check applies to `DELETE /v1/builds/{id}`; environment-scoped cleanup
scripts need a matching build or system grant to delete builds.

Dashboard rollback runs before optional `BAD_BUILD` tagging. If tagging is denied,
the rollback still succeeds and the dashboard warns that the build was not marked
bad. The standalone GOOD/BAD/CERTIFIED tag buttons continue to require build `WRITE`.

Deleting a stored tag whose target cannot be recovered requires `DELETE` on `SYSTEM`.
This lets system-authorized callers clean up orphaned environment tags or historical
build tags with missing or invalid metadata, and stored environment tags with an
unrecoverable environment/stage name. Database lookup failures still reject
the operation, and creating a tag still requires a valid target.

## Examples

There are 3 provided examples under the *examples/* directory. If you open them in VSCode, you will see they are validated against the schema hosted on Github.

Alternatively, you can copy the schema to your local directory, and reference it like below.
Note the first line `# yaml-language-server: $schema=teletraan_auth.yaml` is only supported in VSCode.
If you use other code editors, you might need additional configurations to make the validation work.

```yaml
# yaml-language-server: $schema=teletraan_auth.yaml
# The following is an example of a valid Teletraan & Rodimus Authorization payload
input:
  principal:
    type: USER
    id: Jane Doe
    groups:
      - engineer
      - SRE
  action: READ
  resource:
    type: ENV_STAGE
    name: teletraan-prod
```

## Commit repository permissions

Both commit endpoints require `READ` on an `SCM_REPOSITORY` resource. Its
name is `<configured-scm-name>/<repository>`. An omitted or empty query
`scm` uses the configured default. SCM lookups honor the configured map's
case-insensitive matching and use the configured spelling in permission names.
GitHub owner and repository names are lowercase in authorization identities;
for example, `Github/pinterest/teletraan` when the configured key is `Github`.
Distinct configured aliases remain separate resources, including aliases for
different GitHub hosts. Reserved characters in unusual SCM keys are percent-encoded
in the permission name so a slash in a key cannot overlap another repository's scope.

Repository grants are independent of build and environment grants. In role
authorization mode, a system-wide `READ` or `PUBLISHER` grant does not grant
repository access. An explicit repository grant or a system `ADMIN` grant
does. For policy-based authorization, grant repository access explicitly with
a role that includes `READ`, using the same structure as the
[ACL example](examples/acl_example.yaml):

```yaml
resourceACLs:
  "SCM_REPOSITORY:Github/pinterest/teletraan":
    resourceName: Github/pinterest/teletraan
    resourceType: SCM_REPOSITORY
    groups:
      - groupName: teletraan-developers
        role: reader
```

Use a group and role defined by your deployment. Repository token administration
requires `MANAGE_SCRIPT_TOKEN` on that same resource; a `READ` grant does not
permit token creation or inspection. See [repository script tokens](scm-repository-tokens.md)
for the supported provisioning and rotation API. Script-token authorization uses
the token's exact stored scope; adding its name to a policy ACL does not expand it.

Repository permission names are limited to 175 characters, including the SCM
prefix and separators. This accommodates GitHub's maximum owner and repository
name lengths with ordinary SCM aliases while preserving older MySQL index limits.
Use a shorter configured alias if its full resource name would exceed the limit.
An oversized name or malformed GitHub repository returns HTTP 400 before any
GitHub request. Valid Git references remain safely encoded as request parameters
or a single URL segment.

Follow the [rollout guide](roll-out-repository-authorization.md) for the database
migration, policy grants and automation-token transition. Requests without a
matching grant return HTTP 403, including comparison, pagination and hotfix views
that use these endpoints.
