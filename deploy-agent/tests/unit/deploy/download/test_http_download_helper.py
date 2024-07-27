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

from deployd.download.http_download_helper import HTTPDownloadHelper
from deployd.common.config import Config
import unittest
from unittest import mock
import logging

logger = logging.getLogger()
logger.level = logging.DEBUG


class TestHttpDownloadHelper(unittest.TestCase):

    def setUp(self):
        self.config = Config()
        self.downloader = HTTPDownloadHelper(url="https://deploy1.com", config=self.config)

    @mock.patch('deployd.common.config.Config.get_http_download_allow_list')
    def test_validate_url_with_allow_list(self, mock_get_http_download_allow_list):
        mock_get_http_download_allow_list.return_value = ['deploy1.com', 'deploy2.com']
        result = self.downloader.validate_source()
        self.assertTrue(result)
        mock_get_http_download_allow_list.assert_called_once()

    @mock.patch('deployd.common.config.Config.get_http_download_allow_list')
    def test_validate_url_with_non_https(self, mock_get_http_download_allow_list):
        downloader = HTTPDownloadHelper(url="http://deploy1.com")
        mock_get_http_download_allow_list.return_value = ['deploy1', 'deploy2']
        result = downloader.validate_source()

        self.assertFalse(result)
        mock_get_http_download_allow_list.assert_not_called()

    @mock.patch('deployd.common.config.Config.get_http_download_allow_list')
    def test_validate_url_without_allow_list(self, mock_get_http_download_allow_list):
        mock_get_http_download_allow_list.return_value = []
        result = self.downloader.validate_source()

        self.assertTrue(result)
        mock_get_http_download_allow_list.assert_called_once()

if __name__ == '__main__':
    unittest.main()
