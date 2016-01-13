import abc
import hashlib
import logging

log = logging.getLogger(__name__)


class DownloadHelper(object):

    def __init__(self, url):
        self._url = url

    @staticmethod
    def hash_file(file_path):
        sha = hashlib.sha1()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                sha.update(chunk)
        return sha.hexdigest()

    @staticmethod
    def md5_file(file_path):
        md5 = hashlib.md5()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                md5.update(chunk)
        return md5.hexdigest()

    @abc.abstractproperty
    def download(self, local_full_fn):
        pass
