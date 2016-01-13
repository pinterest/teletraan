import requests
import logging

from deployd.types.ping_response import PingResponse
from deployd.common.decorators import singleton

log = logging.getLogger(__name__)


@singleton
class RestfulClient(object):
    def __init__(self, config):
        self.config = config
        self.url_prefix = config.get_restful_service_url()
        self.url_version = config.get_restful_service_version()
        self.token = config.get_restful_service_token()
        self.default_timeout = 30

    def __call(self, method):
        def api(path, params=None, data=None):
            url = '%s/%s%s' % (self.url_prefix, self.url_version, path)
            if self.token:
                headers = {'Authorization': 'token %s' % self.token, 'Content-type': 'application/json'}
            else:
                headers = {'Content-type': 'application/json'}
            response = getattr(requests, method)(url, headers=headers, params=params, json=data,
                                                 timeout=self.default_timeout, verify=False)

            if response.status_code > 300:
                log.error("Teletraan failed to call backend server. "
                          "Hint: %s, %s" % (response.status_code, response.content))

            if (response.content):
                return response.json()

        return api

    def _ping_internal(self, ping_request):
        return self.__call('post')("/system/ping", data=ping_request)

    def ping(self, ping_request):
        # python object -> json
        response = self._ping_internal(ping_request.to_json())

        # json -> python object
        ping_response = PingResponse(jsonValue=response)
        return ping_response
