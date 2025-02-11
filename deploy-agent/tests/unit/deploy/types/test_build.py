# Copyright 2022 Pinterest, Inc.
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

from deployd.types.build import Build
import unittest


class TestBuild(unittest.TestCase):
    # buildId
    j_buildId = "id"
    buildId = "buildId"
    # buildName
    j_buildName = "name"
    buildName = "buildName"
    # buildVersion
    j_buildVersion = "commitShort"
    buildVersion = "buildVersion"
    # artifactUrl
    j_artifactUrl = "artifactUrl"
    artifactUrl = "artifactUrl"
    # scm
    j_scm = "type"
    scm = "scm"
    # scmRepo
    j_scmRepo = "repo"
    scmRepo = "scmRepo"
    # scmBranch
    j_scmBranch = "branch"
    scmBranch = "scmBranch"
    # scmCommit
    j_scmCommit = "commit"
    scmCommit = "scmCommit"
    # scmInfo
    j_scmInfo = "commitInfo"
    scmInfo = "scmInfo"
    # commitDate
    j_commitDate = "commitDate"
    commitDate = "commitDate"
    # publishInfo
    j_publishInfo = "publishInfo"
    publishInfo = "publishInfo"
    # publishDate
    j_publishDate = "publishDate"
    publishDate = "publishDate"

    def test__no_values(self):
        build = Build(jsonValue=None)
        self.assertIsNone(build.buildId)
        self.assertIsNone(build.buildName)
        self.assertIsNone(build.buildVersion)
        self.assertIsNone(build.artifactUrl)
        self.assertIsNone(build.scm)
        self.assertIsNone(build.scmRepo)
        self.assertIsNone(build.scmCommit)
        self.assertIsNone(build.scmInfo)
        self.assertIsNone(build.commitDate)
        self.assertIsNone(build.publishInfo)
        self.assertIsNone(build.publishDate)

    def test__values(self):
        data = {
            self.j_buildId: self.buildId,
            self.j_buildName: self.buildName,
            self.j_buildVersion: self.buildVersion,
            self.j_artifactUrl: self.artifactUrl,
            self.j_scm: self.scm,
            self.j_scmRepo: self.scmRepo,
            self.j_scmBranch: self.scmBranch,
            self.j_scmCommit: self.scmCommit,
            self.j_scmInfo: self.scmInfo,
            self.j_commitDate: self.commitDate,
            self.j_publishInfo: self.publishInfo,
            self.j_publishDate: self.publishDate,
        }
        build = Build(jsonValue=data)
        self.assertEqual(self.buildId, build.buildId)
        self.assertEqual(self.buildName, build.buildName)
        self.assertEqual(self.buildVersion, build.buildVersion)
        self.assertEqual(self.artifactUrl, build.artifactUrl)
        self.assertEqual(self.scm, build.scm)
        self.assertEqual(self.scmBranch, build.scmBranch)
        self.assertEqual(self.scmCommit, build.scmCommit)
        self.assertEqual(self.scmInfo, build.scmInfo)
        self.assertEqual(self.commitDate, build.commitDate)

    def test____eq__(self):
        data_1 = {
            self.j_buildId: self.buildId,
            self.j_buildName: self.buildName,
            self.j_buildVersion: self.buildVersion,
            self.j_artifactUrl: self.artifactUrl,
            self.j_scm: self.scm,
            self.j_scmBranch: self.scmBranch,
            self.j_scmCommit: self.scmCommit,
            self.j_scmInfo: self.scmInfo,
            self.j_commitDate: self.commitDate,
            self.j_publishInfo: self.publishInfo,
            self.j_publishDate: self.publishDate,
        }

        build_1 = Build(jsonValue=data_1)
        data_2 = {
            self.j_buildId: self.buildId,
            self.j_buildName: self.buildName,
            self.j_buildVersion: self.buildVersion,
            self.j_artifactUrl: self.artifactUrl,
            self.j_scm: self.scm,
            self.j_scmBranch: self.scmBranch,
            self.j_scmCommit: self.scmCommit,
            self.j_scmInfo: self.scmInfo,
            self.j_commitDate: self.commitDate,
            self.j_publishInfo: self.publishInfo,
            self.j_publishDate: self.publishDate,
        }
        build_2 = Build(jsonValue=data_2)
        self.assertEqual(build_1, build_2)
        self.assertTrue(Build(jsonValue=None) == Build(jsonValue=None))
        self.assertFalse(Build(jsonValue=None) is None)

    def test____ne__(self):
        data = {
            self.j_buildId: self.buildId,
            self.j_buildName: self.buildName,
            self.j_buildVersion: self.buildVersion,
            self.j_artifactUrl: self.artifactUrl,
            self.j_scm: self.scm,
            self.j_scmBranch: self.scmBranch,
            self.j_scmCommit: self.scmCommit,
            self.j_scmInfo: self.scmInfo,
            self.j_commitDate: self.commitDate,
            self.j_publishInfo: self.publishInfo,
            self.j_publishDate: self.publishDate,
        }
        build = Build(jsonValue=data)
        other = "other"
        self.assertNotEqual(build, Build(jsonValue={self.j_buildId: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_buildName: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_buildVersion: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_artifactUrl: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_scm: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_scmBranch: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_scmCommit: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_scmInfo: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_commitDate: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_publishInfo: other}))
        self.assertNotEqual(build, Build(jsonValue={self.j_publishDate: other}))
        self.assertTrue(Build(jsonValue=None) is not None)
        self.assertFalse(Build(jsonValue=None) == "")
