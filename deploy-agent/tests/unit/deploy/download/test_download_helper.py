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
import mock
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
        builds_dir = os.path.join(cls.base_dir, 'teletraan')
        cls.builds_dir = builds_dir
        if not os.path.exists(builds_dir):
            os.mkdir(builds_dir)

        target = os.path.join(builds_dir, 'mock.txt')
        cls.target = target
        cls.aws_conn = mock.Mock()
        aws_filekey = cls.aws_conn.get_bucket.return_value.get_key.return_value

        def get_contents_to_filename(fn):
            with open(fn, 'w') as file:
                file.write("hello mock\n")
        aws_filekey.get_contents_to_filename = mock.Mock(side_effect=get_contents_to_filename)
        aws_filekey.etag = "f7673f4693aab49e3f8e643bc54cb70a"

    def test_download_s3(self):
        downloader = S3DownloadHelper(self.target, self.aws_conn, self.url)
        downloader.download(self.target)
        self.aws_conn.get_bucket.assert_called_once_with("pinterest-builds")
        self.aws_conn.get_bucket.return_value.get_key.assert_called_once_with("teletraan/mock.txt")
        self.aws_conn.get_bucket.return_value.get_key.return_value\
            .get_contents_to_filename\
            .assert_called_once_with(self.target)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)

if __name__ == '__main__':
    unittest.main()
