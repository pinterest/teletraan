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
import unittest
import commons

builds_helper = commons.get_build_helper()
environs_helper = commons.get_environ_helper()
systems_helper = commons.get_system_helper()
schedules_helper = commons.get_schedule_helper()


class TestEnvirons(unittest.TestCase):
    envName = ""
    stageName = ""

    @classmethod
    def setUpClass(cls):
        cls.envName = "test-config-" + commons.gen_random_num()
        cls.stageName = "prod"
        data = {}
        data["description"] = 'foo\'s "big deal".'
        data["envName"] = cls.envName
        data["stageName"] = cls.stageName
        environs_helper.create_env(commons.REQUEST, data)

    @classmethod
    def tearDownClass(cls):
        environs_helper.delete_env(commons.REQUEST, cls.envName, cls.stageName)

    def testGets(self):
        stages = environs_helper.get_all_env_stages(
            commons.REQUEST, TestEnvirons.envName
        )
        self.assertTrue(len(stages) == 1)

        names = environs_helper.get_all_env_names(commons.REQUEST, index=1, size=1)
        self.assertTrue(len(names) == 1)

    def testBasicConfigs(self):
        oldBuildName = environs_helper.get_env_by_stage(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )["buildName"]
        self.assertEqual(oldBuildName, TestEnvirons.envName)
        environs_helper.update_env_basic_config(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            {"buildName": "foo"},
        )
        newBuildName = environs_helper.get_env_by_stage(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )["buildName"]
        self.assertEqual(newBuildName, "foo")

    def testCapacityConfigs(self):
        hosts = environs_helper.get_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="HOST",
        )
        self.assertEqual(len(hosts), 0)
        groups = environs_helper.get_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="GROUP",
        )
        self.assertEqual(len(groups), 0)
        hosts = ["host1", "host2"]
        groups = ["group"]
        environs_helper.update_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="HOST",
            data=hosts,
        )
        environs_helper.update_env_capacity(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, data=groups
        )
        new_hosts = environs_helper.get_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="HOST",
        )
        self.assertEqual(hosts, new_hosts)
        new_groups = environs_helper.get_env_capacity(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(groups, new_groups)

        # delete them, otherwise we could not delete the environ
        hosts = []
        groups = []
        environs_helper.update_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="HOST",
            data=hosts,
        )
        environs_helper.update_env_capacity(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, data=groups
        )
        new_hosts = environs_helper.get_env_capacity(
            commons.REQUEST,
            TestEnvirons.envName,
            TestEnvirons.stageName,
            capacity_type="HOST",
        )
        self.assertEqual(len(new_hosts), 0)
        new_groups = environs_helper.get_env_capacity(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(len(new_groups), 0)

    def testAdvancedConfigs(self):
        oldConfigs = environs_helper.get_env_agent_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertTrue(len(oldConfigs) == 0)

        # firsttime, should be a create
        configs = {"foo1": "bar1", "foo2": "bar2"}
        environs_helper.update_env_agent_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, configs
        )
        newConfigs = environs_helper.get_env_agent_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(configs, newConfigs)

        configs = {"foo1": "bar2", "foo3": "bar3"}
        environs_helper.update_env_agent_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, configs
        )
        newConfigs = environs_helper.get_env_agent_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(configs, newConfigs)

    def testScriptConfigs(self):
        oldConfigs = environs_helper.get_env_script_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertTrue(len(oldConfigs) == 0)

        configs = {"foo1": "bar1", "foo2": "bar2"}
        environs_helper.update_env_script_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, configs
        )
        newConfigs = environs_helper.get_env_script_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(configs, newConfigs)

        configs = {"foo1": "bar2", "foo3": "bar3"}
        environs_helper.update_env_script_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, configs
        )
        newConfigs = environs_helper.get_env_script_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(configs, newConfigs)

    def testAlarmsConfigs(self):
        oldAlarms = environs_helper.get_env_alarms_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertTrue(len(oldAlarms) == 0)

        newAlarm = {}
        newAlarm["name"] = "alarm1"
        newAlarm["alarmUrl"] = "www1.pinterest1.com"
        newAlarm["metricsUrl"] = "www2.pinterest1.com"
        alarms = [newAlarm]
        environs_helper.update_env_alarms_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, alarms
        )
        newAlarms = environs_helper.get_env_alarms_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(alarms, newAlarms)

        newAlarm = {}
        newAlarm["name"] = "alarm2"
        newAlarm["alarmUrl"] = "www2.pinterest1.com"
        newAlarm["metricsUrl"] = "www2.pinterest1.com"
        alarms.append(newAlarm)
        environs_helper.update_env_alarms_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, alarms
        )
        newAlarms = environs_helper.get_env_alarms_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(alarms, newAlarms)

    def testMetricsConfigs(self):
        oldMetrics = environs_helper.get_env_metrics_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertTrue(len(oldMetrics) == 0)

        metrics = []
        metConfig = {}
        metSpec = {}
        metSpecs = []

        metSpec["color"] = "blue"
        metSpec["min"] = 0
        metSpec["max"] = 98
        metSpecs.append(metSpec)

        metSpec["color"] = "green"
        metSpec["min"] = 99
        metSpec["max"] = 100
        metSpecs.append(metSpec)

        metConfig["title"] = "TestTitle"
        metConfig["url"] = "www.pinterest.com"
        metConfig["specs"] = metSpecs
        metrics.append(metConfig)

        environs_helper.update_env_metrics_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, metrics
        )
        newMetrics = environs_helper.get_env_metrics_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(metrics, newMetrics)

        metSpec["color"] = "blue"
        metSpec["min"] = 1
        metSpec["max"] = 2
        metSpecs.append(metSpec)

        metConfig["title"] = "TestTitle2"
        metConfig["url"] = "www.pinterest2.com"
        metConfig["specs"] = metSpecs
        metrics.append(metConfig)

        environs_helper.update_env_metrics_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, metrics
        )
        newMetrics = environs_helper.get_env_metrics_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(metrics, newMetrics)

    def testHooksConfigs(self):
        oldHooks = environs_helper.get_env_hooks_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertTrue(oldHooks["preDeployHooks"] is None)

        newHook = {}
        hooks = {}
        newHook["method"] = "GET"
        newHook["url"] = "www1.pinterest1.com"
        newHook["version"] = "HTTP/1.1"
        newHook["headers"] = None
        newHook["body"] = None
        hooks["preDeployHooks"] = [newHook]
        hooks["postDeployHooks"] = [newHook]
        environs_helper.update_env_hooks_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, hooks
        )
        newHooks = environs_helper.get_env_hooks_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(hooks, newHooks)

        newHook = {}
        newHook["method"] = "POST"
        newHook["url"] = "www1.pinterest1.com"
        newHook["version"] = None
        newHook["headers"] = None
        newHook["body"] = None

        newHooks2 = [newHook]
        newHook["method"] = "POST"
        newHook["url"] = "www1.pinterest1.com"
        newHook["version"] = None
        newHook["headers"] = None
        newHook["body"] = None

        newHooks2.append(newHook)
        hooks["postDeployHooks"] = newHooks2
        environs_helper.update_env_hooks_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, hooks
        )
        newHooks = environs_helper.get_env_hooks_config(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )
        self.assertEqual(hooks, newHooks)

    def testScheduleConfigs(self):
        schedule = {}
        schedule["cooldownTimes"] = "20,30,40"
        schedule["hostNumbers"] = "30,50,70"
        schedule["totalSessions"] = 3
        schedules_helper.update_schedule(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, schedule
        )

        scheduleId = environs_helper.get_env_by_stage(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName
        )["scheduleId"]
        envSchedule = schedules_helper.get_schedule(
            commons.REQUEST, TestEnvirons.envName, TestEnvirons.stageName, scheduleId
        )

        newSchedule = {}
        newSchedule["cooldownTimes"] = "20,30,40"
        newSchedule["hostNumbers"] = "30,50,70"
        newSchedule["totalSessions"] = 3
        newSchedule["state"] = "NOT_STARTED"
        newSchedule["id"] = scheduleId
        newSchedule["currentSession"] = 0
        newSchedule["stateStartTime"] = envSchedule["stateStartTime"]

        self.assertEqual(newSchedule, envSchedule)


if __name__ == "__main__":
    unittest.main()
