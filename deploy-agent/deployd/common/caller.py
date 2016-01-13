import subprocess
import traceback
import logging

log = logging.getLogger(__name__)


class Caller(object):
    def __init__(self):
        pass

    @staticmethod
    def call_and_log(cmd, **kwargs):
        try:
            process = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                                       stderr=subprocess.PIPE, **kwargs)
            output, error = process.communicate()
            return output.strip(), error.strip(), process.poll()
        except Exception as e:
            log.error(traceback.format_exc())
            return None, e.message, 1
