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

from unittest import mock
import os.path
import shutil
import unittest
import tempfile
import getpass

from deployd.common.status_code import Status
from deployd.staging.stager import Stager


class TestStagingHelper(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.base_dir = tempfile.mkdtemp()
        builds_dir = os.path.join(cls.base_dir, 'builds')
        target = os.path.join(cls.base_dir, 'test')
        cls.target = target
        cls.builds_dir = builds_dir
        cls.user_role = getpass.getuser()

        if not os.path.exists(builds_dir):
            os.mkdir(builds_dir)

        def mock_get_var(var_name):
            if var_name == 'builds_dir':
                return builds_dir
            elif var_name == 'env_directory':
                return cls.base_dir
            elif var_name == 'package_format':
                return 'tar.gz'
            elif var_name == "deploy_agent_dir":
                return cls.base_dir
            elif var_name == "user_role":
                return getpass.getuser()

        cls.config = mock.Mock()
        cls.config.get_var = mock.MagicMock(side_effect=mock_get_var)
        cls.config.get_target = mock.MagicMock(return_value=target)
        cls.config.get_builds_directory = mock.MagicMock(return_value=builds_dir)
        cls.transformer = mock.Mock()
        cls.transformer.dict_size = mock.Mock(return_value=1)
        cls.transformer.transform_scripts = mock.Mock()

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)

    def test_enable_new(self):
        tarball_dir = os.path.join(self.builds_dir, '24714bc')
        self.config.get_user_role = mock.MagicMock(return_value=self.user_role)
        if not os.path.exists(tarball_dir):
            os.mkdir(tarball_dir)
        script_dir = os.path.join(tarball_dir, "teletraan")
        if not os.path.exists(script_dir):
            os.mkdir(script_dir)

        self.assertTrue(Stager(config=self.config, transformer=self.transformer,
                               build="24714bc", target=self.target,
                               env_name="test").enable_package() == Status.SUCCEEDED)
        self.assertTrue(os.path.exists(self.target))
        self.assertTrue(os.path.exists(os.path.join(self.target, "teletraan_template")))
        self.assertEqual(os.readlink(self.target), os.path.join(self.builds_dir, '24714bc'))
        os.remove(self.target)

    def test_enable_build_with_old(self):
        self.config.get_user_role = mock.MagicMock(return_value=self.user_role)
        old_tarball_dir = os.path.join(self.builds_dir, '24714bc')
        if not os.path.exists(old_tarball_dir):
            os.mkdir(old_tarball_dir)

        os.symlink(os.path.join(self.builds_dir, '24714bc'), self.target)
        tarball_dir = os.path.join(self.builds_dir, '1234567')
        if not os.path.exists(tarball_dir):
            os.mkdir(tarball_dir)

        Stager(config=self.config, transformer=self.transformer,
               build="1234567", target=self.target,
               env_name="test").enable_package()

        self.assertEqual(os.readlink(self.target), os.path.join(self.builds_dir, '1234567'))
        os.remove(self.target)

    def test_get_enabled_build(self):
        missing_target = os.path.join(self.builds_dir, 'foo')
        os.symlink(missing_target, self.target)
        stager = Stager(config=self.config, transformer=self.transformer,
                        build="24714bc", target=self.target, env_name="test")
        self.assertEqual(None, stager.get_enabled_build())
        # now let's make our missing_target a real target!
        os.mkdir(missing_target)
        self.assertEqual('foo', stager.get_enabled_build())
