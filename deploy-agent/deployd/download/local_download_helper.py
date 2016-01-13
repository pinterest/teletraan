from deployd.common.caller import Caller
from deployd.download.download_helper import DownloadHelper
from deployd.common.status_code import Status
import logging
import os

log = logging.getLogger(__name__)


class LocalDownloadHelper(DownloadHelper):

    def _download_files(self, local_full_fn):
        download_cmd = ['curl', '-o', local_full_fn, '-ks', self._url]
        log.info('Running command: {}'.format(' '.join(download_cmd)))
        error_code = Status.SUCCEEDED
        output, error, status = Caller.call_and_log(download_cmd, cwd=os.getcwd())
        if output:
            log.info(output)
        if error:
            log.error(error)
        if status:
            error_code = Status.FAILED
        log.info('Finished downloading: {} to {}'.format(self._url, local_full_fn))
        return error_code

    def download(self, local_full_fn):
        log.info("Start to download from local path {} to {}".format(
            self._url, local_full_fn))

        error = self._download_files(local_full_fn)
        if error != Status.SUCCEEDED:
            log.error('Failed to download the local tar ball for {}'.format(local_full_fn))
        return error
