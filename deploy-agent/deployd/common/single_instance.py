from __future__ import print_function
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

import logging
import os
import stat
import tempfile
import fcntl
from . import utils

log = logging.getLogger(__name__)


class SingleInstance(object):
    def __init__(self):
        # Establish lock file settings
        appname = 'deploy-agent'
        lockfile_name = '.{}.lock'.format(appname)
        lockfile_path = os.path.join(tempfile.gettempdir(), lockfile_name)
        lockfile_flags = os.O_WRONLY | os.O_CREAT
        # This is 0o222, i.e. 146, --w--w--w-
        lockfile_mode = stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH

        # Create lock file
        umask_original = os.umask(0)
        try:
            self.lockfile_fd = os.open(lockfile_path, lockfile_flags, lockfile_mode)
            log.info("Open file {} with flags {} and mode {}".format(lockfile_path, lockfile_flags, lockfile_mode))
        except Exception as e:
            log.error("Unexpected exception: {}".format(e))
        finally:
            os.umask(umask_original)

        # Try locking the file
        try:
            fcntl.lockf(self.lockfile_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            log.info("Acquired lock on file {}".format(lockfile_path))
        except IOError as e:
            log.error("IO Exception: errno {}, errmsg {}".format(e.errno, e.strerror))
            log.error(('Error: {0} may already be running. Only one instance of it '
                   'can run at a time.').format(appname))
            # noinspection PyTypeChecker
            os.close(self.lockfile_fd)
            utils.exit_abruptly(1)
        except Exception as e:
            log.error("Unexpected exception: {}".format(e))
            os.close(self.lockfile_fd)
            utils.exit_abruptly(1)
        
    def __del__(self):
        os.close(self.lockfile_fd)

