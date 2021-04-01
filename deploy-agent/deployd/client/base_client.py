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

from abc import ABCMeta, abstractmethod
from future.utils import with_metaclass


class BaseClient(with_metaclass(ABCMeta, object)):
    """This class plays a role as an interface defining methods for agent to
    communicate with teletraan service.
    """
    
    @abstractmethod
    def send_reports(self, env_reports=None):
        """Args:
            env_reports: a dict with env name as key and DeployStatus as value.

        Returns:
            PingResponse describing next action for deploy agent.  
        """
        pass
