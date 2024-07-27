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
import logging
from deployd.common.status_code import Status
from deployd.common.caller import Caller

log = logging.getLogger(__name__)

class gpgHelper(object):

    @staticmethod
    def decryptFile(source, destination) -> int:
        download_cmd = ['gpg', '--batch', '--yes', '--output', destination, '--decrypt', source]
        log.info('Running command: {}'.format(' '.join(download_cmd)))
        error_code = Status.SUCCEEDED
        output, error, status = Caller.call_and_log(download_cmd, cwd=os.getcwd())
        if output:
            log.info(output)
        if error:
            log.error(error)
            error_code = Status.FAILED
        if status:
            error_code = Status.FAILED
        log.info('Finish decrypting: {} to {}'.format(source, destination))
        return error_code
        
