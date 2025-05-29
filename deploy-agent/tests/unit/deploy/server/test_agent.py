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

import os
import unittest
from unittest import mock
from tests import TestCase

from deployd.agent import DeployAgent
from deployd.common.utils import ensure_dirs
from deployd.common.types import (
    BuildInfo,
    DeployReport,
    DeployStatus,
    OpCode,
    DeployStage,
    AgentStatus,
    PingStatus,
)
from deployd.types.deploy_goal import DeployGoal
from deployd.types.ping_report import PingReport
from deployd.types.ping_response import PingResponse


class TestDeployAgent(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.estatus = mock.Mock()
        cls.estatus.load_envs = mock.Mock(return_value=None)
        cls.config = mock.Mock()
        cls.config.load_env_and_configs = mock.Mock()
        cls.config.get_var = mock.Mock(return_value="")
        cls.config.get_intvar(return_value=1)
        cls.config.get_target = mock.Mock(return_value="/tmp/tests")
        cls.config.get_config_filename = mock.Mock(return_value="/etc/deployagent.conf")
        cls.config.get_agent_directory = mock.Mock(return_value="/tmp/deployd/")
        cls.config.get_builds_directory = mock.Mock(return_value="/tmp/deployd/builds/")
        cls.config.get_log_directory = mock.Mock(return_value="/tmp/logs/")
        cls.config.get_tsd_host = mock.Mock(return_value="localhost")
        cls.config.get_tsd_port = mock.Mock(return_value=18126)
        cls.config.get_tsd_timeout_seconds = mock.Mock(return_value=5)
        ensure_dirs(cls.config)
        cls.executor = mock.Mock()
        cls.executor.execute_command = mock.Mock(
            return_value=(DeployReport(AgentStatus.SUCCEEDED))
        )
        cls.executor.run_cmd = mock.Mock(
            return_value=(DeployReport(AgentStatus.SUCCEEDED))
        )
        cls.helper = mock.Mock()
        cls.helper.get_stale_builds = mock.Mock(return_value=[])

        build = {}
        build["id"] = "123"
        build["name"] = "abc"
        build["commitShort"] = "345"
        build["artifactUrl"] = "https://test"

        envvar = {}
        envvar["id"] = "abc"
        envvar["url"] = "https://test"

        cls.deploy_goal1 = {}
        cls.deploy_goal1["deployId"] = "123"
        cls.deploy_goal1["envName"] = "abc"
        cls.deploy_goal1["envId"] = "def"
        cls.deploy_goal1["stageName"] = "beta"
        cls.deploy_goal1["deployStage"] = DeployStage.PRE_DOWNLOAD
        cls.deploy_goal1["scriptVariables"] = envvar

        cls.deploy_goal2 = {}
        cls.deploy_goal2["deployId"] = "123"
        cls.deploy_goal2["envName"] = "abc"
        cls.deploy_goal2["envId"] = "def"
        cls.deploy_goal2["stageName"] = "beta"
        cls.deploy_goal2["deployStage"] = DeployStage.DOWNLOADING
        cls.deploy_goal2["build"] = build

        cls.deploy_goal3 = {}
        cls.deploy_goal3["deployId"] = "123"
        cls.deploy_goal3["envName"] = "abc"
        cls.deploy_goal3["envId"] = "def"
        cls.deploy_goal3["stageName"] = "beta"
        cls.deploy_goal3["deployStage"] = DeployStage.STAGING

        cls.deploy_goal4 = {}
        cls.deploy_goal4["deployId"] = "123"
        cls.deploy_goal4["envName"] = "abc"
        cls.deploy_goal4["envId"] = "def"
        cls.deploy_goal4["stageName"] = "beta"
        cls.deploy_goal4["deployStage"] = DeployStage.PRE_RESTART

        cls.deploy_goal5 = {}
        cls.deploy_goal5["deployId"] = "123"
        cls.deploy_goal5["envName"] = "abc"
        cls.deploy_goal5["envId"] = "def"
        cls.deploy_goal5["stageName"] = "beta"
        cls.deploy_goal5["deployId"] = "234"
        cls.deploy_goal5["deployStage"] = DeployStage.PRE_DOWNLOAD
        cls.deploy_goal5["build"] = build

        cls.deploy_goal6 = {}
        cls.deploy_goal6["deployId"] = "123"
        cls.deploy_goal6["envName"] = "abc"
        cls.deploy_goal6["envId"] = "def"
        cls.deploy_goal6["stageName"] = "beta"
        cls.deploy_goal6["deployId"] = "234"
        cls.deploy_goal6["deployStage"] = DeployStage.SERVING_BUILD

        cls.ping_response1 = {"deployGoal": cls.deploy_goal1, "opCode": OpCode.DEPLOY}
        cls.ping_response2 = {"deployGoal": cls.deploy_goal2, "opCode": OpCode.DEPLOY}
        cls.ping_response3 = {"deployGoal": cls.deploy_goal3, "opCode": OpCode.DEPLOY}
        cls.ping_response4 = {"deployGoal": cls.deploy_goal4, "opCode": OpCode.DEPLOY}
        cls.ping_response5 = {"deployGoal": cls.deploy_goal5, "opCode": OpCode.DELETE}
        cls.ping_response6 = {"deployGoal": cls.deploy_goal6, "opCode": OpCode.DELETE}
        cls.ping_noop_response = {"deployGoal": None, "opCode": OpCode.NOOP}

    def test_agent_first_run(self):
        # first run
        ping_response_list = [
            PingResponse(jsonValue=self.ping_response1),
            None,
            PingResponse(jsonValue=self.ping_response1),
        ]
        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)
        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        self.assertTrue(d.first_run)
        # first run stickiness
        d._envs = {"data": "data"}
        self.assertTrue(d.first_run)
        # subsequent run
        client.send_reports = mock.Mock(side_effect=ping_response_list)
        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        d._envs = {"data": "data"}
        self.assertFalse(d.first_run)

    def test_agent_status_on_ping_failure(self):
        ping_response_list = [
            PingResponse(jsonValue=self.ping_response1),
            None,
            PingResponse(jsonValue=self.ping_response1),
        ]
        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)

        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        self.assertEqual(
            PingStatus.PLAN_CHANGED,
            d.update_deploy_status(DeployReport(status_code=AgentStatus.SUCCEEDED)),
        )
        self.assertEqual(
            PingStatus.PING_FAILED,
            d.update_deploy_status(DeployReport(status_code=AgentStatus.SUCCEEDED)),
        )
        self.assertEqual(
            PingStatus.PLAN_NO_CHANGE,
            d.update_deploy_status(DeployReport(status_code=AgentStatus.SUCCEEDED)),
        )

    def test_agent_with_switch_command(self):
        ping_response_list = [
            PingResponse(jsonValue=self.ping_response1),
            PingResponse(jsonValue=self.ping_response2),
            PingResponse(jsonValue=self.ping_response3),
            PingResponse(jsonValue=self.ping_response4),
            PingResponse(jsonValue=self.ping_response5),
            PingResponse(jsonValue=self.ping_response6),
            PingResponse(jsonValue=self.ping_noop_response),
        ]

        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)

        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        d.serve_build()

        calls = [
            mock.call(
                [
                    "deploy-downloader",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-u",
                    "https://test",
                    "-e",
                    "abc",
                ]
            ),
            mock.call(
                [
                    "deploy-stager",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-t",
                    "/tmp/tests",
                    "-e",
                    "abc",
                ]
            ),
        ]
        self.executor.run_cmd.assert_has_calls(calls)
        self.assertEqual(len(d._envs), 0)

    def test_agent_with_switch_goal(self):
        build = {}
        build["id"] = "123"
        build["name"] = "bar"
        build["commitShort"] = "345"
        build["commit"] = "abcd"
        build["artifactUrl"] = "https://test"

        build2 = {}
        build2["id"] = "123"
        build2["name"] = "fool"
        build2["commit"] = "abcd"
        build2["commitShort"] = "345"
        build2["artifactUrl"] = "https://test2"

        envvar = {}
        envvar["id"] = "abc"
        envvar["url"] = "https://test"

        envvar2 = {}
        envvar2["id"] = "bcd"
        envvar2["url"] = "https://test2"

        ping_response1 = self.ping_response1
        ping_response1["deployGoal"]["scriptVariables"] = envvar

        ping_response2 = self.ping_response2
        ping_response2["deployGoal"]["build"] = build

        ping_response3 = self.ping_response3
        ping_response4 = self.ping_response4

        deploy_goal5 = {}
        deploy_goal5["deployId"] = "234"
        deploy_goal5["envName"] = "bcd"
        deploy_goal5["envId"] = "efg"
        deploy_goal5["stageName"] = "prod"
        deploy_goal5["deployStage"] = DeployStage.PRE_DOWNLOAD
        deploy_goal5["scriptVariables"] = envvar2

        deploy_goal6 = {}
        deploy_goal6["deployId"] = "234"
        deploy_goal6["envName"] = "bcd"
        deploy_goal6["envId"] = "efg"
        deploy_goal6["stageName"] = "prod"
        deploy_goal6["deployStage"] = DeployStage.DOWNLOADING
        deploy_goal6["build"] = build2

        deploy_goal7 = {}
        deploy_goal7["deployId"] = "234"
        deploy_goal7["envName"] = "bcd"
        deploy_goal7["envId"] = "efg"
        deploy_goal7["stageName"] = "prod"
        deploy_goal7["deployStage"] = DeployStage.STAGING

        deploy_goal8 = {}
        deploy_goal8["deployId"] = "234"
        deploy_goal8["envName"] = "bcd"
        deploy_goal8["envId"] = "efg"
        deploy_goal8["stageName"] = "prod"
        deploy_goal8["deployStage"] = DeployStage.RESTARTING

        deploy_goal9 = {}
        deploy_goal9["deployId"] = "234"
        deploy_goal9["envName"] = "bcd"
        deploy_goal9["envId"] = "efg"
        deploy_goal9["stageName"] = "prod"
        deploy_goal9["deployStage"] = DeployStage.POST_RESTART

        deploy_goal10 = {}
        deploy_goal10["deployId"] = "234"
        deploy_goal10["envName"] = "bcd"
        deploy_goal10["envId"] = "efg"
        deploy_goal10["stageName"] = "prod"
        deploy_goal10["deployStage"] = DeployStage.SERVING_BUILD

        ping_response5 = {"deployGoal": deploy_goal5, "opCode": OpCode.DEPLOY}
        ping_response6 = {"deployGoal": deploy_goal6, "opCode": OpCode.DEPLOY}
        ping_response7 = {"deployGoal": deploy_goal7, "opCode": OpCode.DEPLOY}
        ping_response8 = {"deployGoal": deploy_goal8, "opCode": OpCode.DEPLOY}
        ping_response9 = {"deployGoal": deploy_goal9, "opCode": OpCode.DEPLOY}
        ping_response10 = {"deployGoal": deploy_goal10, "opCode": OpCode.DEPLOY}

        ping_response_list = [
            PingResponse(jsonValue=ping_response1),
            PingResponse(jsonValue=ping_response2),
            PingResponse(jsonValue=ping_response3),
            PingResponse(jsonValue=ping_response4),
            PingResponse(jsonValue=ping_response5),
            PingResponse(jsonValue=ping_response6),
            PingResponse(jsonValue=ping_response7),
            PingResponse(jsonValue=ping_response8),
            PingResponse(jsonValue=ping_response9),
            PingResponse(jsonValue=ping_response10),
            PingResponse(jsonValue=self.ping_noop_response),
        ]

        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)
        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        d.serve_build()
        calls = [
            mock.call(
                [
                    "deploy-downloader",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-u",
                    "https://test",
                    "-e",
                    "abc",
                ]
            ),
            mock.call(
                [
                    "deploy-stager",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-t",
                    "/tmp/tests",
                    "-e",
                    "abc",
                ]
            ),
            mock.call(
                [
                    "deploy-downloader",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-u",
                    "https://test2",
                    "-e",
                    "bcd",
                ]
            ),
            mock.call(
                [
                    "deploy-stager",
                    "-f",
                    "/etc/deployagent.conf",
                    "-v",
                    "123",
                    "-t",
                    "/tmp/tests",
                    "-e",
                    "bcd",
                ]
            ),
        ]
        self.executor.run_cmd.assert_has_calls(calls)
        self.assertEqual(len(d._envs), 2)
        self.assertEqual(d._envs["abc"].report.deployStage, DeployStage.PRE_RESTART)
        self.assertEqual(d._envs["abc"].report.deployId, "123")
        self.assertEqual(d._envs["abc"].report.envId, "def")
        self.assertEqual(d._envs["abc"].report.status, AgentStatus.SUCCEEDED)
        self.assertEqual(d._envs["abc"].build_info.build_commit, "abcd")
        self.assertEqual(d._envs["abc"].build_info.build_url, "https://test")

        self.assertEqual(d._envs["bcd"].report.deployStage, DeployStage.SERVING_BUILD)
        self.assertEqual(d._envs["bcd"].report.deployId, "234")
        self.assertEqual(d._envs["bcd"].report.envId, "efg")
        self.assertEqual(d._envs["bcd"].report.status, AgentStatus.SUCCEEDED)
        self.assertEqual(d._envs["bcd"].build_info.build_commit, "abcd")
        self.assertEqual(d._envs["bcd"].build_info.build_url, "https://test2")

    def test_delete_report(self):
        status = DeployStatus()
        ping_report = {}
        ping_report["deployId"] = "123"
        ping_report["envId"] = "234"
        ping_report["envName"] = "abc"
        ping_report["stageName"] = "beta"
        ping_report["deployStage"] = DeployStage.SERVING_BUILD
        ping_report["status"] = AgentStatus.SUCCEEDED
        status.report = PingReport(jsonValue=ping_report)

        envs = {"abc": status}
        client = mock.Mock()
        estatus = mock.Mock()
        estatus.load_envs = mock.Mock(return_value=envs)
        deploy_goal = {}
        deploy_goal["deployId"] = "123"
        deploy_goal["envId"] = "234"
        deploy_goal["envName"] = "abc"
        deploy_goal["stageName"] = "beta"
        ping_response = {"deployGoal": deploy_goal, "opCode": OpCode.DELETE}

        responses = [
            PingResponse(jsonValue=ping_response),
            PingResponse(jsonValue=self.ping_noop_response),
        ]
        client.send_reports = mock.Mock(side_effect=responses)
        agent = DeployAgent(
            client=client,
            estatus=estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        agent.serve_build()
        calls = [mock.call(envs), mock.call({})]
        client.send_reports.assert_has_calls(calls)
        self.assertIsNone(agent._curr_report)
        self.assertEqual(agent._envs, {})

    def test_init_report(self):
        if os.path.exists("/tmp/env_status"):
            os.remove("/tmp/env_status")

        client = mock.Mock()
        client.send_reports = mock.Mock(
            return_value=(PingResponse(jsonValue=self.ping_noop_response))
        )
        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        d.serve_build()
        client.send_reports.assert_called_once_with({})

    @mock.patch("socket.socket")
    @mock.patch("time.time")
    def test_report_health(self, mock_time, mock_socket):
        mock_time.return_value = 100.1
        mock_sock = mock.Mock()
        mock_socket.return_value = mock_sock

        status = DeployStatus()
        ping_report = {}
        ping_report["deployId"] = "123"
        ping_report["envId"] = "234"
        ping_report["envName"] = "abc"
        ping_report["stageName"] = "beta"
        ping_report["deployStage"] = DeployStage.SERVING_BUILD
        ping_report["status"] = AgentStatus.SUCCEEDED
        status.report = PingReport(jsonValue=ping_report)
        status.build_info = BuildInfo(
            "test_commit_sha", "test_build_url", "test_build_id"
        )

        envs = {"234": status}
        client = mock.Mock()
        estatus = mock.Mock()
        estatus.load_envs = mock.Mock(return_value=envs)
        client.send_reports = mock.Mock(
            return_value=PingResponse(jsonValue=self.ping_noop_response)
        )
        agent = DeployAgent(
            client=client,
            estatus=estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        agent.serve_build()
        client.send_reports.assert_called_once_with(envs)
        self.assertEqual(agent._curr_report.report.envId, "234")
        self.assertEqual(
            agent._curr_report.report.deployStage, DeployStage.SERVING_BUILD
        )
        self.assertEqual(agent._curr_report.report.status, AgentStatus.SUCCEEDED)

        mock_socket.assert_called_once()
        mock_sock.settimeout.assert_called_once_with(5)
        mock_sock.connect.assert_called_once_with(("localhost", 18126))
        expected_put = "put deploy.info 100 1 source=teletraan artifact=234 commit_sha=test_commit_sha\n"
        mock_sock.sendall.assert_called_once_with(expected_put.encode("utf-8"))

    def test_report_with_deploy_goal(self):
        if os.path.exists("/tmp/env_status"):
            os.remove("/tmp/env_status")

        build = {}
        build["id"] = "123"
        build["url"] = "https://test"
        client = mock.Mock()
        deploy_goal = {}
        deploy_goal["deployId"] = "123"
        deploy_goal["envName"] = "456"
        deploy_goal["envId"] = "789"
        deploy_goal["stageName"] = "beta"
        deploy_goal["deployStage"] = DeployStage.PRE_DOWNLOAD
        deploy_goal["scriptVariables"] = build
        ping_response = {"deployGoal": deploy_goal, "opCode": OpCode.DEPLOY}

        responses = [
            PingResponse(jsonValue=ping_response),
            PingResponse(jsonValue=self.ping_noop_response),
        ]
        client.send_reports = mock.Mock(side_effect=responses)
        agent = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        agent.process_deploy = mock.Mock(
            return_value=(DeployReport(AgentStatus.SUCCEEDED))
        )
        agent.serve_build()
        self.assertEqual(agent._curr_report.report.envId, "789")
        self.assertEqual(
            agent._curr_report.report.deployStage, DeployStage.PRE_DOWNLOAD
        )
        self.assertEqual(len(agent._envs), 1)

    def test_set_and_get_deploy_status(self):
        envvar = {}
        envvar["id"] = "bar"
        envvar["url"] = "https://abc-123.tar.gz"

        build = {}
        build["id"] = "123"
        build["name"] = "bar"
        build["commitShort"] = "234"
        build["artifactUrl"] = "https://abc-123.tar.gz"

        ping_response1 = self.ping_response1
        ping_response1["deployGoal"]["scriptVariables"] = envvar

        ping_response2 = self.ping_response2
        ping_response2["deployGoal"]["build"] = build

        deploy_goal5 = {}
        deploy_goal5["deployId"] = "123"
        deploy_goal5["envName"] = "abc"
        deploy_goal5["envId"] = "def"
        deploy_goal5["stageName"] = "beta"
        deploy_goal5["deployStage"] = DeployStage.RESTARTING

        deploy_goal6 = {}
        deploy_goal6["deployId"] = "123"
        deploy_goal6["envName"] = "abc"
        deploy_goal6["envId"] = "def"
        deploy_goal6["stageName"] = "beta"
        deploy_goal6["deployStage"] = DeployStage.POST_RESTART

        deploy_goal7 = {}
        deploy_goal7["deployId"] = "123"
        deploy_goal7["envName"] = "abc"
        deploy_goal7["envId"] = "def"
        deploy_goal7["stageName"] = "beta"
        deploy_goal7["deployStage"] = DeployStage.SERVING_BUILD

        ping_response5 = {"deployGoal": deploy_goal5, "opCode": OpCode.DEPLOY}
        ping_response6 = {"deployGoal": deploy_goal6, "opCode": OpCode.DEPLOY}
        ping_response7 = {"deployGoal": deploy_goal7, "opCode": OpCode.DEPLOY}

        ping_response_list = [
            PingResponse(jsonValue=ping_response1),
            PingResponse(jsonValue=ping_response2),
            PingResponse(jsonValue=self.ping_response3),
            PingResponse(jsonValue=self.ping_response4),
            PingResponse(jsonValue=ping_response5),
            PingResponse(jsonValue=ping_response6),
            PingResponse(jsonValue=ping_response7),
            PingResponse(jsonValue=self.ping_noop_response),
        ]

        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)
        d = DeployAgent(
            client=client,
            estatus=self.estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        d.serve_build()
        calls = [
            mock.call(stage)
            for stage in ["PRE_DOWNLOAD", "PRE_RESTART", "RESTARTING", "POST_RESTART"]
        ]
        self.executor.execute_command.assert_has_calls(calls)
        self.assertEqual(len(d._envs), 1)
        self.assertEqual(d._curr_report.report.envId, "def")
        self.assertEqual(d._curr_report.report.envName, "abc")
        self.assertEqual(d._curr_report.report.deployId, "123")
        self.assertEqual(d._curr_report.report.stageName, "beta")
        self.assertEqual(d._curr_report.report.deployStage, DeployStage.SERVING_BUILD)

    def test_plan_change(self):
        old_response = None
        new_response = None
        self.assertFalse(DeployAgent.plan_changed(old_response, new_response))
        new_response = PingResponse()
        self.assertTrue(DeployAgent.plan_changed(old_response, new_response))
        old_response = PingResponse()
        old_response.opCode = OpCode.DEPLOY
        new_response.opCode = OpCode.NOOP
        self.assertTrue(DeployAgent.plan_changed(old_response, new_response))
        new_response.opCode = OpCode.DEPLOY
        self.assertFalse(DeployAgent.plan_changed(old_response, new_response))

        deploy_goal = {}
        deploy_goal["deployId"] = "123"
        deploy_goal2 = {}
        deploy_goal2["deployId"] = "234"
        old_response.deployGoal = DeployGoal(jsonValue=deploy_goal)
        new_response.deployGoal = DeployGoal(jsonValue=deploy_goal2)
        self.assertTrue(DeployAgent.plan_changed(old_response, new_response))
        new_response.deployGoal.deployId = "123"
        new_response.deployGoal.deployStage = DeployStage.PRE_RESTART
        old_response.deployGoal.deployStage = DeployStage.PRE_RESTART
        self.assertFalse(DeployAgent.plan_changed(old_response, new_response))

    def test_switch_goal_download_variable_failed(self):
        pass

    @mock.patch("deployd.agent.create_sc_increment")
    def test_send_deploy_status(self, mock_create_sc):
        status = DeployStatus()
        status.report = PingReport(jsonValue=self.deploy_goal1)

        envs = {"abc": status}
        estatus = mock.Mock()
        estatus.load_envs = mock.Mock(return_value=envs)
        ping_response_list = [
            PingResponse(jsonValue=self.ping_response1),
            PingResponse(jsonValue=self.ping_noop_response),
        ]
        mock_create_sc.return_value = None

        client = mock.Mock()
        client.send_reports = mock.Mock(side_effect=ping_response_list)
        agent = DeployAgent(
            client=client,
            estatus=estatus,
            conf=self.config,
            executor=self.executor,
            helper=self.helper,
        )
        agent.serve_build()
        mock_create_sc.assert_called_once_with(
            "deployd.stats.deploy.status.sum",
            tags={
                "first_run": False,
                "deploy_stage": "PRE_DOWNLOAD",
                "env_name": "abc",
                "stage_name": "beta",
                "status_code": "SUCCEEDED",
            },
        )
        self.assertEqual(
            agent._curr_report.report.deployStage, DeployStage.PRE_DOWNLOAD
        )
        self.assertEqual(agent._curr_report.report.status, AgentStatus.SUCCEEDED)


if __name__ == "__main__":
    unittest.main()
