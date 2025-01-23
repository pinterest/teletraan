# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from typing import Callable, Optional
import requests
import logging

from deployd.types.ping_response import PingResponse
from deployd.common.decorators import singleton
from deployd.common.exceptions import AgentException
from deployd.common.stats import create_sc_increment

requests.packages.urllib3.disable_warnings()

log = logging.getLogger(__name__)


@singleton
class RestfulClient(object):
    def __init__(self, config) -> None:
        self.config = config
        self.url_prefix = config.get_restful_service_url()
        self.url_version = config.get_restful_service_version()
        self.token = config.get_restful_service_token()
        self.verify = config.get_verify_https_certificate() == "True"
        self.default_timeout = 30

    @staticmethod
    def sc_fail(reason) -> None:
        """send RestfulClient failure metrics"""
        create_sc_increment(name="deploy.agent.rest.failure", tags={"reason": reason})

    def __call(self, method) -> Callable:
        def api(path, params=None, data=None) -> Optional[dict]:
            url = "%s/%s%s" % (self.url_prefix, self.url_version, path)
            if self.token:
                headers = {
                    "Authorization": "token %s" % self.token,
                    "Content-type": "application/json",
                }
            else:
                headers = {"Content-type": "application/json"}
            try:
                response = getattr(requests, method)(
                    url,
                    headers=headers,
                    params=params,
                    json=data,
                    timeout=self.default_timeout,
                    verify=self.verify,
                )
            except Exception as exception:
                ex = type(exception).__name__
                self.sc_fail(ex)
                raise

            create_sc_increment(
                name="deploy.agent.rest.status",
                tags={"status_code": response.status_code},
            )

            if response.status_code > 300:
                msg = "Teletraan failed to call backend server. Hint: %s, %s" % (
                    response.status_code,
                    response.content,
                )
                log.error(msg)
                raise AgentException(msg)

            if response.content:
                return response.json()
            else:
                self.sc_fail("response_content_empty")

        return api

    def _ping_internal(self, ping_request) -> Optional[dict]:
        return self.__call("post")("/system/ping", data=ping_request)

    def ping(self, ping_request) -> PingResponse:
        # python object -> json
        response = self._ping_internal(ping_request.to_json())

        # json -> python object
        ping_response = PingResponse(jsonValue=response)
        return ping_response
