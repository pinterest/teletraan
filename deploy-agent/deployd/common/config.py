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

import logging
import os

from deployd import __version__

from deployd.common.exceptions import DeployConfigException
from deployd.common.types import DeployType
from deployd.common.utils import exit_abruptly
from deployd.types.deploy_stage import DeployStage
from deployd.types.opcode import OperationCode

from configparser import ConfigParser


log = logging.getLogger(__name__)


class Config(object):
    _DEFAULT_CONFIG_SECTION = 'default_config'
    _configs = {}

    def __init__(self, filenames=None, config_reader=None):
        self._configs = {}
        self._filenames = None
        if config_reader:
            self._config_reader = config_reader
            return

        self._config_reader = ConfigParser()
        if not filenames:
            return

        if not os.path.exists(filenames):
            print('Cannot find config files: {}'.format(filenames))
            exit_abruptly(1)

        self._filenames = filenames
        loaded_filenames = self._config_reader.read(self._filenames)
        if len(loaded_filenames) == 0:
            print('Cannot read config files: {}'.format(self._filenames))
            exit_abruptly(1)

    def get_config_filename(self):
        return self._filenames

    def _get_deploy_type_from_opcode(self, opCode):
        # TODO: Should use common.types.OpCode for next version
        if opCode == 'RESTART':
            return DeployType.RESTART
        elif opCode == 'ROLLBACK':
            return DeployType.ROLLBACK
        elif opCode == 'STOP' or opCode == 'TERMINATE':
            return DeployType.STOP
        else:
            return DeployType.REGULAR

    def update_variables(self, deploy_status):
        if not deploy_status:
            return

        self._configs = {}
        if deploy_status.runtime_config:
            self._configs.update(deploy_status.runtime_config)

        # update environment variables
        os.environ['DEPLOY_ID'] = deploy_status.report.deployId

        # TODO: This is only used for migration, should clean them up
        if isinstance(deploy_status.report.deployStage, int):
            os.environ['DEPLOY_STEP'] = \
                DeployStage._VALUES_TO_NAMES[deploy_status.report.deployStage]
        else:
            os.environ['DEPLOY_STEP'] = deploy_status.report.deployStage

        if isinstance(deploy_status.op_code, int):
            op_code = OperationCode._VALUES_TO_NAMES[deploy_status.op_code]
        else:
            op_code = deploy_status.op_code
        os.environ['OPCODE'] = op_code

        os.environ['DEPLOY_TYPE'] = self._get_deploy_type_from_opcode(op_code)

        if deploy_status.report.envName:
            os.environ['ENV_NAME'] = deploy_status.report.envName
        if deploy_status.report.stageName:
            os.environ['STAGE_NAME'] = deploy_status.report.stageName
        if deploy_status.first_deploy:
            os.environ['FIRST_DEPLOY'] = str(deploy_status.first_deploy)
        if deploy_status.is_docker:
            os.environ['IS_DOCKER'] = str(deploy_status.is_docker)
        os.environ['TARGET'] = self.get_target()

        # export script var to environment
        if deploy_status.script_variables:
            for key, value in deploy_status.script_variables.items():
                os.environ[key] = value

        if deploy_status.build_info:
            if deploy_status.build_info.build_commit:
                os.environ['BUILD_COMMIT'] = deploy_status.build_info.build_commit
            if deploy_status.build_info.build_name:
                os.environ['BUILD_NAME'] = deploy_status.build_info.build_name
            if deploy_status.build_info.build_repo:
                os.environ['BUILD_REPO'] = deploy_status.build_info.build_repo
            if deploy_status.build_info.build_branch:
                os.environ['BUILD_BRANCH'] = deploy_status.build_info.build_branch
            if deploy_status.build_info.build_id:
                os.environ['BUILD_ID'] = deploy_status.build_info.build_id
            if deploy_status.build_info.build_url:
                os.environ['BUILD_URL'] = deploy_status.build_info.build_url

        os.environ['BUILDS_DIR'] = self.get_builds_directory()

    def get_var(self, var_name, default_value=None):
        try:
            if self._configs and var_name in self._configs:
                return self._configs[var_name]

            return self._config_reader.get(self._DEFAULT_CONFIG_SECTION, var_name)
        except Exception:
            if default_value is not None:
                return default_value
            raise DeployConfigException('{} cannot be found.'.format(var_name))

    def get_intvar(self, var_name, default_value=None):
        return int(self.get_var(var_name, default_value))

    def get_target(self):
        target_default_dir = self.get_var("target_default_dir", "/tmp")
        if not (self._configs and self._configs.get('target')):
            return os.path.join(target_default_dir, os.environ['ENV_NAME'])

        return self._configs.get('target')

    def get_subprocess_log_name(self):
        if 'ENV_NAME' in os.environ:
            return '{}/{}.log'.format(self.get_log_directory(), os.environ['ENV_NAME'])
        else:
            return os.path.join(self.get_log_directory(), "deploy_subprocess.log")

    def get_script_directory(self):
        script_dir = '{}/teletraan/'.format(self.get_target())
        subscript_dir = os.path.join(script_dir, os.environ['ENV_NAME'])
        if os.path.exists(subscript_dir):
            return subscript_dir
        else:
            return script_dir

    def get_agent_directory(self):
        return self.get_var("deploy_agent_dir", "/tmp/deployd/")

    def get_env_status_fn(self):
        return os.path.join(self.get_agent_directory(), "env_status")

    def get_host_info_fn(self):
        return os.path.join(self.get_agent_directory(), "host_info")

    def get_builds_directory(self):
        return self.get_var("builds_dir", "/tmp/deployd/builds")

    def get_log_directory(self):
        return self.get_var("log_directory", "/tmp/deployd/logs")

    def get_user_role(self):
        import getpass
        return self.get_var("user_role", getpass.getuser())

    def get_restful_service_url(self):
        return self.get_var('teletraan_service_url', 'http://localhost:8080')

    def get_restful_service_version(self):
        return self.get_var('teletraan_service_version', 'v1')

    def get_restful_service_token(self):
        return self.get_var('teletraan_service_token', '')

    # aws specific configuration
    def get_aws_access_key(self):
        return self.get_var('aws_access_key_id', None)

    def get_aws_access_secret(self):
        return self.get_var('aws_secret_access_key', None)

    # agent process configs
    def get_agent_ping_interval(self):
        return self.get_intvar('min_running_time', 60)

    def get_subprocess_running_timeout(self):
        return self.get_intvar('process_timeout', 1800)

    def get_subproces_max_retry(self):
        return self.get_intvar('max_retry', 3)

    def get_subprocess_max_log_bytes(self):
        return self.get_intvar('max_tail_bytes', 10240)

    def get_subprocess_max_sleep_interval(self):
        return self.get_intvar('max_sleep_interval', 60)

    def get_subprocess_poll_interval(self):
        return self.get_intvar("process_wait_interval", 2)

    def get_backoff_factor(self):
        return self.get_intvar("back_off_factor", 2)

    def get_num_builds_retain(self):
        return self.get_intvar("num_builds_to_retain", 2)

    def respect_puppet(self):
        return self.get_intvar("respect_puppet", 0)

    def get_puppet_file_path(self):
        return self.get_var("puppet_file_path", None)

    def get_daemon_sleep_time(self):
        return self.get_intvar("daemon_sleep_time", 30)
    
    def get_init_sleep_time(self):
        return self.get_intvar("init_sleep_time", 50)

    def get_log_level(self):
        log_level = self.get_var("log_level", 'DEBUG')
        if log_level == "INFO":
            return logging.INFO
        elif log_level == "ERROR":
            return logging.ERROR
        return logging.DEBUG

    def get_facter_id_key(self):
        return self.get_var('agent_id_key', None)

    def get_facter_ip_key(self):
        return self.get_var('agent_ip_key', None)

    def get_facter_name_key(self):
        return self.get_var('agent_name_key', None)

    def get_facter_group_key(self):
        return self.get_var('agent_group_key', None)
    
    def get_verify_https_certificate(self):
        return self.get_var('verify_https_certificate', 'False')

    def get_deploy_agent_version(self):
        return self.get_var('deploy_agent_version', __version__)

    def get_facter_az_key(self):
        return self.get_var('availability_zone_key', None)

    def get_facter_ec2_tags_key(self):
        return self.get_var('ec2_tags_key', None)

    def get_facter_asg_tag_key(self):
        return self.get_var('autoscaling_tag_key', None)

    def get_stage_type_key(self):
        return self.get_var('stage_type_key', None)
