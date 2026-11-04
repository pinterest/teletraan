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
