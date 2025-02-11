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
import os
from unittest import mock
import shutil
import tempfile
import unittest
import logging

log = logging.getLogger(__name__)


class DownloadFunctionsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.url = "s3://pinterest-builds/teletraan/mock.txt"
        cls.base_dir = tempfile.mkdtemp()
        builds_dir = os.path.join(cls.base_dir, "teletraan")
        cls.builds_dir = builds_dir
        if not os.path.exists(builds_dir):
            os.mkdir(builds_dir)

        target = os.path.join(builds_dir, "mock.txt")
        cls.target = target
        cls.s3_client = mock.MagicMock()
        cls.s3_client.head_object.return_value.__getitem__.return_value = (
            "f7673f4693aab49e3f8e643bc54cb70a"
        )

        def download_file(bucket, key, fn):
            with open(fn, "w") as file:
                file.write("hello mock\n")

        cls.s3_client.download_file = mock.Mock(side_effect=download_file)

    def test_download_s3(self):
        downloader = S3DownloadHelper(self.target, self.s3_client, self.url)
        downloader.download(self.target)
        self.s3_client.head_object.assert_called_once_with(
            Bucket="pinterest-builds", Key="teletraan/mock.txt"
        )
        self.s3_client.download_file.assert_called_once_with(
            "pinterest-builds", "teletraan/mock.txt", self.target
        )
        self.s3_client.head_object.return_value.__getitem__.assert_called_once_with(
            "ETag"
        )

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)


if __name__ == "__main__":
    unittest.main()
