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

#!/usr/bin/env python

import json
import time
from ngapptools.ext.zk import ZK

counter = 0
total = 10

zk = ZK()


def write_data(name, data2):
    data = {}
    data["default"] = data2["default"]
    data["versions"] = data2["versions"]
    data["all-versions"] = data2["versions"]
    data["upgrade-to"] = data["default"]
    zk._safe_set("/builds2/ngapp2/varnish-control/now-serving/{}".format(name), json.dumps(data))


for i in range(total):
    zk.zk.ensure_path("/builds2/ngapp2/varnish-control/now-serving/{}".format(i))

data2 = json.loads(zk.safe_get("/builds2/ngapp2/varnish-control/ok-to-serve"))

while counter < total:
    write_data(str(counter), data2)
    counter += 1
    print('Set varnish ok-to-serve for {} times...'.format(counter))
    time.sleep(2)
