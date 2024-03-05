from deploy_board.webapp.helpers.rodimus_client import RodimusClient
import logging

log = logging.getLogger(__name__)

rodimus_client = RodimusClient()


def get_all_accounts(request):
    try:
        return rodimus_client.get("/accounts", request.teletraan_user_id.token, {"provider": "AWS"})
    except Exception as e:
        log.error(f"Can't get all accounts: error = {e}")
        # return None for backward-compatibility
        return None
