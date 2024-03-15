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

environs_helper = commons.get_environ_helper()


def main():
    for x in range(1, 3):
        name = "sample-service-%d" % x
        stage = "canary"
        commons.create_env(name, stage)
        hosts = ["sample-host1", "sample-host2"]
        environs_helper.update_env_capacity(commons.REQUEST, name, stage,
                                            capacity_type="HOST", data=hosts)
        stage = "prod"
        commons.create_env(name, stage)
        groups = ["sample-group1", "sample-group2"]
        environs_helper.update_env_capacity(commons.REQUEST, name, stage, data=groups)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(e.message)
