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
"""Helper class to connect Nimbus service
"""
import logging
from decorators import singleton
from deploy_board.settings import NIMBUS_SERVICE_URL, NIMBUS_SERVICE_VERSION
from exceptions import NotAuthorizedException, TeletraanException, FailedAuthenticationException
import requests

log = logging.getLogger(__name__)

@singleton
class NimbusClient(object):
    def handle_response(self, response):
        if response.status_code == 404:
            log.error("Resource not found. Nimbus API response - %s" % response.content)
            return None

        if response.status_code == 409:
            log.error("Resource already exists. Nimbus API response - %s" % response.content)
            raise TeletraanException('Resource conflict - Nimbus already has an Identifier for your proposed new stage. ')
            # return None

        if 400 <= response.status_code < 600:
            log.error("Nimbus API Error %s, %s" % (response.content, response.status_code))
            raise TeletraanException(
                "Teletraan failed to successfully call Nimbus. Contact your friendly Teletraan owners for assistance. Hint: %s, %s" % (response.status_code, response.content)
            )

        if response.status_code == 200 or response.status_code == 201:
            return response.json()

        return None

    def get_one_identifier(self, name):
        response = requests.get('{}/api/{}/identifiers/{}'.format(NIMBUS_SERVICE_URL, NIMBUS_SERVICE_VERSION, name))
        return self.handle_response(response)

    def create_one_identifier(self, data):
        payload = {}
        payload['kind'] = 'Identifier'
        payload['apiVersion'] = 'v1'
        payload['platformName'] = 'teletraan'
        payload['projectName'] = data.get('projectName')

        for property in data['propertyList']['properties']:
            if property['propertyName'] == 'cellName':
                cellName = property['propertyValue']

        payload['spec'] = {
            'kind': 'EnvironmentSpec',
            'apiVersion': 'v1',
            'cellName': cellName,
            'envName': data.get('env_name'),
            'stageName': data.get('stage_name')
        }

        response = requests.post('{}/api/{}/identifiers'.format(NIMBUS_SERVICE_URL, NIMBUS_SERVICE_VERSION), json=payload)

        return self.handle_response(response)
















# # former 

# import logging
# from decorators import singleton
# from deploy_board.settings import RODIMUS_SERVICE_URL, RODIMUS_SERVICE_VERSION
# from base_client import BaseClient


# log = logging.getLogger(__name__)


# @singleton
# class RodimusClient(BaseClient):
#     def __init__(self):
#         BaseClient.__init__(self, url_prefix=RODIMUS_SERVICE_URL, version=RODIMUS_SERVICE_VERSION)
