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
import time
import commons

builds_helper = commons.get_build_helper()


class TestBuilds(unittest.TestCase):
    def test_build_apis(self):
        request1 = self._generate_request("build1", "commit-1")
        request2 = self._generate_request("build2", "commit-1")
        request3 = self._generate_request("build2", "commit-2", "branch-1")
        request4 = self._generate_request("build3", "commit-3", "branch-2", "Github")

        id1 = builds_helper.publish_build(commons.REQUEST, request1)["id"]
        id2 = builds_helper.publish_build(commons.REQUEST, request2)["id"]
        id3 = builds_helper.publish_build(commons.REQUEST, request3)["id"]
        id4 = builds_helper.publish_build(commons.REQUEST, request4)["id"]

        build1 = builds_helper.get_build(commons.REQUEST, id1)
        builds = builds_helper.get_builds(
            commons.REQUEST, name="build1", branch="master", pageIndex=1, pageSize=1
        )
        build2 = builds[0]
        self.assertTrue(len(builds) == 1)
        self.assertTrue(build1 == build2)

        builds = builds_helper.get_builds(
            commons.REQUEST, name="build1", pageIndex=1, pageSize=1
        )
        build2 = builds[0]
        self.assertTrue(len(builds) == 1)
        self.assertTrue(build1 == build2)

        builds = builds_helper.get_builds(
            commons.REQUEST, commit="commit-1", pageIndex=1, pageSize=2
        )
        self.assertTrue(len(builds) == 2)

        builds = builds_helper.get_builds(
            commons.REQUEST, name="build2", pageIndex=1, pageSize=2
        )
        self.assertTrue(len(builds) == 2)

        build_names = builds_helper.get_build_names(
            commons.REQUEST, pageIndex=1, pageSize=2
        )
        self.assertTrue(len(build_names) >= 2)

        build_names = builds_helper.get_branches(commons.REQUEST, "build2")
        self.assertTrue(len(build_names) >= 2)

        builds = builds_helper.get_builds(
            commons.REQUEST, name="build3", pageIndex=1, pageSize=1
        )
        self.assertTrue(len(builds) == 1)

        builds_helper.delete_build(commons.REQUEST, id1)
        builds_helper.delete_build(commons.REQUEST, id2)
        builds_helper.delete_build(commons.REQUEST, id3)
        builds_helper.delete_build(commons.REQUEST, id4)

    def _generate_request(self, buildName, commit, branch="master", type="Phabricator"):
        request = {}
        request["name"] = buildName
        request["type"] = type
        request["repo"] = "repo-1"
        request["branch"] = branch
        request["commit"] = commit
        request["commitDate"] = int(round(time.time() * 1000))
        request["artifactUrl"] = "https://sample.com"
        request["publishInfo"] = "jenkins12345"
        return request


if __name__ == "__main__":
    unittest.main()
