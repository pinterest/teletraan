# Roll out tag and PinDeploy authorization

This guide is for operators deploying the tag and PinDeploy authorization fix. It
covers the required policy rollout, dashboard behavior, and canary checks. See the
[permission reference](README.md#tag-and-pindeploy-mutations) for endpoint scopes.

## Prepare the authorization policy

1. Publish the companion authorization-policy change before deploying the server.
   For Pastis, define a `pinDeployManager` role whose only action is
   `MANAGE_PINDEPLOY`, and grant it on `SYSTEM:*` to the intended PinDeploy service
   identities. The system-to-environment inheritance permits the same action on
   `ENV_STAGE`, so both mapping endpoints work. This action is accepted only by
   the PinDeploy enable/disable endpoints.
2. In deployments that check cAuthZ before the fallback ACL, map the new action to
   the existing `admin` permission so existing resource administrators retain
   access. The PinDeploy service can receive its narrow grant through the fallback
   ACL; it does not need general system `admin`, `write`, or `delete` permission.
3. Run the production policy tests with cAuthZ decisions controlled separately
   from fallback ACL decisions. Cover the cases below, including the identities
   used by both current and legacy PinDeploy deployments. Run the normal policy
   build and tests in the policy repository before publishing it.
4. Audit build-tag automation. Build publication permission does not authorize
   changing deploy-gating tags. Assign intended automation `WRITE`/`DELETE` on its
   build targets, and check that environment-scoped tokens cannot mutate unrelated
   builds. Do not broaden environment owners to global build administrators.
   Audit build-deletion scripts too: `DELETE /v1/builds/{id}` uses the same build
   scope check and no longer accepts an unrelated environment token.

| Principal | Request | Expected result from the fallback policy |
| --- | --- | --- |
| Environment owner with no build grant | BUILD `WRITE`/`DELETE` | Deny |
| Build administrator | BUILD `WRITE`/`DELETE` | Allow |
| PinDeploy service with only `pinDeployManager` | SYSTEM or ENV_STAGE `MANAGE_PINDEPLOY` | Allow |
| Same PinDeploy service | SYSTEM `DELETE`, ENV_STAGE `WRITE`, or BUILD `WRITE`/`DELETE` | Deny |
| Unrelated service or reader | `MANAGE_PINDEPLOY` | Deny |

These expectations are about the fallback policy. Additional live cAuthZ grants
can change the effective result. Record and verify those grants separately; a
local policy test does not establish the service's live permissions.

## Deploy and verify

1. Deploy the dashboard change before or together with the server change. It
   requests rollback first and catches authorization denial when marking the
   previous build bad. The redirect must display a warning when the tag was not
   created. A failed rollback must not create a `BAD_BUILD` tag.
2. Announce the build-tag permission change to environment owners and automation
   owners. Include the standalone build-page GOOD/BAD/CERTIFIED buttons: these now
   require build `WRITE`, and users without it will receive an authorization error.
   Environment ownership alone does not grant this access. Rollback can complete
   without tagging, but the warning means that the build remains eligible for
   automatic deployment; follow the incident's normal build-blocking procedure.
3. Canary the server after the policy has propagated. Through the normal PinDeploy
   service authentication path, create/update a test application and verify that
   its Teletraan stages become PinDeploy-managed. Delete that test application and
   verify its pipeline mappings disappear. Use disposable stages and builds.
4. Exercise rollback as an environment owner without build-tag permission. Verify
   both the rollback and the warning. Check that an authorized build administrator
   can still tag, and that read-only and unrelated scoped credentials receive 403
   on tag mutations.
5. Compare `pindeploy.teletraan.pipeline_write.failed` and Teletraan 403 rates on
   `/v1/tags` and `/v1/pindeploy/*` with the pre-canary baseline. PinDeploy records
   mapping failures without failing its surrounding application workflow, so a
   successful application update is not sufficient evidence that mappings changed.

## Troubleshoot denied requests

If PinDeploy receives 403, stop the rollout and check the authenticated service
identity, policy version, action mapping, and resource scope. Avoid granting
general SYSTEM `DELETE` to resolve this: the inherited system policy can authorize
deletion of unrelated resources. Keep the endpoint protections enabled while
correcting the narrow policy grant.

If a rollback succeeds with a tagging warning, verify the build's status before
allowing automatic promotion. A build permission denial should not be retried as
another rollback. Use an authorized build operator to apply the tag if needed.

The HTTP tests in this repository exercise endpoint role alternatives and scope
resolution using a policy decision double. They do not call live Pastis/cAuthZ.
Production policy publication and canary verification remain required release
steps.
