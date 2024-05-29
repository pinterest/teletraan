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
import subprocess
import traceback
import time
import string
import random
import requests
requests.packages.urllib3.disable_warnings()


def main():
    build_path = "file://%s/%s" % (os.path.dirname(os.path.realpath(__file__)),
                                        "quickstart-build.tar.gz")
    host_info_path = "file://%s/%s" % (os.path.dirname(os.path.realpath(__file__)),
                                        "host_info")
    build_dest_dir = '/tmp/quickstart-build.tar.gz'
    host_info_dest_dir = '/tmp/deployd/host_info'
    build_download_cmd = ['curl', '-ksS', build_path, '-o', build_dest_dir]
    host_info_download_cmd = ['curl', '-ksS', host_info_path, '-o', host_info_dest_dir]

    try:
        # Publish build
        process = subprocess.Popen(build_download_cmd, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        output, error = process.communicate()
        if error:
            print("Error: failed to publish build to /tmp directory.", error)
            return

        # Make deployd directory if it doesn't yet exist
        if not os.path.exists("/tmp/deployd"):
            os.makedirs("/tmp/deployd")

        # Copy over host_info  file
        process = subprocess.Popen(host_info_download_cmd, stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        output, error = process.communicate()
        if error:
            print("Error: failed to publish host_info to /tmp directory.", error)
            return
        publish_local_build("file://%s" % build_dest_dir)
    except Exception as e:
        print(traceback.format_exc())
        return None, e.message, 1


def gen_random_num(size=8, chars=string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


def publish_local_build(build_path, build_name='deploy-sentinel', branch='master', commit=gen_random_num(32)):
    build = {}
    publish_build_url = "http://localhost:8011/v1/builds"
    headers = {'Content-type': 'application/json'}
    build['name'] = build_name
    build['repo'] = 'local'
    build['branch'] = branch
    build['commit'] = commit
    build['commitDate'] = int(round(time.time()))
    build['artifactUrl'] = build_path
    build['publishInfo'] = build_path
    r = requests.post(publish_build_url, json=build, headers=headers)
    if 200 <= r.status_code < 300:
        print("Successfully published local deploy-sentinel build and host_info " \
              "configuration file to local /tmp directory!")
    else:
        print("Error publishing local deploy-sentinel build. Status code = %s, response = %s" % (str(r.status_code),
                                                                                                 str(r.text)))
    return build


if __name__ == "__main__":
    main()
