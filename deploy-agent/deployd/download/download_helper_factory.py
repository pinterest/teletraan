from boto.s3.connection import S3Connection
from deployd.download.s3_download_helper import S3DownloadHelper
from deployd.download.http_download_helper import HTTPDownloadHelper
from deployd.download.local_download_helper import LocalDownloadHelper
from deployd.common.config import Config
from urlparse import urlparse
import logging

log = logging.getLogger(__name__)


class DownloadHelperFactory(object):

    @staticmethod
    def gen_downloader(url):
        url_parse = urlparse(url)
        if url_parse.scheme == 's3':
            config = Config()
            aws_access_key_id = config.get_aws_access_key()
            aws_secret_access_key = config.get_aws_access_secret()
            if aws_access_key_id is None or aws_secret_access_key is None:
                log.error("aws access key id and secret access key not found")
                return None
            aws_conn = S3Connection(aws_access_key_id, aws_secret_access_key, True)
            return S3DownloadHelper(url, aws_conn)
        elif url_parse.scheme == 'file':
            return LocalDownloadHelper(url)
        else:
            return HTTPDownloadHelper(url)
