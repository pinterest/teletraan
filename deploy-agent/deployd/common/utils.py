from __future__ import print_function
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

import code
import errno
import hashlib
import logging
import os
import signal
import sys
import traceback
import subprocess
import yaml
import json
from deployd import IS_PINTEREST

log = logging.getLogger(__name__)


# noinspection PyProtectedMember
def exit_abruptly(status=0):
    """Exit method that just quits abruptly.

    Helps with KeyError issues.

    :param status: exit code
    """
    # if we are testing we want to test gracefully or this will abort the tests
    if os.environ.get('DEPLOY_TESTING'):
        sys.exit(status)

    os._exit(status)


def touch(fname, times=None):
    try:
        with open(fname, 'a'):
            os.utime(fname, times)
    except IOError:
        log.error('Failed touching host type file {}'.format(fname))


def hash_file(filepath):
    """ hash the file content
    :param filepath: the full path of the file
    :return:the sha1 of the file data
    """
    with open(filepath, 'rb') as f:
        return hashlib.sha1(f.read()).hexdigest()


# steal from http://stackoverflow.com/questions/132058/
# showing-the-stack-trace-from-a-running-python-application
# use : sudo kill -SIGUSR1 $pid to trigger the debug
def debug(sig, frame):
    """Interrupt running process, and provide a python prompt for
    interactive debugging."""
    d = {'_frame': frame}      # Allow access to frame object.
    d.update(frame.f_globals)  # Unless shadowed by global
    d.update(frame.f_locals)

    i = code.InteractiveConsole(d)
    message = "Signal recieved : entering python shell.\nTraceback:\n"
    message += ''.join(traceback.format_stack(frame))
    i.interact(message)


def listen():
    signal.signal(signal.SIGUSR1, debug)  # Register handler


def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as ex:
        # if the directory exists, silently exits
        if ex.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise


def ensure_dirs(config):
    # make sure deployd directories exist
    mkdir_p(config.get_builds_directory())
    mkdir_p(config.get_agent_directory())
    mkdir_p(config.get_log_directory())


def run_prereqs(config):
    # check if the puppet has finished or not
    if IS_PINTEREST:
        respect_puppet = config.respect_puppet()
        puppet_file_path = config.get_puppet_file_path()
        if respect_puppet and \
           puppet_file_path is not None and \
           not os.path.exists(puppet_file_path):
            print("Waiting for first puppet run.")
            sys.exit(0)

    ensure_dirs(config)


def get_info_from_facter(keys):
    try:
        log.info("Fetching {} keys from facter".format(keys))
        cmd = ['facter', '-p', '-j']
        cmd.extend(keys)
        output = subprocess.check_output(cmd)
        if output:
            return json.loads(output)
        else:
            return None
    except:
        log.error("Failed to get info from facter by keys {}".format(keys))
        return None

def check_not_none(arg, msg=None):
    if arg is None:
        raise ValueError(msg)
    return arg

