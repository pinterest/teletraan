import logging

from deploy_board.settings import DEFAULT_CELL, DEFAULT_PROVIDER
from deploy_board.webapp.helpers.rodimus_client import RodimusClient
from . import clusters_helper

log = logging.getLogger(__name__)

rodimus_client = RodimusClient()


def get_all_accounts(request):
    try:
        return rodimus_client.get(
            "/accounts", request.teletraan_user_id.token, {"provider": "AWS"}
        )
    except Exception as e:
        log.error(f"Can't get all accounts: error = {e}")
        # return None for backward-compatibility
        return None


def get_by_cell_and_id(request, cell, account_id, provider="AWS"):
    try:
        return rodimus_client.get(
            f"/accounts/{provider}/{cell}/{account_id}", request.teletraan_user_id.token
        )
    except Exception as e:
        log.error(
            f"Can't get account by cell and account_id: "
            f"provder = {provider}, cell = {cell}, account_id = {account_id}, error = {e}"
        )
        # return None for backward-compatibility
        return None


def get_default_account(request, cell, provider="AWS"):
    return get_by_cell_and_id(request, cell, "default", provider)


def get_aws_owner_id_for_cluster_name(request, cluster_name):
    cluster = clusters_helper.get_cluster(request, cluster_name)
    return get_aws_owner_id_for_cluster(request, cluster)


def get_aws_owner_id_for_cluster(request, cluster):
    account_id = cluster.get("accountId")
    cell = cluster.get("cellName", DEFAULT_CELL)
    provider = cluster.get("provider", DEFAULT_PROVIDER)
    if not account_id:
        account = get_default_account(request, cell, provider)
    else:
        account = get_by_cell_and_id(request, cell, account_id, provider)
    return account["data"]["ownerId"]
