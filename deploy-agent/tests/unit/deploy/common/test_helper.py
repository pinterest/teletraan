import getpass
import mock
import os.path
import shutil
import tests
import tempfile

from deployd.common.helper import Helper


class TestHelper(tests.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.base_dir = tempfile.mkdtemp()
        builds_dir = os.path.join(cls.base_dir, 'builds')
        target = os.path.join(cls.base_dir, 'test')
        cls.target = target
        cls.builds_dir = builds_dir
        cls.url = 'https://deployrepo.pinadmin.com/deploy-scripts/'\
                  '24714bcb0927661873c0acf34194d4f51b4da429.tar.gz'

        if not os.path.exists(builds_dir):
            os.mkdir(builds_dir)

        def mock_get_var(var_name):
            if var_name == 'builds_dir':
                return builds_dir
            elif var_name == 'env_directory':
                return cls.base_dir
            elif var_name == 'package_format':
                return 'tar.gz'

        config = mock.Mock()
        config.get_var = mock.MagicMock(side_effect=mock_get_var)
        config.get_target = mock.MagicMock(return_value=target)
        cls.helper = Helper(config)

    def setUp(self):
        os.environ['DEPLOY_ID'] = '123'
        os.environ['TARGET'] = self.target
        os.environ['BUILDS_DIR'] = self.builds_dir
        os.environ['USER_ROLE'] = getpass.getuser()

    def tearDown(self):
        del os.environ['DEPLOY_ID']
        del os.environ['TARGET']
        del os.environ['BUILDS_DIR']

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)
