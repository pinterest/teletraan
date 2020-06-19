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

import lockfile
import logging
import os
import socket
import traceback

from deployd.client.base_client import BaseClient
from deployd.client.restfulclient import RestfulClient
from deployd.common.decorators import retry
from deployd.common.stats import create_stats_timer, create_sc_increment
from deployd.common import utils
from deployd.types.ping_request import PingRequest

log = logging.getLogger(__name__)


class Client(BaseClient):
    def __init__(self, config=None, hostname=None, ip=None, hostgroup=None, 
                host_id=None, use_facter=None, stage=None, availability_zone=None):
        self._hostname = hostname
        self._ip = ip
        self._hostgroup = hostgroup
        self._id = host_id
        self._config = config
        self._use_facter = use_facter
        self._stage = stage
        self._availability_zone = availability_zone

    def _read_host_info(self):
        if self._use_facter:
            log.info("Use facter to get host info")
            if not self._hostname and self._config.get_facter_name_key():
                self._hostname = utils.get_info_from_facter(self._config.get_facter_name_key())

            if not self._ip and self._config.get_facter_ip_key():
                self._ip = utils.get_info_from_facter(self._config.get_facter_ip_key())

            if not self._id and self._config.get_facter_id_key():
                self._id = utils.get_info_from_facter(self._config.get_facter_id_key())

            if not self._hostgroup and self._config.get_facter_group_key():
                output = utils.get_info_from_facter(self._config.get_facter_group_key())
                if output:
                    self._hostgroup = output.split(",")

            # Hosts brought up outside of ASG or Teletraan might not have stage 
            if not self._stage:
                self._stage = utils.get_info_from_facter("host_type")

            if not self._availability_zone:
                self._availability_zone = utils.get_info_from_facter("ec2_metadata.placement.availability-zone")
        else:
            # read host_info file
            host_info_fn = self._config.get_host_info_fn()
            lock_fn = '{}.lock'.format(host_info_fn)
            lock = lockfile.FileLock(lock_fn)
            if os.path.exists(host_info_fn):
                with lock, open(host_info_fn, "r+") as f:
                    host_info = dict((n.strip('\"\n\' ') for n in line.split("=", 1)) for line in f)

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

                # Hosts brought up outside of ASG or Teletraan might not have stage 
                if not self._stage and "host_type" in host_info:
                    self._stage = host_info.get("host_type")

                if not self._availability_zone and "availability-zone" in host_info:
                    self._availability_zone = host_info.get("availability-zone")
            else:
                log.warn("Cannot find host information file {}. See doc for more details".format(host_info_fn))

        # patch missing part
        if not self._hostname:
            self._hostname = socket.gethostname()

        if not self._id:
            if self._use_facter:
                #Must fail here as it cannot identify the host if id is missing
                return False
            else:
                self._id = self._hostname

        if not self._ip:
            try:
                self._ip = socket.gethostbyname(self._hostname)
            except Exception:
                log.warn('Host ip information does not exist.')
                pass

        log.info("Host information is loaded. "
                 "Host name: {}, IP: {}, host id: {}, stage: {}, availability_zone: {}, "
                 "group: {}".format(self._hostname, self._ip, self._id, self._stage, 
                 self._availability_zone, self._hostgroup))
        return True

    def send_reports(self, env_reports=None):
        try:
            if self._read_host_info():
                reports = [status.report for status in env_reports.values()]
                for report in reports:
                    if isinstance(report.errorMessage, str):
                        report.errorMessage = unicode(report.errorMessage, "utf-8")

                    # We ignore non-ascii charater for now, we should further solve this problem on
                    # the server side:
                    # https://app.asana.com/0/11815463290546/40714916594784
                    if report.errorMessage:
                        report.errorMessage = report.errorMessage.encode('ascii', 'ignore')
                ping_request = PingRequest(hostId=self._id, hostName=self._hostname, hostIp=self._ip,
                                        groups=self._hostgroup, reports=reports, stage=self._stage,
                                        availabilityZone=self._availability_zone)

                with create_stats_timer('deploy.agent.request.latency',
                                        sample_rate=1.0,
                                        tags={'host': self._hostname}):
                    ping_response = self.send_reports_internal(ping_request)

                log.debug('%s -> %s' % (ping_request, ping_response))
                return ping_response
            else:
                log.error("Fail to read host info")
                create_sc_increment(stats='deploy.failed.agent.hostinfocollection',
                                sample_rate=1.0,
                                tags={'host': self._hostname})
        except Exception:
            log.error(traceback.format_exc())
            create_sc_increment(stats='deploy.failed.agent.requests',
                                sample_rate=1.0,
                                tags={'host': self._hostname})
            return None

    @retry(ExceptionToCheck=Exception, delay=1, tries=3)
    def send_reports_internal(self, request):
        ping_service = RestfulClient(self._config)
        response = ping_service.ping(request)
        return response
