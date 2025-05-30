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

from typing import Optional
import lockfile
import logging
import os
import socket
import traceback
import json
from pathlib import Path
import re
import subprocess

from deployd import __version__
from deployd.client.base_client import BaseClient
from deployd.client.restfulclient import RestfulClient
from deployd.common.decorators import retry
from deployd.common.stats import create_stats_timer, create_sc_increment
from deployd.common import utils
from deployd.types.ping_request import PingRequest
from deployd import IS_PINTEREST
from deployd.types.ping_response import PingResponse


log = logging.getLogger(__name__)

NORMANDIE_CERT_FILEPATH = "/var/lib/normandie/fuse/cert/generic"
SAN_URI_PATTERN = r"URI:(\S+),?"
STATUSERRNO_PATTERN = r"StatusErrno=(\d+)"
ACTIVESTATE_PATTERN = r"ActiveState=(\S+)"
SUBSTATE_PATTERN = r"SubState=(\S+)"


class Client(BaseClient):
    def __init__(
        self,
        config=None,
        hostname=None,
        ip=None,
        hostgroup=None,
        host_id=None,
        use_facter=None,
        use_host_info=False,
    ) -> None:
        self._hostname = hostname
        self._ip = ip
        self._hostgroup = hostgroup
        self._id = host_id
        self._config = config
        self._use_facter = use_facter
        self._use_host_info = use_host_info
        self._autoscaling_group = None
        self._availability_zone = None
        self._stage_type = None
        # stage_type doesn't always exist, and if it doesn't we don't want to
        # keep trying to fetch it from facter every time
        self._stage_type_fetched = False
        self._account_id = None
        self._normandie_status = None
        self._knox_status = None

    def _read_host_info(self) -> bool:
        if self._use_facter:
            log.info("Use facter to get host info")
            name_key = self._config.get_facter_name_key()
            ip_key = self._config.get_facter_ip_key()
            id_key = self._config.get_facter_id_key()
            group_key = self._config.get_facter_group_key()
            keys_to_fetch = set()
            # facter call is expensive so collect all keys to fetch first
            if not self._hostname and name_key:
                keys_to_fetch.add(name_key)

            if not self._ip and ip_key:
                keys_to_fetch.add(ip_key)

            if not self._id and id_key:
                keys_to_fetch.add(id_key)

            if not self._hostgroup and group_key:
                keys_to_fetch.add(group_key)

            if keys_to_fetch:
                facter_data = utils.get_info_from_facter(keys_to_fetch)

            if not self._hostname:
                self._hostname = facter_data.get(name_key, None)

            if not self._ip:
                self._ip = facter_data.get(ip_key, None)

            if not self._id:
                self._id = facter_data.get(id_key, None)

            if not self._hostgroup and group_key in facter_data:
                hostgroup = facter_data[group_key]
                if hostgroup is not None:
                    self._hostgroup = hostgroup.split(",")
        else:
            # read host_info file
            host_info_fn = self._config.get_host_info_fn()
            lock_fn = "{}.lock".format(host_info_fn)
            lock = lockfile.FileLock(lock_fn)
            if os.path.exists(host_info_fn):
                with lock, open(host_info_fn, "r+") as f:
                    host_info = dict(
                        (n.strip("\"\n' ") for n in line.split("=", 1)) for line in f
                    )

                if not self._hostname and "hostname" in host_info:
                    self._hostname = host_info.get("hostname")

                if not self._ip and "ip" in host_info:
                    self._ip = host_info.get("ip")

                if not self._id and "id" in host_info:
                    self._id = host_info.get("id")

                if not self._hostgroup:
                    host_group = host_info.get("groups", None)
                    if host_group:
                        self._hostgroup = host_group.split(",")

                # Hosts brought up outside of ASG or Teletraan might not have ASG
                if not self._autoscaling_group:
                    self._autoscaling_group = host_info.get("autoscaling-group", None)

                if not self._availability_zone:
                    self._availability_zone = host_info.get("availability-zone", None)

                if not self._stage_type:
                    self._stage_type = host_info.get("stage-type", None)
            else:
                log.warn(
                    "Cannot find host information file {}. See doc for more details".format(
                        host_info_fn
                    )
                )

        # patch missing part
        if not self._hostname:
            self._hostname = socket.gethostname()

        if not self._id:
            if self._use_facter:
                # Must fail here as it cannot identify the host if id is missing
                return False
            else:
                self._id = self._hostname

        if not self._ip:
            try:
                self._ip = socket.gethostbyname(self._hostname)
            except Exception:
                log.warn("Host ip information does not exist.")
                pass

        if IS_PINTEREST and self._use_host_info is False:
            # Read new keys from facter always
            az_key = self._config.get_facter_az_key()
            secondary_az_key = self._config.get_facter_secondary_az_key()
            asg_tag_key = self._config.get_facter_asg_tag_key()
            ec2_tags_key = self._config.get_facter_ec2_tags_key()
            stage_type_key = self._config.get_stage_type_key()
            account_id_key = self._config.get_facter_account_id_key()
            keys_to_fetch = set()

            if not self._availability_zone:
                if az_key:
                    keys_to_fetch.add(az_key)
                if secondary_az_key:
                    keys_to_fetch.add(secondary_az_key)

            if not self._autoscaling_group:
                keys_to_fetch.add(ec2_tags_key)

            if not self._stage_type and not self._stage_type_fetched:
                keys_to_fetch.add(stage_type_key)

            if not self._account_id:
                keys_to_fetch.add(account_id_key)

            if keys_to_fetch:
                facter_data = utils.get_info_from_facter(keys_to_fetch)

            if not self._availability_zone:
                self._availability_zone = facter_data.get(az_key, None)

            if not self._availability_zone:
                self._availability_zone = facter_data.get(secondary_az_key, None)

            # Hosts brought up outside of ASG or Teletraan might not have ASG
            # Note: on U14, facter -p ec2_tags.Autoscaling does not work.
            # so need to read ec2_tags from facter and parse Autoscaling tag to cover this case
            if not self._autoscaling_group:
                ec2_tags = facter_data.get(ec2_tags_key)
                if ec2_tags:
                    ec2_tags["availability_zone"] = self._availability_zone
                self._ec2_tags = json.dumps(ec2_tags) if ec2_tags else None
                self._autoscaling_group = (
                    ec2_tags.get(asg_tag_key) if ec2_tags else None
                )

            if not self._stage_type and not self._stage_type_fetched:
                self._stage_type = facter_data.get(stage_type_key, None)
                self._stage_type_fetched = True

            if not self._account_id:
                ec2_metadata = facter_data.get(account_id_key, None)
                if ec2_metadata:
                    info = json.loads(ec2_metadata)
                    self._account_id = info.get("AccountId", None)

        # Retrieve Normandie Status, swallowing exceptions if any: Ping should always be sent.
        try:
            self._normandie_status = self.get_normandie_status()
        except Exception as e:
            log.exception(f"Failed to get normandie status: {e}")
            self._normandie_status = "ERROR"

        # Retrieve Knox Status, swallowing exceptions if any: Ping should always be sent.
        try:
            self._knox_status = self.get_knox_status()
        except Exception as e:
            log.exception(f"Failed to get knox status: {e}")
            self._knox_status = "ERROR"

        log.info(
            "Host information is loaded. "
            "Host name: {}, IP: {}, host id: {}, agent_version={}, autoscaling_group: {}, "
            "availability_zone: {}, ec2_tags: {}, stage_type: {}, group: {}, account id: {},"
            "normandie_status: {}, knox_status: {}".format(
                self._hostname,
                self._ip,
                self._id,
                __version__,
                self._autoscaling_group,
                self._availability_zone,
                self._ec2_tags,
                self._stage_type,
                self._hostgroup,
                self._account_id,
                self._normandie_status,
                self._knox_status,
            )
        )

        if not self._availability_zone:
            log.error("Fail to read host info: availablity zone")
            create_sc_increment(
                name="deploy.failed.agent.hostinfocollection",
                tags={"info": "availability_zone"},
            )
            return False

        return True

    def get_normandie_status(self) -> Optional[str]:
        path = Path(NORMANDIE_CERT_FILEPATH)
        cmd = [
            "openssl",
            "x509",
            "-in",
            path.as_posix(),
            "-noout",
            "-text",
            "-certopt",
            "no_subject,no_header,no_version,no_serial,no_signame,no_validity,no_issuer,no_pubkey,no_sigdump,no_aux",
        ]
        try:
            cert = subprocess.check_output(cmd).decode("utf-8")
        except subprocess.CalledProcessError as e:
            log.exception(f"failed to get spiffe id from normandie: {e}")
            return "ERROR"

        matcher = re.search(SAN_URI_PATTERN, cert)
        if matcher is None:
            return "ERROR"
        spiff_id = matcher.group(1)

        if spiff_id:
            return "OK"
        else:
            return "ERROR"

    def get_knox_status(self) -> Optional[str]:
        cmd = [
            "systemctl",
            "show",
            "knox",
            "--property=Result",
            "--property=StatusErrno",
            "--property=ActiveState",
            "--property=SubState",
        ]
        try:
            status = subprocess.check_output(cmd).decode("utf-8")
        except subprocess.CalledProcessError as e:
            log.exception(f"failed to get knox service status from systemctl: {e}")
            return "ERROR"

        # Use three different matchers and pattern to not make assumptions on the order of the properties
        matcher = re.search(STATUSERRNO_PATTERN, status)
        if matcher is None:
            return "ERROR"
        statusErrNo = matcher.group(1)

        matcher = re.search(ACTIVESTATE_PATTERN, status)
        if matcher is None:
            return "ERROR"
        activeState = matcher.group(1)

        matcher = re.search(SUBSTATE_PATTERN, status)
        if matcher is None:
            return "ERROR"
        subState = matcher.group(1)

        if statusErrNo == "0" and activeState == "active" and subState == "running":
            return "OK"
        else:
            return "ERROR"

    def send_reports(self, env_reports=None) -> Optional[PingResponse]:
        try:
            if self._read_host_info():
                reports = [status.report for status in env_reports.values()]
                for report in reports:
                    if isinstance(report.errorMessage, bytes):
                        report.errorMessage = report.errorMessage.decode("utf-8")

                    # We ignore non-ascii charater for now, we should further solve this problem on
                    # the server side:
                    # https://app.asana.com/0/11815463290546/40714916594784
                    if report.errorMessage:
                        report.errorMessage = report.errorMessage.encode(
                            "ascii", "ignore"
                        ).decode()
                ping_request = PingRequest(
                    hostId=self._id,
                    hostName=self._hostname,
                    hostIp=self._ip,
                    groups=self._hostgroup,
                    reports=reports,
                    agentVersion=__version__,
                    autoscalingGroup=self._autoscaling_group,
                    availabilityZone=self._availability_zone,
                    ec2Tags=self._ec2_tags,
                    stageType=self._stage_type,
                    accountId=self._account_id,
                    normandieStatus=self._normandie_status,
                    knoxStatus=self._knox_status,
                )

                with create_stats_timer("deploy.agent.request.latency"):
                    ping_response = self.send_reports_internal(ping_request)

                log.debug("%s -> %s" % (ping_request, ping_response))
                return ping_response
            else:
                log.error("Fail to read host info")
                create_sc_increment(name="deploy.failed.agent.hostinfocollection")
        except Exception:
            log.error(traceback.format_exc())
            create_sc_increment(name="deploy.failed.agent.requests")
            return None

    @retry(ExceptionToCheck=Exception, delay=1, tries=3)
    def send_reports_internal(self, request) -> PingResponse:
        ping_service = RestfulClient(self._config)
        response = ping_service.ping(request)
        return response
