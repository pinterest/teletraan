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

import glob
import logging
import os
import shutil
from typing import Generator, List, Optional, Tuple

log = logging.getLogger(__name__)


class Helper(object):

    def __init__(self, config=None) -> None:
        self._config = config

    def builds_available_locally(self, builds_dir, env_name) -> List:
        """Returns a list of (build, timestamp) that we have installed."""
        builds = []
        try:
            for filename in os.listdir(builds_dir):
                path = os.path.join(builds_dir, filename)
                if os.path.isfile(path):
                    #Only check downloaded file
                    found, build = Helper.get_build_id(filename, env_name)
                    #Builds are downloaded as env_name-build_id.tar.gz
                    if found:  
                        # We only care about the actual builds.
                        builds.append((build, os.path.getmtime(path)))
        except OSError:
            # if builds_dir doesn't exist, there is no local build,
            # go on and return empty list.
            log.debug("OSError: {} does not exist.".format(builds_dir))
        finally:
            return builds
        
    @staticmethod
    def get_build_name(filename) -> str:
        """
        Extract build name from the file name
        In downloader.py, we have the following name convenion
             local_fn = u'{}-{}.{}'.format(self._build_name, self._build, extension)
        """
        fn_without_extension = filename.split('.')[0]
        return fn_without_extension.rsplit('-', 1)[0]

    @staticmethod
    def get_build_id(filename, env_name) -> Tuple[bool, Optional[str]]:
        """
        Extract build id from the file name
        In downloader.py, we have the following name convenion
             local_fn = u'{}-{}.{}'.format(self._build_name, self._build, extension)
        """
        prefix = "{0}-".format(env_name)
       
        if filename.startswith(prefix) and "." in filename:
            return True, filename[len(prefix):filename.index(".")]
        return False, None

    @staticmethod
    def get_stale_builds(build_timestamps, num_builds_to_retain=2) -> Generator:
        """
        Given a list of (build, timestamp) tuples, determine which are stale.

        :param num_builds_to_retain: number of builds to keep at a minimum.

        """
        # Sorted by timestamp, oldest first.
        sorted_items = sorted(build_timestamps, key=lambda bt: bt[1])
        total_builds = len(sorted_items)
        yielded_builds = 0

        for item in sorted_items:
            if total_builds - yielded_builds <= num_builds_to_retain:
                break
            build, timestamp = item

            yield build
            yielded_builds += 1

    @staticmethod
    def clean_package(base_dir, build, build_name) -> None:
        """
           Clean a package:
           :param base_dir: builds dir
                  build: build id
                  build_name: environment name
        """
        local_fn = '{}-{}.*'.format(build_name, build)
        try:
            # Remove extracted pointer from disk
            extracted_file = os.path.join(base_dir, '{}.extracted'.format(build))
            if os.path.exists(extracted_file):
                os.remove(extracted_file)
            # Remove staged pointer from disk
            staged_file = os.path.join(base_dir, '{}.staged'.format(build))
            if os.path.exists(staged_file):
                os.remove(staged_file)
        except OSError:
            log.exception("Failed: remove old pointer file from disk")

        try:
            # Remove build directory from disk
            shutil.rmtree(os.path.join(base_dir, build))
        except BaseException:
            # Catch base exception class, as there's a multitude of reasons a rmtree can fail
            log.exception("Failed: remove build directory from disk")

        try:
            # Remove archive from disk
            fns = glob.glob(os.path.join(base_dir, local_fn))
            if fns:
                os.remove(fns[0])
        except OSError:
            log.exception("Failed: remove build archive from disk")
