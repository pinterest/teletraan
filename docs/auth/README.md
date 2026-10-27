# Teletraan Auth Model YAML/JSON Schema

To help reducing error when manually editing the Teletraan acl.yaml file, a Json schema is crafted to help validation.

## Tag and PinDeploy mutations

Tag mutations require permission on the tagged resource. The server resolves build and
environment IDs to their authorization resources; deleting a tag uses its stored target.

| Endpoint | Action | Authorization resource |
| --- | --- | --- |
| `POST /v1/tags` | `WRITE` | Tagged build, environment/stage, or `SYSTEM` for a `TELETRAAN` tag |
| `DELETE /v1/tags/{id}` | `DELETE` | Stored tag's build, environment/stage, or `SYSTEM` |
| `POST /v1/pindeploy/enable` | `WRITE` | Environment/stage specified by `envName` and `stageName` |
| `DELETE /v1/pindeploy/disable` | `DELETE` | `SYSTEM` |

PinDeploy disable removes every mapping for the supplied pipeline, which can span
environments. It therefore requires system-wide permission. An environment-scoped
script token cannot disable a pipeline. Read-only access does not allow these mutations.

Build publication permission (`PUBLISHER`) does not authorize tag mutations. Existing
automation needs explicit `WRITE`/`DELETE` permission for its tag targets. Environment
ownership also does not imply permission to mutate build tags, which can affect
deployments across environments. Verify the intended users' and services' build
permissions in the deployment's authorization policy before rolling out this change.

Deleting a stored tag whose target cannot be recovered requires `DELETE` on `SYSTEM`.
This lets system-authorized callers clean up orphaned environment tags or historical
build tags with missing or invalid metadata. Database lookup failures still reject
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
