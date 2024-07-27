#!/usr/bin/env python3
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

""" Simulate l00 hosts to ping server constantly. If group name match the env
capacity config and there is active deploy, it will simulate the full deploy cycle as well.
"""
from commons import REQUEST
from deploy_board.webapp.helpers import systems_helper
import argparse
import time
import threading

states = {}

HOST_COUNT = 100


def ping(i: str, groups: list):
    host = f"host-888-{i}"
    ip = f"{i}.{i}.{i}.{i}"
    reports = {}

    while True:
        pingRequest = {}
        pingRequest["hostId"] = host
        pingRequest["hostName"] = host
        pingRequest["hostIp"] = ip
        pingRequest["groups"] = groups
        pingRequest["reports"] = list(reports.values())

        try:
            pingResponse = systems_helper.ping(REQUEST, pingRequest)
        except Exception as e:
            print(e)
            continue

        if pingResponse.get("opCode") == "NOOP":
            continue
        print(
            f"{host} :-> {pingResponse.get('opCode')}:{pingResponse.get('deployGoal').get('deployStage')}"
        )
        goal = pingResponse.get("deployGoal")
        report = {}
        report["envId"] = goal.get("envId")
        report["deployId"] = goal.get("deployId")
        report["agentStatus"] = "SUCCEEDED"
        report["deployStage"] = goal.get("deployStage")
        reports[goal.get("envId")] = report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Simulate 100 hosts to ping server constantly"
    )
    parser.add_argument("-g", "--group", type=str, help="Group name", required=True)
    args = parser.parse_args()
    groups = [args.group]
    for i in range(HOST_COUNT):
        states[f"host-sim-{i}"] = False

    for i in range(HOST_COUNT):
        t = threading.Thread(target=ping, args=(i, groups), daemon=True)
        t.start()

    while True:
        time.sleep(5)
        continue
