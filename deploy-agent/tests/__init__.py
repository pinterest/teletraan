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

import mock
import logging
import os
import shutil
import tempfile
import unittest

from deployd.common.types import OpCode


class TestCase(unittest.TestCase):
    process_count_file = None

    def setUp(self):
        # Silence the logger that talks to HipChat
        logger = logging.getLogger('deployd.messaging')
        logger.handlers = []

        os.environ['DEPLOY_TESTING'] = '1'
        self.client = mock.Mock()
        self.client.sendRequest = mock.Mock(return_value=(OpCode.NOOP, None))

    def write_process_count(self, count):
        """Writes out a process count file to a tmp location."""
        fd, self.process_count_file = tempfile.mkstemp()
        with os.fdopen(fd, 'w') as f:
            f.write(str(count))

        return self.process_count_file

    def tearDown(self):
        if self.process_count_file and os.path.exists(self.process_count_file):
            os.remove(self.process_count_file)


class FileTestCase(TestCase):
    def setUp(self):
        super(FileTestCase, self).setUp()
        self.sandbox = tempfile.mkdtemp()
        self.builds_dir = os.path.join(self.sandbox, 'builds')
        os.mkdir(self.builds_dir)
        self.target = os.path.join(self.sandbox, 'target')

    def tearDown(self):
        shutil.rmtree(self.sandbox)
