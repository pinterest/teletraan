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
from .downloader import Status
from deployd.download.download_helper import DownloadHelper
import os
import requests
import logging
requests.packages.urllib3.disable_warnings()

log = logging.getLogger(__name__)


class HTTPDownloadHelper(DownloadHelper):

    def _download_files(self, local_full_fn):
        download_cmd = ['curl', '-o', local_full_fn, '-fks', self._url]
        log.info('Running command: {}'.format(' '.join(download_cmd)))
        error_code = Status.SUCCEEDED
        output, error, status = Caller.call_and_log(download_cmd, cwd=os.getcwd())
        if output:
            log.info(output)
        if error:
            log.error(error)
        if status:
            error_code = Status.FAILED
        log.info('Finish downloading: {} to {}'.format(self._url, local_full_fn))
        return error_code

    def download(self, local_full_fn):
        log.info("Start to download from url {} to {}".format(
            self._url, local_full_fn))

        error = self._download_files(local_full_fn)
        if error != Status.SUCCEEDED:
            log.error('Failed to download the tar ball for {}'.format(local_full_fn))
            return error

        try:
            sha_url = '{}.sha1'.format(self._url)
            sha_r = requests.get(sha_url)
            if sha_r.status_code != 200:
                log.error('sha1 file does not exist for {}, ignore checksum.'.format(self._url))
                return error

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
