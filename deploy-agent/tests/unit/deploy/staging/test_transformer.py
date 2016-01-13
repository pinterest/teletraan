import os
import os.path
import shutil
import unittest
import tempfile

from deployd.staging.transformer import Transformer


class TestHelper(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.base_dir = tempfile.mkdtemp()
        cls.template_dir = os.path.join(cls.base_dir, 'teletraan_template')
        cls.script_dir = os.path.join(cls.base_dir, 'teletraan')
        os.mkdir(cls.template_dir)
        os.mkdir(cls.script_dir)

        fn1 = os.path.join(cls.template_dir, 'test1.tmpl')
        fn2 = os.path.join(cls.template_dir, 'test2.tmpl')
        with open(fn1, 'w') as f:
            f.write('$TELETRAAN_who is $TELETRAAN_mike.')

        with open(fn2, 'w') as f:
            f.write('$TEST="$TELETRAAN_Wh-O"')

        lines = ['who = \"test1\"\n',
                 'Wh-O =   \'test2\'\n',
                 'TEST = test3\n']
        with open(os.path.join(cls.base_dir, '123_SCRIPT_CONFIG'), 'w') as f:
            f.writelines(lines)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)

    def test_load_configs(self):
        transformer = Transformer(agent_dir=self.base_dir, env_name="123")
        self.assertEqual(transformer._dictionary.get('who'), 'test1')
        self.assertEqual(transformer._dictionary.get('Wh-O'), 'test2')
        self.assertEqual(transformer._dictionary.get('TEST'), 'test3')

    def test_translate(self):
        transformer = Transformer(agent_dir=self.base_dir, env_name="123")
        transformer.transform_scripts(self.template_dir, self.template_dir, self.script_dir)

        fn1 = os.path.join(self.script_dir, 'test1')
        fn2 = os.path.join(self.script_dir, 'test2')

        with open(fn1, 'r') as f:
            s = f.read()
            self.assertEqual(s, 'test1 is $TELETRAAN_mike.')

        with open(fn2, 'r') as f:
            s = f.read()
            self.assertEqual(s, '$TEST="test2"')
