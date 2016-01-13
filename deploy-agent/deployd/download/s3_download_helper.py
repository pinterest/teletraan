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

import traceback
import logging
import re

from boto.s3.connection import S3Connection
from deployd.common.config import Config
from deployd.common.status_code import Status
from deployd.download.download_helper import DownloadHelper


log = logging.getLogger(__name__)


class S3DownloadHelper(DownloadHelper):

    def __init__(self, local_full_fn, aws_connection=None, url=None):
        super(S3DownloadHelper, self).__init__(local_full_fn)
        self._s3_matcher = "^s3://(?P<BUCKET>[a-zA-Z0-9\-_]+)/(?P<KEY>[a-zA-Z0-9\-_/\.]+)/?"
        if aws_connection:
            self._aws_connection = aws_connection
        else:
            config = Config()
            aws_access_key_id = config.get_aws_access_key()
            aws_secret_access_key = config.get_aws_access_secret()
            self._aws_connection = S3Connection(aws_access_key_id, aws_secret_access_key, True)
        if url:
            self._url = url

    def download(self, local_full_fn):
        s3url_parse = re.match(self._s3_matcher, self._url)
        bucket_name = s3url_parse.group("BUCKET")
        key = s3url_parse.group("KEY")
        log.info("Start to download file {} from s3 bucket {} to {}".format(
            key, bucket_name, local_full_fn))

        try:
            filekey = self._aws_connection.get_bucket(bucket_name).get_key(key)
            if filekey is None:
                log.error("s3 key {} not found".format(key))
                return Status.FAILED

            filekey.get_contents_to_filename(local_full_fn)
            etag = filekey.etag
            if etag.startswith('"') and etag.endswith('"'):
                etag = etag[1:-1]

            md5 = self.md5_file(local_full_fn)
            if md5 != etag:
                log.error("MD5 verification failed. tarball is corrupt.")
                return Status.FAILED

            log.info("Successfully downloaded to {}".format(local_full_fn))
            return Status.SUCCEEDED
        except Exception:
            log.error("Failed to get package from s3: {}".format(traceback.format_exc()))
            return Status.FAILED
