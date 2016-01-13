from deployd.download.local_download_helper import LocalDownloadHelper
import os
import shutil
import io
import tempfile
import unittest
import logging

logger = logging.getLogger()
logger.level = logging.DEBUG


class LocalDownloadFunctionsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.url = "file://%s%s" % (os.path.dirname(os.path.realpath(__file__)), "/test.txt")
        cls.base_dir = tempfile.mkdtemp()
        builds_dir = os.path.join(cls.base_dir, 'teletraan')
        cls.builds_dir = builds_dir
        if not os.path.exists(builds_dir):
            os.mkdir(builds_dir)

        target = os.path.join(builds_dir, 'test.txt')
        cls.target = target

    def test_download_local(self):
        log_capture_string = io.BytesIO()
        stream_handler = logging.StreamHandler(log_capture_string)
        logger.addHandler(stream_handler)
        try:
            downloader = LocalDownloadHelper(self.url)
            downloader.download(self.target)
            log_contents = log_capture_string.getvalue()
            assert('Failed' in log_contents, False)
        finally:
            log_capture_string.close()
            logger.removeHandler(stream_handler)

    @classmethod
    def tearDownClass(cls):
        shutil.rmtree(cls.base_dir)

if __name__ == '__main__':
    unittest.main()
