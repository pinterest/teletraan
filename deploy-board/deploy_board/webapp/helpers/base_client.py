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
from decorators import retry

from exceptions import NotAuthorizedException, TeletraanException, FailedAuthenticationException
requests.packages.urllib3.disable_warnings()

DEFAULT_TIMEOUT = 30

log = logging.getLogger(__name__)


class BaseClient(object):
    def __init__(self, url_prefix, version):
        self.url_prefix = url_prefix
        self.url_version = version

    def __call(self, method):
        @retry(requests.RequestException, tries=1, delay=1, backoff=1)
        def api(path, token=None, params=None, data=None):
            url = '%s/%s%s' % (self.url_prefix, self.url_version, path)
            headers = {'Content-type': 'application/json'}
            if token:
                headers['Authorization'] = 'token %s' % token

            response = getattr(requests, method)(url, headers=headers, params=params, json=data,
                                                 timeout=DEFAULT_TIMEOUT, verify=False)

            if response.status_code >= 400 and response.status_code < 600:
                try:
                    if "access_token=" in response.content:
                        bad_text = response.content.split("access_token=")[1].split('"')[0].replace("\\", "")
                        response.content = response.content.replace(bad_text, "ACCESS_TOKEN")
                except:
                    pass

            if response.status_code == 401:
                raise FailedAuthenticationException(
                    "Oops! Teletraan was unable to authenticate you. Contact an environment ADMIN for "
                    "assistance. " + response.content)

            if response.status_code == 403:
                raise NotAuthorizedException(
                    "Oops! You do not have the required permissions for this action. Contact an environment ADMIN for "
                    "assistance. " + response.content)

            if response.status_code == 404:
                log.info("Resource %s Not found" % path)
                return None

            if 400 <= response.status_code < 600:
                log.error("Backend return error %s" % response.content)
                raise TeletraanException(
                    "Teletraan failed to call backend server."
                    "Hint: %s, %s" % (response.status_code, response.content))

            if response.content:
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
        for key, value in kwargs.iteritems():
            if value:
                params[key] = value
        return params
