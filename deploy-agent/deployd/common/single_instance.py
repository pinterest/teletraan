import logging
import os
import stat
import tempfile
import fcntl
import utils
import subprocess
from deployd import IS_PINTEREST

log = logging.getLogger(__name__)


class SingleInstance(object):
    def __init__(self):
        if IS_PINTEREST:
            _RUNNING_AGENT_PATTERN = ".*/deployd_venv/bin/python.*/deployd_venv/bin/deploy-agent"
            try:
                res = subprocess.check_output('pgrep -f \"%s\"' % _RUNNING_AGENT_PATTERN,
                                              shell=True)
                if res:
                    lists = res[:-1].split()
                    # except the pgrep subprocess, and current running deploy-agent.
                    if len(lists) > 2:
                        log.info('Another deploy-agent with pid {} is running'.format(res))
                        utils.exit_abruptly(1)
            except subprocess.CalledProcessError as e:
                if e.returncode == 1:
                    return
                else:
                    utils.exit_abruptly(1)
            except Exception:
                utils.exit_abruptly(1)
        else:
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
                lockfile_fd = os.open(lockfile_path, lockfile_flags, lockfile_mode)
            finally:
                os.umask(umask_original)

            # Try locking the file
            try:
                fcntl.lockf(lockfile_fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except IOError:
                print ('Error: {0} may already be running. Only one instance of it '
                       'can run at a time.').format(appname)
                # noinspection PyTypeChecker
                os.close(lockfile_fd)
                utils.exit_abruptly(1)
