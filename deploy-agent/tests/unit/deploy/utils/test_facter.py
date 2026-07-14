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

    def test_cache_hit_skips_subprocess(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")
        call_count = {"n": 0}

        def fake_run(cmd, check, stdout):
            call_count["n"] += 1
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            with unittest.mock.patch(
                "deployd.common.utils.time.monotonic", return_value=1000.0
            ):
                result1 = utils.get_info_from_facter({"hostname"}, cache_ttl=300)
                result2 = utils.get_info_from_facter({"hostname"}, cache_ttl=300)

        assert result1 == payload
        assert result2 == payload
        assert call_count["n"] == 1

    def test_cache_expires_after_ttl_bucket_changes(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")
        call_count = {"n": 0}

        def fake_run(cmd, check, stdout):
            call_count["n"] += 1
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            with unittest.mock.patch(
                "deployd.common.utils.time.monotonic", return_value=1000.0
            ):
                # bucket = int(1000 / 300) = 3
                utils.get_info_from_facter({"hostname"}, cache_ttl=300)
            with unittest.mock.patch(
                "deployd.common.utils.time.monotonic", return_value=1300.0
            ):
                # bucket = int(1300 / 300) = 4 — new bucket, cache miss
                utils.get_info_from_facter({"hostname"}, cache_ttl=300)

        assert call_count["n"] == 2

    def test_no_caching_when_ttl_zero(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")
        call_count = {"n": 0}

        def fake_run(cmd, check, stdout):
            call_count["n"] += 1
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            utils.get_info_from_facter({"hostname"}, cache_ttl=0)
            utils.get_info_from_facter({"hostname"}, cache_ttl=0)

        assert call_count["n"] == 2

    def test_no_cache_false_with_ttl_does_not_add_no_cache_flag(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")

        def fake_run(cmd, check, stdout):
            assert "--no-cache" not in cmd
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            with unittest.mock.patch(
                "deployd.common.utils.time.monotonic", return_value=1000.0
            ):
                out = utils.get_info_from_facter(
                    {"hostname"}, no_cache=False, cache_ttl=300
                )

        assert out == payload

    def test_no_cache_false_with_ttl_zero_does_not_add_no_cache_flag(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")

        def fake_run(cmd, check, stdout):
            assert "--no-cache" not in cmd
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            out = utils.get_info_from_facter({"hostname"}, no_cache=False, cache_ttl=0)

        assert out == payload

    def test_no_cache_flag_bypasses_cache(self):
        payload = {"hostname": "myhost"}
        fake_stdout = json.dumps(payload).encode("utf-8")
        call_count = {"n": 0}

        def fake_run(cmd, check, stdout):
            call_count["n"] += 1
            assert "--no-cache" in cmd
            return SimpleNamespace(stdout=fake_stdout)

        utils._facter_query_cached.cache_clear()
        with unittest.mock.patch(
            "deployd.common.utils.subprocess.run", side_effect=fake_run
        ):
            with unittest.mock.patch(
                "deployd.common.utils.time.monotonic", return_value=1000.0
            ):
                utils.get_info_from_facter({"hostname"}, no_cache=True, cache_ttl=300)
                utils.get_info_from_facter({"hostname"}, no_cache=True, cache_ttl=300)

        assert call_count["n"] == 2


if __name__ == "__main__":
    unittest.main()
