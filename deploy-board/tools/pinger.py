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

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
''' Could do one ping based on your customized request
'''
import json
import commons
import argparse

systems_helper = commons.get_system_helper()
environs_helper = commons.get_environ_helper()
deploys_helper = commons.get_deploy_helper()
agents_helper = commons.get_agent_helper()


def main():
    parser = argparse.ArgumentParser(description='Ping server once')
    parser.add_argument('-i', '--host', type=str, default='deploy-sentinel-1',
                        help='host name, default is deploy-sentinel-1')
    parser.add_argument('-g', '--group', nargs='+', type=str, default='deploy-sentinel',
                        help='group name, default is deploy-sentinel')
    parser.add_argument('-n', '--env-name', type=str, default=['deploy-sentinel'],
                        help='env name to update, default is deploy-sentinel')
    parser.add_argument('-s', '--stage-name', type=str, default='prod',
                        help='env stage to update, default is prod')
    parser.add_argument('--deploy-stage', type=str, default="PRE_DOWNLOAD",
                        help='deploy stage to set')
    parser.add_argument('--agent-status', type=str, default="SUCCEEDED",
                        help='deploy status to set')
    parser.add_argument('--error-code', type=int, default=0,
                        help='Agent error code')
    parser.add_argument('--error-message', type=str, default=None,
                        help='Agent error message')
    parser.add_argument('--deploy-alias', type=str, default=None,
                        help='Deploy alias')
    parser.add_argument('--fail-count', type=int, default=0,
                        help='Failed number')
    args = parser.parse_args()

    # first locate the env and deploy
    env = environs_helper.get_env_by_stage(commons.REQUEST, args.env_name, args.stage_name)

    pingRequest = {}
    pingRequest['hostId'] = args.host
    pingRequest['hostName'] = args.host
    pingRequest['hostIp'] = "8.8.8.8"
    pingRequest['groups'] = args.group

    report = {}
    report['envId'] = env['id']
    report['deployId'] = env['deployId']
    report['deployStage'] = args.deploy_stage
    report['agentStatus'] = args.agent_status
    report['errorCode'] = args.error_code
    report['errorMessage'] = args.error_message
    report['deployAlias'] = args.deploy_alias
    report['failCount'] = args.fail_count

    pingRequest['reports'] = [report]

    print("----------------REQUEST-------------------")
    print(json.dumps(pingRequest, indent=2))

    pingResponse = systems_helper.ping(commons.REQUEST, pingRequest)

    print("----------------RESPONSE----------------------")
    print(json.dumps(pingResponse, indent=2))


if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        print('Exception error is: %s' % e.message)
