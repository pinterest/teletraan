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
from deployd import IS_PINTEREST, PUPPET_SUCCESS_EXIT_CODES, REDEPLOY_MAX_RETRY
from deployd.common.stats import TimeElapsed, create_sc_increment, create_sc_timing, send_statsboard_metric

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


def uptime():
    """ return int: seconds of uptime in int, default 0 """
    sec = 0
    if sys.platform.startswith('linux'):
        with open('/proc/uptime') as proc:
            line = proc.readline().split()
            sec = int(float(line[0]))
    return sec


def ensure_dirs(config):
    # make sure deployd directories exist
    mkdir_p(config.get_builds_directory())
    mkdir_p(config.get_agent_directory())
    mkdir_p(config.get_log_directory())


def is_first_run(config):
    env_status_file = config.get_env_status_fn()
    return not os.path.exists(env_status_file)


def check_prereqs(config):
    """
    Check prerequisites before deploy agent can run

    :return: True all conditions meet else False
    """
    if IS_PINTEREST:
        respect_puppet = config.respect_puppet()
        # check if the puppet has finished successfully or not
        if respect_puppet:
            puppet_state_file_path = config.get_puppet_state_file_path()
            if puppet_state_file_path and (not os.path.exists(puppet_state_file_path)):
                log.error("Waiting for first puppet run.")
                return False
            if not check_first_puppet_run_success(config):
                log.error("First puppet run failed.")
                return False

    ensure_dirs(config)
    return True


def get_puppet_exit_code(config):
    """
    Get puppet exit code from the corresponding file

    :return: puppet exit code or 999 if file doesn't exist
    """
    puppet_exit_code_file = config.get_puppet_exit_code_file_path()
    try:
        with open(puppet_exit_code_file, "rt") as f:
            exit_code = f.readline().strip()
    except Exception as e:
        log.warning(f"Could not read {puppet_exit_code_file} file: {e}")
        exit_code = 999

    return exit_code


def load_puppet_summary(config):
    """
    Load last_run_summary yaml file, parse results

    :return: returns a dict constructed from for the puppet summary file
    """
    summary_file = config.get_puppet_summary_file_path()
    summary = {}
    if not os.path.exists(summary_file):
        log.warning(f"{summary_file} does not exist. This could be the first puppet run")
        return summary

    with open(summary_file) as f:
        summary = yaml.safe_load(f)
    return summary


def check_first_puppet_run_success(config):
    """
    Check first puppet run success from exit code and last run summary

    :return: returns True if success else False
    """
    if not is_first_run(config):
        return True

    puppet_exit_code = get_puppet_exit_code(config)
    if puppet_exit_code in PUPPET_SUCCESS_EXIT_CODES:
        return True

    # If failed, double check with puppet last summary
    puppet_summary = load_puppet_summary(config)
    puppet_failures = puppet_summary.get('events', {}).get(
        'failure', None) if puppet_summary else None
    log.info(f"Puppet failures: {puppet_failures}")

    if puppet_failures != 0:
        send_statsboard_metric(name='deployd.first_puppet_failed', value=1,
                               tags={"puppet_exit_code": puppet_exit_code})
    return puppet_failures == 0


def get_info_from_facter(keys):
    try:
        time_facter = TimeElapsed()
        # increment stats - facter calls
        create_sc_increment('deployd.stats.internal.facter_calls_sum', 1)
        log.info(f"Fetching {keys} keys from facter")
        cmd = ['facter', '-jp']
        cmd.extend(keys)
        output = subprocess.run(cmd, check=True, stdout=subprocess.PIPE).stdout
        # timing stats - facter run time
        create_sc_timing('deployd.stats.internal.time_elapsed_facter_calls_sec',
                         time_facter.get())
        if output:
            return json.loads(output)
        else:
            log.warn("Got empty output from facter by keys {}".format(keys))
            return None
    except:
        log.exception("Failed to get info from facter by keys {}".format(keys))
        return None


def redeploy_check(labels, service):
    max_retry = REDEPLOY_MAX_RETRY
    for label in labels:
        if "redeploy_max_retry" in label:
            max_retry = int(label.split('=')[1])
    retry_num = 0
    file_name = "/mnt/deployd/" + service
    if os.path.exists(file_name):
        with open(file_name, mode="r") as f:
            retry_num = int(f.readline())
    if retry_num < max_retry:
        with open(file_name, mode="w") as ff:
            ff.write('%d' % (retry_num + 1))
            return True
    return False
    
def get_container_health_info(commit, service):
    try:
        log.info(f"Get health info for container with commit {commit}")
        result = []
        cmd = ['docker', 'ps', '--format', '{{.Image}};{{.Names}};{{.Labels}}']
        output = subprocess.run(cmd, check=True, stdout=subprocess.PIPE).stdout
        if output:
            lines = output.decode().strip().splitlines()
            for line in lines:
                if commit in line:
                    parts = line.split(';')
                    name = parts[1]
                    try:
                        command = ['docker', 'inspect', '-f', '{{.State.Health.Status}}', name]
                        status = subprocess.run(command, check=True, stdout=subprocess.PIPE).stdout
                        if status:
                            status = status.decode().strip()
                            if status == "unhealthy" and "redeploy_when_unhealthy=enabled" in parts[2]:
                                labels = parts[2].split(',')
                                if redeploy_check(labels, service) == True:
                                    return "delete"
                            result.append(f"{name}:{status}")
                    except:
                        continue
            return ";".join(result) if result else None
        else:
            return None
    except:
        log.error(f"Failed to get container health info with commit {commit}")
        # Report failure
        return None


def get_telefig_version():
    if not IS_PINTEREST:
        return None    
    try:
        cmd = ['configure-serviceset', '-v']
        output = subprocess.run(cmd, check=True, stdout=subprocess.PIPE).stdout
        if output:
            return output.decode().strip()
        else:
            return None
    except:
        log.error("Error when fetching teletraan configure manager version")
        return None

def check_not_none(arg, msg=None):
    if arg is None:
        raise ValueError(msg)
    return arg
