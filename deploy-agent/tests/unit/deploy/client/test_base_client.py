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

import unittest
from abc import ABC, abstractmethod
from tests import TestCase

from deployd.client.base_client import BaseClient


class TestBaseClient(TestCase):

    def test_missing_abstract_method_impl_cause_error(self):

        class MyClient(BaseClient):
            pass

        with self.assertRaises(TypeError):
            MyClient()

    def test_abc_equivalent_to_old(self):
        """
        Make sure that new changes to base client extend the original class
        """
        class OldBaseClient(ABC):
            @abstractmethod
            def send_reports(self, env_reports=None):
                pass
        self.assertLessEqual(set(dir(OldBaseClient)), set(dir(BaseClient)))


if __name__ == '__main__':
    unittest.main()
