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

import abc
import hashlib
import logging

log = logging.getLogger(__name__)

DOWNLOAD_VALIDATE_METRICS = 'deployd.stats.download.validate'

class DownloadHelper(metaclass=abc.ABCMeta):

    def __init__(self, url) -> None:
        self._url = url

    @staticmethod
    def hash_file(file_path) -> str:
        sha = hashlib.sha1()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                sha.update(chunk)
        return sha.hexdigest()

    @staticmethod
    def md5_file(file_path) -> str:
        md5 = hashlib.md5()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                md5.update(chunk)
        return md5.hexdigest()

    @abc.abstractmethod
    def download(self, local_full_fn):
        pass

    @abc.abstractmethod
    def validate_source(self):
        pass
