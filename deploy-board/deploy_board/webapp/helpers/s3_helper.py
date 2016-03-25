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

# -*- coding: utf-8 -*-
"""helper class for S3 using boto
"""

import boto

class S3Helper(object):
    def __init__(self, bucket_name=None):
        self.s3 = boto.connect_s3()
        self.bucket = self.s3.get_bucket(bucket_name)

    def exists(self, key):
        return self.bucket.get_key(key) is not None

    def upload_file(self, key, path):
        self.bucket.new_key(key).set_contents_from_filename(path)

    def upload_string(self, key, str):
        self.bucket.new_key(key).set_contents_from_string(str)

    def download_file(self, key, path):
        self.bucket.get_key(key).get_contents_to_filename(path)

    def download_string(self, key):
        return self.bucket.get_key(key).get_contents_as_string()

    def list(self, key):
        return self.bucket.list(prefix=key)

