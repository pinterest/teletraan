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

import json
from types import SimpleNamespace
import unittest
import tests

import deployd.common.utils as utils


class TestFacter(tests.TestCase):
    def test_get_info_from_facter_adds_no_cache_flag(self):
        # Arrange: subprocess.run returns JSON bytes
        payload = {"ec2_metadata": {"instance_id": "i-123"}}
        fake_stdout = json.dumps(payload).encode("utf-8")

        def fake_run(cmd, check, stdout):
            # Assert: --no-cache is present when no_cache=True
            assert cmd[0:2] == ["facter", "-jp"]
            assert "--no-cache" in cmd
            return SimpleNamespace(stdout=fake_stdout)

        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            out = utils.get_info_from_facter({"ec2_metadata"}, no_cache=True)

        assert out == payload


if __name__ == "__main__":
    unittest.main()
