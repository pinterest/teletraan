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

from deployd.client.restfulclient import RestfulClient
from deployd.common.decorators import retry
from deployd.common.stats import create_stats_timer, create_sc_increment
from deployd.types.ping_request import PingRequest

log = logging.getLogger(__name__)


class Client(object):
    def __init__(self, config=None, hostname=None,
                 ip=None, hostgroup=None, host_id=None):
        self._hostname = hostname
        self._ip = ip
        self._hostgroup = hostgroup
        self._id = host_id
        self._config = config

    def _read_host_info(self):
        host_info_fn = self._config.get_host_info_fn()
        lock_fn = '{}.lock'.format(host_info_fn)
        lock = lockfile.FileLock(lock_fn)
        if os.path.exists(host_info_fn):
            with lock, open(host_info_fn, "r+") as f:
                host_info = dict((n.strip('\"\n\' ') for n in line.split("=", 1)) for line in f)

            if "hostname" in host_info:
                self._hostname = host_info.get("hostname")

            if "ip" in host_info:
                self._ip = host_info.get("ip")

            if "id" in host_info:
                self._id = host_info.get("id")

            host_group = host_info.get("groups", None)
            if host_group:
                self._hostgroup = host_group.split(",")
        else:
            log.warn("Cannot find host information file {}. See doc for more details".format(host_info_fn))

        if not self._hostname:
            self._hostname = socket.gethostname()

        if not self._id:
            self._id = self._hostname

        if not self._ip:
            try:
                self._ip = socket.gethostbyname(self._hostname)
            except Exception:
                log.warn('Host ip information does not exist.')
                pass

        log.info("Host information is loaded. "
                 "Host name: {}, IP: {}, host id: {}, group: {}".format(self._hostname, self._ip,
                                                                        self._id,  self._hostgroup))

    def send_reports(self, env_reports=None):
        try:
            self._read_host_info()
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
                                       groups=self._hostgroup, reports=reports)

            with create_stats_timer('deploy.agent.request.latency',
                                    sample_rate=1.0,
                                    tags={'host': self._hostname}):
                ping_response = self.send_reports_internal(ping_request)

            log.debug('%s -> %s' % (ping_request, ping_response))
            return ping_response
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
