# Teletraan Auth Model YAML/JSON Schema

To help reducing error when manually editing the Teletraan acl.yaml file, a Json schema is crafted to help validation.

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
