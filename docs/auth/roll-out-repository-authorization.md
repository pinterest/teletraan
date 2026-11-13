# Roll out repository authorization

This guide is for operators deploying the commit-endpoint authorization change.
It covers the database prerequisite, policy grants and automation credentials.

## Prepare the database

Apply the existing schema upgrades through version 24 first. Back up the three
role tables, then apply `deploy-service/common/src/main/resources/sql/schema-update-25.sql`
with your normal database connection settings. For a local database named `deploy`:

```bash
mysql deploy < deploy-service/common/src/main/resources/sql/schema-update-25.sql
```

Migration 25 widens `resource_id` in `users_and_roles`, `groups_and_roles` and
`tokens_and_roles` from 64 to 175 characters. It preserves their charset,
collation, complete primary-key uniqueness and existing grants. Each existing
`utf8` composite key remains at most 765 bytes, within older InnoDB's 767-byte
limit. The fresh-install schema already uses the wider columns. The migration
ends by setting `schema_versions.version` to 25.

## Grant repository permissions

List the repositories used by commit comparisons, pagination, hotfix views and
automation. For each, use the exact configured SCM key followed by the repository;
GitHub owner and repository names are lowercase. Case variants of a configured
SCM key resolve to its configured spelling. Reserved characters in unusual SCM
keys are percent-encoded in the permission name.

Publish explicit `READ` grants for the intended users and services. Policy engines
that provision resource types separately must also recognize `SCM_REPOSITORY`.
Existing environment or build permissions do not imply repository access. In
legacy role authorization, system administrators retain access; system readers,
publishers and environment administrators need an explicit repository grant.

Grant `MANAGE_SCRIPT_TOKEN` on a repository only to its designated token
administrators. Policy-based system administrator roles also need a matching
grant for this new resource type; the service does not assume a policy's global
administrator fallback covers it.

Legacy role deployments store repository grants in `users_and_roles` and
`groups_and_roles`. Their existing user/group administration APIs address only
environment and system resources, so provision repository grants through a
trusted database administration connection. For example, grant one group read
access and a separate group token-management access:

```sql
INSERT INTO groups_and_roles (group_name, resource_id, resource_type, role)
VALUES ('teletraan-developers', 'Github/pinterest/teletraan', 'SCM_REPOSITORY', 'READ');

INSERT INTO groups_and_roles (group_name, resource_id, resource_type, role)
VALUES ('repository-token-admins', 'Github/pinterest/teletraan', 'SCM_REPOSITORY', 'MANAGE_SCRIPT_TOKEN');
```

Replace the example groups and canonical repository name with your deployment's
values. Individual grants use the same resource fields in `users_and_roles`,
with `user_name` instead of `group_name`. Inspect existing rows before inserting
or changing a grant.

## Transition automation credentials

Deploy the service after the database migration and policy grants are ready.
Use the [repository-token API](scm-repository-tokens.md) to create a read-only
token for each repository an automation needs, then store it in that automation's
secret store and update the caller. Existing environment and system-reader tokens
cannot gain repository access through a policy ACL because script-token scopes
come from the token store.

Coordinate the deployment and caller switch: an old reader token starts returning
HTTP 403 on commit endpoints as soon as enforcement reaches its service instance.
The new provisioning API becomes available with this service version, so plan a
maintenance window or direct the transition through an upgraded service instance.
Old service instances do not understand the new token scope; route a new token
only to upgraded instances. Revoke replaced tokens after checking their other
uses. Repository tokens expire after 180 days; issue a replacement under a new
script name before expiry and revoke the old token after the caller switches.
Repository tokens are rechecked against the token store on every request,
including when authentication caching is configured. Expiry and revocation do
not wait for the authentication cache to expire.

## Verify the deployment

Use representative user and script-token credentials to confirm that an allowed
repository returns commit data and another repository returns HTTP 403. Exercise
comparison, pagination and hotfix views. Confirm that an ordinary repository
reader cannot create, list or revoke tokens; a designated token administrator
can provision a token, and metadata responses omit its bearer secret. Verify a
malformed repository, such as `victim/x/../../other`, returns HTTP 400.
