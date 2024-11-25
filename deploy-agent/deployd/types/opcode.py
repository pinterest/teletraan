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
import enum


class OperationCode(enum.Enum):
    # No action needed
    NOOP = 0
    # Agent need to install new build
    DEPLOY = 1
    # Agent needs to update status file based on the new deploy goal
    UPDATE = 2
    # Agent needs to restart service
    RESTART = 3
    # Agent needs to delete its own status file for this env
    DELETE = 4
    # Host is removed from env, agent needs to delete its status file and
    # shutdown service as well
    TERMINATE = 5
    # Agent needs to sleep for certain time
    WAIT = 6
    # Agent needs to rollback
    ROLLBACK = 7
    # Agent needs to shutdown service
    STOP = 8
