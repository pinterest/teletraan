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
# -*- coding: utf-8 -*-
''' Publish some random builds
'''
import commons


def main():
    for x in range(1, 3):
        # deploy requires build name to be the same as env stage configuration's build name
        buildName = "sample-service-{}".format(x)
        commons.publish_build(buildName, "master")
#    for x in xrange(1):
#        commons.publish_build("sample-build", "hotfix")


if __name__ == "__main__":
    main()
