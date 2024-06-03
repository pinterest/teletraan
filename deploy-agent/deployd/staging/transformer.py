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
import traceback
from string import Template
import re
import logging
from typing import List
from deployd import IS_PINTEREST

log = logging.getLogger(__name__)


class TeletraanTemplate(Template):
    delimiter = '$TELETRAAN_'
    idpattern = r'[a-zA-Z][_a-z0-9A-Z\-]*'


class Transformer(object):

    def __init__(self, agent_dir, env_name, dict_fn=None) -> None:
        self._agent_dir = agent_dir
        self._env_name = env_name
        self._load_config(dict_fn)

    def _load_config(self, fn) -> None:
        if not fn:
            fn = os.path.join(self._agent_dir, "{}_SCRIPT_CONFIG".format(self._env_name))

        if not os.path.isfile(fn):
            self._dictionary = {}
            return

        with open(fn, 'r') as f:
            self._dictionary = dict((n.strip('\"\n\' ') for n in line.split("=", 1)) for line in f)

    def dict_size(self) -> int:
        return len(self._dictionary)

    def _translate(self, from_path, to_path) -> None:
        try:
            with open(from_path, 'r') as f:
                res = f.read()

            matcher = "(\{\$|\$\{)TELETRAAN_(?P<KEY>[a-zA-Z0-9\-_]+)(?P<COLON>:)?(?P<VALUE>.*?)\}"
            match_string = re.finditer(matcher, res)
            for match in match_string:
                key = match.group("KEY")
                colon = match.group("COLON")
                value = match.group("VALUE")
                if colon is None:
                    value = None
                value = self._dictionary.get(key, value)
                if value is not None:
                    res = res.replace(match.group(), value)

            s = TeletraanTemplate(res)
            res = s.safe_substitute(self._dictionary)
            with open(to_path, 'w') as f:
                f.write(res)
        except Exception:
            log.error('Fail to translate script {}, stacktrace: {}'.format(from_path,
                                                                           traceback.format_exc()))

    def transform_scripts(self, script_dir, template_dirname, script_dirname) -> List:
        scripts = []
        suffix = ".tmpl"
        try:
            for root, dirs, files in os.walk(script_dir):
                for filename in files:
                    if IS_PINTEREST or filename.endswith(suffix):
                        from_path = os.path.join(root, filename)
                        to_path = from_path.replace(template_dirname, script_dirname)
                        to_path = to_path.replace(suffix, "")

                        if os.path.isfile(from_path):  # We only care about files
                            self._translate(from_path, to_path)
                            log.info('finish translating: {} to {}'.format(from_path, to_path))
        except OSError:
            # if scripts_dir doesn't exist, there is no local build,
            # go on and return empty list.
            log.debug("OSError: {} does not exist.".format(script_dir))
        finally:
            return scripts
