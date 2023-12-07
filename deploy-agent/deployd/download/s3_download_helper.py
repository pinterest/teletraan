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
from deployd.download.download_helper import DownloadHelper, DOWNLOAD_VALIDATE_METRICS
from deployd.common.stats import create_sc_increment

log = logging.getLogger(__name__)


class S3DownloadHelper(DownloadHelper):

    def __init__(self, local_full_fn, aws_connection=None, url=None):
        super(S3DownloadHelper, self).__init__(local_full_fn)
        self._s3_matcher = "^s3://(?P<BUCKET>[a-zA-Z0-9\-_]+)/(?P<KEY>[a-zA-Z0-9\-_/\.]+)/?"
        self._config = Config()
        if aws_connection:
            self._aws_connection = aws_connection
        else:
            aws_access_key_id = self._config.get_aws_access_key()
            aws_secret_access_key = self._config.get_aws_access_secret()
            self._aws_connection = S3Connection(aws_access_key_id, aws_secret_access_key, True)

        if url:
            self._url = url
            s3url_parse = re.match(self._s3_matcher, self._url)
            self._bucket_name = s3url_parse.group("BUCKET")
            self._key = s3url_parse.group("KEY")


    def download(self, local_full_fn):
        log.info(f"Start to download file {self._key} from s3 bucket {self._bucket_name} to {local_full_fn}")
        if not self.validate_source():
            log.error(f'Invalid url: {self._url}. Skip downloading.')
            return Status.FAILED

        try:
            filekey = self._aws_connection.get_bucket(self._bucket_name).get_key(self._key)
            if filekey is None:
                log.error("s3 key {} not found".format(self._key))
                return Status.FAILED

            filekey.get_contents_to_filename(local_full_fn)
            etag = filekey.etag
            if "-" not in etag:
                if etag.startswith('"') and etag.endswith('"'):
                    etag = etag[1:-1]

                md5 = self.md5_file(local_full_fn)
                if md5 != etag:
                    log.error("MD5 verification failed. tarball is corrupt.")
                    return Status.FAILED
            else:
                log.info("MD5 verification currently not supported on multipart uploads.")

            log.info("Successfully downloaded to {}".format(local_full_fn))
            return Status.SUCCEEDED
        except Exception:
            log.error("Failed to get package from s3: {}".format(traceback.format_exc()))
            return Status.FAILED

    def validate_source(self):
        allow_list = self._config.get_s3_download_allow_list()
        tags = {'type': 's3', 'url': self._url, 'bucket' : self._bucket_name}
        create_sc_increment(DOWNLOAD_VALIDATE_METRICS, tags=tags)

        return self._bucket_name in allow_list if allow_list else True