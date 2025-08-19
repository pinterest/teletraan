"""Helper class to connect Nimbus service"""

import logging
from .decorators import singleton
from deploy_board.settings import (
    IS_PINTEREST,
    NIMBUS_SERVICE_URL,
    NIMBUS_SERVICE_VERSION,
    NIMBUS_USE_EGRESS,
    NIMBUS_EGRESS_URL,
    TELETRAAN_PROJECT_URL_FORMAT,
)
from .exceptions import TeletraanException
from urllib.parse import urlparse
import requests

requests.packages.urllib3.disable_warnings()

log = logging.getLogger(__name__)


@singleton
class NimbusClient(object):
    def handle_response(self, response):
        if response.status_code == 404:
            log.error("Resource not found. Nimbus API response - %s" % response.content)
            return None

        if response.status_code == 409:
            log.error(
                "Resource already exists. Nimbus API response - %s" % response.content
            )
            raise TeletraanException(
                "Resource conflict - Nimbus already has an Identifier for your proposed new stage. "
            )

        if 400 <= response.status_code < 600:
            log.error(
                "Nimbus API Error %s, %s" % (response.content, response.status_code)
            )
            raise TeletraanException(
                "Teletraan failed to successfully call Nimbus. Contact your friendly Teletraan owners for assistance. Hint: %s, %s"
                % (response.status_code, response.content)
            )

        if response.status_code == 200 or response.status_code == 201:
            return response.json()

        return None

    def get_one_identifier(self, name, token=None):  # ava 0
        service_url = NIMBUS_EGRESS_URL if NIMBUS_USE_EGRESS else NIMBUS_SERVICE_URL

        headers = {}
        headers["Client-Authorization"] = "client Teletraan"
        if token:
            headers["Authorization"] = "token %s" % token

        if NIMBUS_USE_EGRESS:
            parsed_uri = urlparse(NIMBUS_SERVICE_URL)
            headers["Host"] = parsed_uri.netloc

        response = requests.get(
            "{}/api/{}/identifiers/{}".format(
                service_url, NIMBUS_SERVICE_VERSION, name
            ),
            headers=headers,
        )
        return self.handle_response(response)

    def create_one_identifier(self, data, token=None):
        """
        Create a Nimbus Identifier according to the input request data.
        If the request data does not have all the information needed for creating a Nimbus identifier, this method will raise a Teletraan Exception.
        """
        requiredParams = ["projectName", "env_name", "stage_name"]  # ava 1
        for param in requiredParams:
            if data.get(param) is None or len(data.get(param)) == 0:
                log.error(
                    "Missing %s in the request data, cannot create a Nimbus identifier"
                    % param
                )
                exceptionMessage = (
                    "Teletraan cannot create a Nimbus identifier because %s is missing."
                    % param
                )
                if IS_PINTEREST:
                    exceptionMessage += " Contact #teletraan for assistance."
                raise TeletraanException(exceptionMessage)

        headers = {}
        headers["Client-Authorization"] = "client Teletraan"
        if token:
            headers["Authorization"] = "token %s" % token

        payload = {}
        payload["kind"] = "Identifier"
        payload["apiVersion"] = "v1"
        payload["platformName"] = "teletraan"
        payload["projectName"] = data.get("projectName")

        cellName = None
        for property in data["propertyList"]["properties"]:
            if property["propertyName"] == "cellName":
                cellName = property["propertyValue"]
        if cellName is None:
            log.error(
                "Missing cellName in the request data, cannot create a Nimbus identifier"
            )
            exceptionMessage = "Teletraan cannot create a Nimbus identifier because cellName is missing in this env's existing identifier."
            if IS_PINTEREST:
                exceptionMessage += " Contact #teletraan for assistance."
            raise TeletraanException(exceptionMessage)

        payload["spec"] = {
            "kind": "EnvironmentSpec",
            "apiVersion": "v1",
            "cellName": cellName,
            "envName": data.get("env_name"),
            "stageName": data.get("stage_name"),
        }

        service_url = NIMBUS_EGRESS_URL if NIMBUS_USE_EGRESS else NIMBUS_SERVICE_URL
        if NIMBUS_USE_EGRESS:
            parsed_uri = urlparse(NIMBUS_SERVICE_URL)
            headers["Host"] = parsed_uri.netloc

        response = requests.post(
            "{}/api/{}/identifiers".format(service_url, NIMBUS_SERVICE_VERSION),
            json=payload,
            headers=headers,
        )

        return self.handle_response(response)

    def delete_one_identifier(self, name, token=None):
        headers = {}
        headers["Client-Authorization"] = "client Teletraan"
        if token:
            headers["Authorization"] = "token %s" % token

        service_url = NIMBUS_EGRESS_URL if NIMBUS_USE_EGRESS else NIMBUS_SERVICE_URL
        if NIMBUS_USE_EGRESS:
            parsed_uri = urlparse(NIMBUS_SERVICE_URL)
            headers["Host"] = parsed_uri.netloc

        response = requests.delete(
            "{}/api/{}/identifiers/{}".format(
                service_url, NIMBUS_SERVICE_VERSION, name
            ),
            headers=headers,
        )
        return self.handle_response(response)

    def get_one_project_console_url(self, project_name):
        if not TELETRAAN_PROJECT_URL_FORMAT:
            return ""
        return TELETRAAN_PROJECT_URL_FORMAT.format(projectName=project_name)
