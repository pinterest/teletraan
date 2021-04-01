from __future__ import absolute_import
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

import argparse
import os
from pwd import getpwnam
import sys
import shutil
import traceback
import logging

from deployd.common.caller import Caller
from deployd.common.config import Config
from deployd.common.status_code import Status
from .transformer import Transformer

log = logging.getLogger(__name__)


class Stager(object):
    _script_dirname = "teletraan"
    _template_dirname = "teletraan_template"

    def __init__(self, config, build, target, env_name, transformer=None):
        self._build_dir = config.get_builds_directory()
        self._user_role = config.get_user_role()
        agent_dir = config.get_agent_directory()
        self._transformer = \
            transformer or Transformer(agent_dir=agent_dir, env_name=env_name)
        self._build = build
        self._target = target

    def enable_package(self):
        """Set the enabled build.
        """
        old_build = self.get_enabled_build()
        if self._build == old_build:
            self.transform_script()
            log.info("Build already at {}".format(self._build))
            return Status.SUCCEEDED

        old_build = self.get_enabled_build()
        build_dir = os.path.join(self._build_dir, self._build)
        # Make a tmp_symlink
        tmp_symlink = "{}_tmp".format(self._target)
        if os.path.exists(tmp_symlink):
            os.remove(tmp_symlink)

        status_code = Status.SUCCEEDED
        try:
            # change the owner of the directory
            uinfo = getpwnam(self._user_role)
            owner = '{}:{}'.format(uinfo.pw_uid, uinfo.pw_gid)
            commands = ['chown', '-R', owner, build_dir]
            log.info('Running command: {}'.format(' '.join(commands)))
            output, error, status = Caller().call_and_log(commands)
            if status != 0:
                log.error(error)
                return Status.FAILED

            # setup symlink
            os.symlink(build_dir, tmp_symlink)
            # Move tmp_symlink over existing symlink.
            os.rename(tmp_symlink, self._target)
            log.info("{} points to {} (previously {})".format(
                self._target,
                self.get_enabled_build(),
                old_build))
            self.transform_script()
        except Exception:
            log.error(traceback.format_exc())
            status_code = Status.FAILED
        finally:
            return status_code

    def get_enabled_build(self):
        """Figure out what build is enabled by looking at symlinks."""
        if not os.path.exists(self._target):
            if (os.path.islink(self._target) and not
                    os.path.lexists(self._target)):
                symlink_target = os.readlink(self._target)
                log.info("{} points to {} which does not exist".format(
                    self._target, symlink_target))
            else:
                log.info("{} does not exist".format(self._target))
            return None

        if not os.path.islink(self._target):
            log.info("{} is not a symlink".format(self._target))
            return None

        symlink_target = os.readlink(self._target)

        return symlink_target.rsplit("/", 1)[-1]

    def transform_script(self):
        script_dir = os.path.join(self._target, self._script_dirname)
        if not os.path.exists(script_dir):
            return

        template_dir = os.path.join(self._target, self._template_dirname)
        # copy user script template to a dedicated template directory
        if not os.path.exists(template_dir):
            shutil.copytree(script_dir, template_dir)

        self._transformer.transform_scripts(script_dir=template_dir,
                                            template_dirname=self._template_dirname,
                                            script_dirname=self._script_dirname)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('-f', '--config-file', dest='config_file', default=None,
                        help="the deploy agent conf file filename path. If none, "
                             "/etc/deployagent.conf will be used")
    parser.add_argument('-v', '--build-id', dest='build', required=True,
                        help="the current deploying build version for the current environment.")
    parser.add_argument('-t', '--target', dest='target', required=True,
                        help="The deploy target directory name.")
    parser.add_argument('-e', '--env-name', dest='env_name', required=True,
                        help="the environment name currently in deploy.")
    args = parser.parse_args()
    config = Config(args.config_file)
    logging.basicConfig(level=config.get_log_level())

    log.info("Start to stage the package.")
    result = Stager(config=config, build=args.build,
                    target=args.target, env_name=args.env_name).enable_package()
    if result == Status.SUCCEEDED:
        return 0
    else:
        return 1


if __name__ == '__main__':
    sys.exit(main())
