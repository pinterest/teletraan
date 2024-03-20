from deploy_board.settings import AWS_PRIMARY_ACCOUNT, AWS_SUB_ACCOUNT
from .helpers import clusters_helper, accounts_helper, deploys_helper
from . import agent_report

def create_legacy_ui_account(account_id):
    if account_id == AWS_PRIMARY_ACCOUNT:
        return {
            "name": f"{AWS_PRIMARY_ACCOUNT} / Primary AWS account",
            "ownerId": AWS_PRIMARY_ACCOUNT,
        }
    if account_id == AWS_SUB_ACCOUNT:
        return {
            "name": f"{AWS_SUB_ACCOUNT} / Moka account",
            "ownerId": AWS_SUB_ACCOUNT,
        }
    return {
        "name": f"{account_id} / Sub AWS account",
        "ownerId": account_id,
    }


def get_accounts(report):
    accounts = set()
    for agentStat in report.agentStats:
        if "accountId" in agentStat.agent and is_valid_account_id(agentStat.agent["accountId"]):
            accounts.add(agentStat.agent["accountId"])
    return accounts



def is_valid_account_id(account_id):
    return account_id is not None and account_id != "" and account_id != "null"


def get_accounts_from_deploy(request, env, deploy, build_with_tag):
    account = None
    deploy_accounts = []
    if env and env.get("clusterName") is not None:
        cluster = clusters_helper.get_cluster(request, env["clusterName"])
        provider, cell, id = cluster["provider"], cluster["cellName"], cluster.get("accountId", None)
        if not id:
            account = accounts_helper.get_default_account(request, cell, provider=provider)
        else:
            account = accounts_helper.get_by_cell_and_id(request, cell, id, provider)

    if account is None and env and deploy and build_with_tag:
        # terraform deploy, get information from deploy report
        progress = deploys_helper.update_progress(request, env["envName"], env["stageName"])
        report = agent_report.gen_report(request, env, progress, deploy=deploy, build_info=build_with_tag)
        deploy_accounts = [create_legacy_ui_account(account) for account in get_accounts(report)]
        deploy_accounts = [{"legacy_name": account["name"]} for account in deploy_accounts]
    elif account:
        deploy_accounts = [account]
    return deploy_accounts
