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
import os.path
import shutil
import unittest
import tempfile

from deployd.staging.transformer import Transformer


class TransformTest(unittest.TestCase):
    def setUp(self):
        self.base_dir = tempfile.mkdtemp()
        self.template_dir = os.path.join(self.base_dir, "teletraan_template")
        self.script_dir = os.path.join(self.base_dir, "teletraan")
        os.mkdir(self.template_dir)
        os.mkdir(self.script_dir)

        fn1 = os.path.join(self.template_dir, "test1.tmpl")
        fn2 = os.path.join(self.template_dir, "test2.tmpl")
        with open(fn1, "w") as f:
            f.write("foo1=foo-${TELETRAAN_foo}-${TELETRAAN_foo}\n")
            f.write("foo2=foo-{$TELETRAAN_foo}-{$TELETRAAN_foo}\n")
            f.write("foo3=foo-$TELETRAAN_foo $TELETRAAN_foo\n")
            f.write("foo4=foo-${TELETRAAN_foo:foo}-${TELETRAAN_foo:foo}\n")
            f.write("foo5=foo-{$TELETRAAN_foo:foo}-{$TELETRAAN_foo:foo}\n")
            f.write("foo6=foo-${TELETRAAN_bar:foo}-${TELETRAAN_bar:foo}\n")
            f.write("foo7=foo-{$TELETRAAN_bar:foo}-{$TELETRAAN_bar:foo}\n")
            f.write("foo8=foo-${TELETRAAN_bar}-${TELETRAAN_bar:}\n")
            f.write("foo9=foo-{$TELETRAAN_bar}-{$TELETRAAN_bar:}\n")
            f.write("foo10=foo-$TELETRAAN_bar $TELETRAAN_bar\n")

        with open(fn2, "w") as f:
            f.write('$TEST="$TELETRAAN_Wh-O"')

        lines = ['foo = "bar"\n', "Wh-O =   'test2'\n", "TEST = test3\n"]
        with open(os.path.join(self.base_dir, "123_SCRIPT_CONFIG"), "w") as f:
            f.writelines(lines)

    def tearDown(self):
        shutil.rmtree(self.base_dir)

    def test_load_configs(self):
        transformer = Transformer(agent_dir=self.base_dir, env_name="123")
        self.assertEqual(transformer._dictionary.get("foo"), "bar")
        self.assertEqual(transformer._dictionary.get("Wh-O"), "test2")
        self.assertEqual(transformer._dictionary.get("TEST"), "test3")

    def test_translate1(self):
        transformer = Transformer(agent_dir=self.base_dir, env_name="123")
        transformer.transform_scripts(
            self.template_dir, self.template_dir, self.script_dir
        )

        fn1 = os.path.join(self.script_dir, "test1")
        fn2 = os.path.join(self.script_dir, "test2")

        # match console value
        value1 = "foo-bar-bar"
        # no console value, use default
        value2 = "foo-foo-foo"
        values = {
            "foo1": value1,
            "foo2": value1,
            "foo3": "foo-bar bar",
            "foo4": value1,
            "foo5": value1,
            "foo6": value2,
            "foo7": value2,
            "foo8": "foo-${TELETRAAN_bar}-",
            "foo9": "foo-{$TELETRAAN_bar}-",
            "foo10": "foo-$TELETRAAN_bar $TELETRAAN_bar",
        }

        with open(fn1, "r") as f:
            for line in f:
                key, value = line.split("=")
                newline = "%s=%s\n" % (key, values[key])
                self.assertEqual(line, newline)

        with open(fn2, "r") as f:
            s = f.read()
            self.assertEqual(s, '$TEST="test2"')

    def test_translate2(self):
        transformer = Transformer(agent_dir=self.base_dir, env_name="xyz")
        transformer.transform_scripts(
            self.template_dir, self.template_dir, self.script_dir
        )

        fn1 = os.path.join(self.script_dir, "test1")

        value1 = "foo-foo-foo"
        values = {
            "foo1": "foo-${TELETRAAN_foo}-${TELETRAAN_foo}",
            "foo2": "foo-{$TELETRAAN_foo}-{$TELETRAAN_foo}",
            "foo3": "foo-$TELETRAAN_foo $TELETRAAN_foo",
            "foo4": value1,
            "foo5": value1,
            "foo6": value1,
            "foo7": value1,
            "foo8": "foo-${TELETRAAN_bar}-",
            "foo9": "foo-{$TELETRAAN_bar}-",
            "foo10": "foo-$TELETRAAN_bar $TELETRAAN_bar",
        }

        with open(fn1, "r") as f:
            for line in f:
                key, value = line.split("=")
                newline = "%s=%s\n" % (key, values[key])
                self.assertEqual(line, newline)
