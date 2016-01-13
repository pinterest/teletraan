import mock
import os
import tests
import time
import unittest

from deployd.common.helper import Helper


class TestAgentHelperFunctions(tests.FileTestCase):

    def test_builds_available_locally(self):
        """This uses the filesystem."""
        config = mock.Mock()
        config.get_var = mock.Mock(return_value=self.builds_dir)
        self.assertEqual([], Helper(config).builds_available_locally(self.builds_dir))
        os.mkdir(os.path.join(self.builds_dir, 'fakebuild'))
        # builds_available_locally returns a tuple with buildname and timestamp.
        # let's just look at buildname
        self.assertEqual('fakebuild',
                         Helper(config).builds_available_locally(self.builds_dir)[0][0])

    def test_get_stale_builds(self):
        """Test the ``get_stale_builds`` method.

        Make sure that we: Return nor more than n-m builds.  Where n is
        ``len(build_timestamps)`` and m is ``num_builds_to_retain``.
        """
        def check_stale_build(build_timestamps, expected_total):
            num_builds = len(
                list(
                    Helper.get_stale_builds(
                        build_timestamps,
                        2)))
            self.assertEqual(expected_total, num_builds)

        # Test no results for 0 builds, ``num_builds_to_retain=2``.
        check_stale_build([], 0)

        # Test no results for 1 builds, ``num_builds_to_retain=2``.
        check_stale_build([('abuild', 1)], 0)

        # Test no results for 2 builds, ``num_builds_to_retain=2``.
        check_stale_build([('abuild', 1)] * 2, 0)

        # Test 1 results for 3 builds, ``num_builds_to_retain=2``.
        check_stale_build([('abuild', 1)] * 3, 1)

        # Test 5 results for 7 builds, ``num_builds_to_retain=2``
        check_stale_build([('abuild', 1)] * 7, 5)

        # verify oldest build got returned first
        time1 = time.time()  # time now
        time2 = time1 - 10  # 10s ago
        time3 = time2 - 10  # 10 more seconds ago
        self.assertEqual(list(
            Helper.get_stale_builds(
                [('abuild', time1), ('abuild2', time2), ('abuild3', time3)],
                2)), ['abuild3'])


if __name__ == '__main__':
    unittest.main()
