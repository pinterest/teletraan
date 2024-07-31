from __future__ import absolute_import
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

from deployd.common.caller import Caller
from deployd.common.helper import Helper
from deployd.common.status_code import Status
from deployd.common.config import Config
from deployd.download.download_helper import DownloadHelper, DOWNLOAD_VALIDATE_METRICS
from deployd.common.stats import create_sc_increment
import os
import requests
import logging
from urllib.parse import ParseResult, urlparse
requests.packages.urllib3.disable_warnings()

log = logging.getLogger(__name__)


class HTTPDownloadHelper(DownloadHelper):

    def __init__(self, url=None, config=None) -> None:
        super().__init__(url)
        self._config = config if config else Config()

    def _download_files(self, local_full_fn) -> int:
        download_cmd = ['curl', '-o', local_full_fn, '-fksS', self._url]
        log.info('Running command: {}'.format(' '.join(download_cmd)))
        output, error, process_return_code = Caller.call_and_log(download_cmd, cwd=os.getcwd())
        status_code = Status.FAILED if process_return_code else Status.SUCCEEDED
        if output:
            log.info(output)
        if error:
            log.error(error)
        log.info('Finish downloading: {} to {}'.format(self._url, local_full_fn))
        return status_code

    def download(self, local_full_fn) -> int:
        log.info("Start to download from url {} to {}".format(
            self._url, local_full_fn))
        if not self.validate_source():
            log.error(f'Invalid url: {self._url}. Skip downloading.')
            return Status.FAILED

        status_code = self._download_files(local_full_fn)
        if status_code != Status.SUCCEEDED:
            log.error('Failed to download the tar ball for {}'.format(local_full_fn))
            build_name = Helper.get_build_name(local_full_fn.rsplit('/', 1)[-1])
            tags = {'type': 'http', 'build_name': build_name }
            create_sc_increment('deployd.stats.download.failed', tags=tags)
            return status_code

        try:
            sha_url = '{}.sha1'.format(self._url)
            sha_r = requests.get(sha_url)
            if sha_r.status_code != 200:
                log.warning('Skip checksum verification. Invalid response from {}'.format(sha_url))
                return status_code

            sha_value = sha_r.content
            hash_value = self.hash_file(local_full_fn)
            if hash_value != sha_value:
                log.error('Checksum failed for {}'.format(local_full_fn))
                return Status.FAILED

            log.info("Successfully downloaded to {}".format(local_full_fn))
            return Status.SUCCEEDED
        except requests.ConnectionError:
            log.error('Could not connect to: {}'.format(self._url))
            return Status.FAILED

    def validate_source(self) -> bool:
        tags = {'type': 'http', 'url': self._url}
        create_sc_increment(DOWNLOAD_VALIDATE_METRICS, tags=tags)

        parsed_url: ParseResult = urlparse(self._url)
        if not parsed_url.scheme == 'https':
            return False

        domain: str = parsed_url.netloc
        if not domain:
            return False

        allow_list = self._config.get_http_download_allow_list()

        if not allow_list or domain in allow_list:
            return True
        else:
            log.error(f"{domain} is not in the allow list: {allow_list}.")
            return False
