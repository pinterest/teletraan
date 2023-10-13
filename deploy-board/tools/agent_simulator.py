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

# -*- coding: utf-8 -*-
''' Simulate l00 hosts to ping server constantly. If group name match the env
capacity config and there is active deploy, it will simulate the full deploy cycle as well.
'''
import commons
import argparse
import time
import threading

states = {}

systems_helper = commons.get_system_helper()


def ping(i, groups):
    host = "host-888-%d" % i
    ip = "%d.%d.%d.%d" % (i, i, i, i)
    reports = {}

    while True:
        pingRequest = {}
        pingRequest['hostId'] = host
        pingRequest['hostName'] = host
        pingRequest['hostIp'] = ip
        pingRequest['groups'] = groups
        pingRequest['reports'] = list(reports.values())

        try:
            pingResponse = systems_helper.ping(commons.REQUEST, pingRequest)
        except Exception as e:
            print(e.message)
            continue

        if pingResponse.get('opCode') == 'NOOP':
            continue
        else:
            print("%s :-> %s:%s" % (host,
                                    pingResponse.get('opCode'),
                                    pingResponse.get('deployGoal').get('deployStage')))
        goal = pingResponse.get('deployGoal')
        report = {}
        report['envId'] = goal.get('envId')
        report['deployId'] = goal.get('deployId')
        report['agentStatus'] = 'SUCCEEDED'
        report['deployStage'] = goal.get('deployStage')
        reports[goal.get('envId')] = report


def main():
    parser = argparse.ArgumentParser(description='Simulate 100 hosts to ping server constantly')
    parser.add_argument('-g', '--group', type=str, help='Group name', required=True)
    args = parser.parse_args()
    groups = [args.group]
    for i in range(100):
        host = "host-sim-%d" % i
        states[host] = False

    for i in range(100):
        t = threading.Thread(target=ping, args=(i, groups))
        t.daemon = True
        t.start()

    while True:
        time.sleep(5)
        continue


if __name__ == "__main__":
    main()
