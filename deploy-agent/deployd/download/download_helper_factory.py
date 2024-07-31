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

from logging import Logger, getLogger
from typing import Optional

import boto3
from urllib.parse import urlparse

from deployd.download.download_helper import DownloadHelper
from deployd.download.s3_download_helper import S3DownloadHelper
from deployd.download.http_download_helper import HTTPDownloadHelper
from deployd.download.local_download_helper import LocalDownloadHelper


log: Logger = getLogger(__name__)


class DownloadHelperFactory(object):

    @staticmethod
    def gen_downloader(url, config) -> Optional[DownloadHelper]:
        url_parse = urlparse(url)
        if url_parse.scheme == 's3':
            aws_access_key_id = config.get_aws_access_key()
            aws_secret_access_key = config.get_aws_access_secret()
            if aws_access_key_id is None or aws_secret_access_key is None:
                log.error("aws access key id and secret access key not found")
                return None
            s3_client = boto3.client('s3', aws_access_key_id=aws_access_key_id, aws_secret_access_key=aws_secret_access_key)
            return S3DownloadHelper(local_full_fn=url, s3_client=s3_client, url=None, config=config)
        elif url_parse.scheme == 'file':
            return LocalDownloadHelper(url=url)
        else:
            return HTTPDownloadHelper(url=url, config=config)
