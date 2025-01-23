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
import tempfile
import unittest
import tests

from deployd.common.types import DeployStatus, BuildInfo, DeployStage, AgentStatus
from deployd.common.env_status import EnvStatus
from deployd.types.ping_report import PingReport


class TestStatusFunction(tests.TestCase):
    def test_load_dump_file(self):
        fn = tempfile.mkstemp()[1]
        env_status = EnvStatus(fn)
        status1 = DeployStatus()
        ping_report = {}
        ping_report["deployId"] = "deploy1"
        ping_report["envId"] = "envId1"
        ping_report["envName"] = "env1"
        ping_report["stageName"] = "beta"
        ping_report["deployStage"] = DeployStage.POST_RESTART
        ping_report["status"] = AgentStatus.AGENT_FAILED
        ping_report["errorCode"] = 1
        ping_report["errorMessage"] = "Fail to open files"
        status1.report = PingReport(jsonValue=ping_report)
        status1.build_info = BuildInfo(
            commit="abc", build_url="http://google.com", build_id="234"
        )

        status2 = DeployStatus()
        ping_report = {}
        ping_report["deployId"] = "deploy2"
        ping_report["envId"] = "envId2"
        ping_report["envName"] = "env2"
        ping_report["stageName"] = "prod"
        ping_report["deployStage"] = DeployStage.SERVING_BUILD
        ping_report["status"] = AgentStatus.SUCCEEDED
        status2.report = PingReport(jsonValue=ping_report)
        status2.build_info = BuildInfo(
            commit="bcd", build_url="http://pinterest.com", build_id="234"
        )
        envs = {"env1": status1, "env2": status2}
        env_status.dump_envs(envs)
        envs2 = env_status.load_envs()

        self.assertEqual(envs["env1"].report.status, envs2["env1"].report.status)
        self.assertEqual(
            envs["env1"].report.errorMessage, envs2["env1"].report.errorMessage
        )
        self.assertEqual(
            envs["env1"].build_info.build_commit, envs2["env1"].build_info.build_commit
        )
        self.assertEqual(
            envs["env2"].report.deployStage, envs2["env2"].report.deployStage
        )
        self.assertEqual(
            envs["env2"].build_info.build_url, envs2["env2"].build_info.build_url
        )
        os.remove(fn)

    def test_load_non_exist_file(self):
        fn = tempfile.mkstemp()[1]
        env_status = EnvStatus(fn)
        envs = env_status.load_envs()
        self.assertEqual(envs, {})

    def test_bad_format(self):
        fn = tempfile.mkstemp()[1]
        contents = '{  "env1": {    "deployId": "1",    "deployStage": 3,'

        with open(fn, "w") as f:
            f.write(contents)

        env_status = EnvStatus(fn)
        envs = env_status.load_envs()
        self.assertEqual(envs, {})
        os.remove(fn)


if __name__ == "__main__":
    unittest.main()
