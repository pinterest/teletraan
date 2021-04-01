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

import subprocess
import traceback
import logging
import time

from future.utils import PY3

log = logging.getLogger(__name__)


class Caller(object):
    def __init__(self):
        pass

    @staticmethod
    def call_and_log(cmd, **kwargs):
        output = ""
        start = time.time()
        try:
            if PY3:
                process = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                                           stderr=subprocess.PIPE, encoding='utf-8', **kwargs)
            else:
                process = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                                           stderr=subprocess.PIPE, **kwargs)
            while process.poll() is None:
                line = process.stdout.readline()
                if line:
                    output = output + "[%.2f]" % (time.time() - start) + line
                line = process.stderr.readline()
                if line:
                    output = output + "[%.2f]" % (time.time() - start) + line
            temp, error = process.communicate()
            return output.strip(), error.strip(), process.poll()
        except Exception as e:
            log.error(traceback.format_exc())
            return None, str(e), 1
