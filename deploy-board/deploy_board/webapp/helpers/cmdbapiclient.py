import logging
import requests
from .decorators import singleton
from deploy_board.settings import CMDB_API_HOST, CMDB_INSTANCE_URL

requests.packages.urllib3.disable_warnings()
log = logging.getLogger(__name__)


@singleton
class CmdbApiClient(object):
    def query(self, query, fields, account_id):
        headers = {} if not account_id else {"x-aws-account-id": account_id}
        return requests.post(
            CMDB_API_HOST + "/v2/query",
            json={"query": query, "fields": fields},
            headers=headers,
        )

    def get_host_details(self, host_id, account_id):
        headers = {} if not account_id else {"x-aws-account-id": account_id}
        return requests.get(
            CMDB_API_HOST + CMDB_INSTANCE_URL + host_id, headers=headers
        )
