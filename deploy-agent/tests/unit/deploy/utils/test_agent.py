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
import os
import tests
import time
import unittest

from deployd.common.helper import Helper
from deployd.common import utils


class TestAgentHelperFunctions(tests.FileTestCase):
    def test_builds_available_locally(self):
        """This uses the filesystem."""
        config = mock.Mock()
        config.get_var = mock.Mock(return_value=self.builds_dir)
        helper = Helper(config)
        self.assertEqual(
            [], helper.builds_available_locally(self.builds_dir, "fakeenv")
        )
        open(os.path.join(self.builds_dir, "fakeenv-fakebuild.tar.gz"), "a").close()
        # builds_available_locally returns a tuple with buildname and timestamp.
        # let's just look at buildname
        self.assertEqual(
            "fakebuild",
            helper.builds_available_locally(self.builds_dir, "fakeenv")[0][0],
        )

    def test_get_stale_builds(self):
        """Test the ``get_stale_builds`` method.

        Make sure that we: Return nor more than n-m builds.  Where n is
        ``len(build_timestamps)`` and m is ``num_builds_to_retain``.
        """

        def check_stale_build(build_timestamps, expected_total):
            num_builds = len(list(Helper.get_stale_builds(build_timestamps, 2)))
            self.assertEqual(expected_total, num_builds)

        # Test no results for 0 builds, ``num_builds_to_retain=2``.
        check_stale_build([], 0)

        # Test no results for 1 builds, ``num_builds_to_retain=2``.
        check_stale_build([("abuild", 1)], 0)

        # Test no results for 2 builds, ``num_builds_to_retain=2``.
        check_stale_build([("abuild", 1)] * 2, 0)

        # Test 1 results for 3 builds, ``num_builds_to_retain=2``.
        check_stale_build([("abuild", 1)] * 3, 1)

        # Test 5 results for 7 builds, ``num_builds_to_retain=2``
        check_stale_build([("abuild", 1)] * 7, 5)

        # verify oldest build got returned first
        time1 = time.time()  # time now
        time2 = time1 - 10  # 10s ago
        time3 = time2 - 10  # 10 more seconds ago
        self.assertEqual(
            list(
                Helper.get_stale_builds(
                    [("abuild", time1), ("abuild2", time2), ("abuild3", time3)], 2
                )
            ),
            ["abuild3"],
        )

    def test_get_build_id(self):
        """
        Test get_build_id method
        """
        self.assertEqual(
            Helper.get_build_id(
                "cmp_test-r4TTZfrWQEmgyaYic8uU6w_8ef9007.tar.gz", "cmp_test"
            )[1],
            "r4TTZfrWQEmgyaYic8uU6w_8ef9007",
        )
        self.assertEqual(
            Helper.get_build_id(
                "cmp_test-r4TTZfrWQEmgyaYic8uU6w_8ef9007.zip", "cmp_test"
            )[1],
            "r4TTZfrWQEmgyaYic8uU6w_8ef9007",
        )
        self.assertFalse(Helper.get_build_id("whateverfile", "cmp_test")[0])
        self.assertFalse(Helper.get_build_id("whateverfile.tar.gz", "cmp_test")[0])
        self.assertFalse(Helper.get_build_id("cmp_test-tmp", "cmp_test")[0])

    def test_get_build_name(self):
        """
        Test get_build_name method
        """
        self.assertEqual(
            Helper.get_build_name("cmp_test-r4TTZfrWQEmgyaYic8uU6w_8ef9007.tar.gz"),
            "cmp_test",
        )
        self.assertEqual(
            Helper.get_build_name("cmp_test-r4TTZfrWQEmgyaYic8uU6w_8ef9007.zip"),
            "cmp_test",
        )

    def test_get_uptime(self):
        """
        Test utils.uptime()
        """
        self.assertTrue(isinstance(utils.uptime(), int))


if __name__ == "__main__":
    unittest.main()
