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
from deploy_board.webapp.helpers import environs_helper
from commons import create_env, REQUEST

if __name__ == "__main__":
    for x in range(1, 3):
        name = f"sample-service-{x}"
        stage = "canary"
        try:
            create_env(name, stage)
            hosts = ["sample-host1", "sample-host2"]
            environs_helper.update_env_capacity(REQUEST, name, stage,
                                                capacity_type="HOST", data=hosts)
            stage = "prod"
            create_env(name, stage)
            groups = ["sample-group1", "sample-group2"]
            environs_helper.update_env_capacity(REQUEST, name, stage, data=groups)
        except Exception as e:
            print(e)
