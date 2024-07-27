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
import commons

builds_helper = commons.get_build_helper()
deploys_helper = commons.get_deploy_helper()


def pick_build_id(buildName, x):
    builds = builds_helper.get_builds(commons.REQUEST, name=buildName, pageIndex=1, pageSize=x)
    return builds[x - 1]['id']


def create(name, stage, x):
    buildName = "sample-service-{}".format(x)
    buildId = pick_build_id(buildName, 0)
    id = deploys_helper.deploy(commons.REQUEST, name, stage, buildId)['id']
    print("Successfully created deploy %s" % id)


def main():
    for x in range(1, 3):
        name = "sample-service-%d" % x
        stage = "canary"
        create(name, stage, x)
        stage = "prod"
        create(name, stage, x)


if __name__ == "__main__":
    main()
