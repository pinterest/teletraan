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
import logging
import requests
from .decorators import retry
from deploy_board.settings import UNAUTHORIZED_ERROR_TEXT
from .exceptions import NotAuthorizedException, TeletraanException, FailedAuthenticationException, IllegalArgumentException
requests.packages.urllib3.disable_warnings()

DEFAULT_TIMEOUT = 30

log = logging.getLogger(__name__)


class BaseClient(object):
    def __init__(self, url_prefix, version, proxy_http=None, proxy_https=None, bearer=False):
        self.url_prefix = url_prefix
        self.url_version = version
        self.proxies = dict()
        self.bearer = bearer
        if proxy_http:
            self.proxies['http'] = proxy_http
        if proxy_https:
            self.proxies['https'] = proxy_https

    def __call(self, method):
        @retry(requests.RequestException, tries=1, delay=1, backoff=1)
        def api(path, token=None, params=None, data=None):
            url = '%s/%s%s' % (self.url_prefix, self.url_version, path)
            headers = {'Content-type': 'application/json'}

            if token:
                headers['Authorization'] = 'bearer %s' % token if self.bearer else 'token %s' % token

            response = getattr(requests, method)(url, proxies=self.proxies, headers=headers, params=params, json=data,
                                                 timeout=DEFAULT_TIMEOUT, verify=False)

            if response.status_code >= 400 and response.status_code < 600:
                try:
                    if "access_token=" in response.text:
                        bad_text = response.text.split("access_token=")[1].split('"')[0].replace("\\", "")
                        response.text = response.text.replace(bad_text, "ACCESS_TOKEN")
                except Exception:
                    pass

            if response.status_code == 401:
                raise FailedAuthenticationException(
                    f"Oops! Teletraan was unable to authenticate you. Please re-login. Server message: {response.json()['message']}")

            if response.status_code == 403:
                raise NotAuthorizedException(f"{UNAUTHORIZED_ERROR_TEXT}. Server message: {response.json()['message']}")

            if response.status_code == 400 or response.status_code == 422:
                raise IllegalArgumentException(response.text)

            if response.status_code == 404:
                log.info("Resource %s Not found" % path)
                return None

            if 400 <= response.status_code < 600:
                log.error("Backend return error %s" % response.text)
                raise TeletraanException(
                    "Teletraan failed to call backend server."
                    "Hint: %s, %s" % (response.status_code, response.text))

            if response.text:
                return response.json()

            return None

        return api

    def get(self, path, token=None, params=None, data=None):
        return self.__call('get')(path, token, params=params, data=data)

    def post(self, path, token=None, params=None, data=None):
        return self.__call('post')(path, token, params=params, data=data)

    def put(self, path, token=None, params=None, data=None):
        return self.__call('put')(path, token, params=params, data=data)

    def delete(self, path, token=None, params=None, data=None):
        return self.__call('delete')(path, token, params=params, data=data)

    def gen_params(self, kwargs):
        params = {}
        for key, value in kwargs.items():
            if value:
                params[key] = value
        return params
