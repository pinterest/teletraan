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

from deployd.download.s3_download_helper import S3DownloadHelper
import unittest
from unittest import mock
import logging
from deployd.common.config import Config
logger = logging.getLogger()
logger.level = logging.DEBUG


class TestS3DownloadHelper(unittest.TestCase):
    @mock.patch('deployd.download.s3_download_helper.Config.get_aws_access_key')
    @mock.patch('deployd.download.s3_download_helper.Config.get_aws_access_secret')
    def setUp(self, mock_aws_key, mock_aws_secret):
        mock_aws_key.return_value = "test_key"
        mock_aws_secret.return_value= "test_secret"
        self.config = Config()
        self.downloader = S3DownloadHelper(local_full_fn='', aws_connection=None, url="s3://bucket1/key1", config=self.config)

    @mock.patch('deployd.common.config.Config.get_s3_download_allow_list')
    def test_validate_url_with_allow_list(self, mock_get_s3_download_allow_list):
        mock_get_s3_download_allow_list.return_value = ['bucket1', 'bucket2', 'bucket3']
        result = self.downloader.validate_source()
        self.assertTrue(result)
        mock_get_s3_download_allow_list.assert_called_once()

        mock_get_s3_download_allow_list.return_value = ['bucket3']
        result = self.downloader.validate_source()
        self.assertFalse(result)

    @mock.patch('deployd.common.config.Config.get_s3_download_allow_list')
    def test_validate_url_without_allow_list(self, mock_get_s3_download_allow_list):
        mock_get_s3_download_allow_list.return_value = []
        result = self.downloader.validate_source()

        self.assertTrue(result)
        mock_get_s3_download_allow_list.assert_called_once()

    @mock.patch('deployd.common.config.Config.get_s3_download_allow_list')
    def test_validate_url_without_allow_list(self, mock_get_s3_download_allow_list):
        mock_get_s3_download_allow_list.return_value = []
        result = self.downloader.validate_source()

        self.assertTrue(result)
        mock_get_s3_download_allow_list.assert_called_once()

if __name__ == '__main__':
    unittest.main()
