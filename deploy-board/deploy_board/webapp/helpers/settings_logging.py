import json
import logging

from django.http import HttpRequest


class StructuredMessage(logging.Formatter):
    def formatException(self, exc_info):
        """
        format an exception so that it prints on a single line.
        return: str
        """
        result = super(StructuredMessage, self).formatException(exc_info)
        return repr(result)

    @staticmethod
    def to_str(obj):
        return "{}".format(obj)

    def format(self, record):
        """
        param: record: class 'logging.LogRecord'
        return: json as str
        """
        # call super for asctime
        super(StructuredMessage, self).format(record)
        attrs = ['asctime', 'created', 'exc_info', 'exc_text', 'filename',
                 'funcName', 'levelname', 'levelno', 'lineno', 'message', 'module',
                 'msecs', 'msg', 'name', 'pathname', 'process', 'processName',
                 'relativeCreated', 'thread', 'threadName']
        logs = dict()
        # attrs vary and can sometimes contain non-serializable data
        for attr in attrs:
            if hasattr(record, attr):
                logs[attr] = self.to_str(getattr(record, attr))
        logs["message"] = self.to_str(record.getMessage())
        dumps = json.dumps(logs)
        return dumps


class RequestJsonFormatter(logging.Formatter):
    def format(self, record):
        """
        param: record: class 'logging.LogRecord'
        return: json as str
        """
        print(record)
        if hasattr(record, 'request') and record.request is not None:
            request = record.request
            user = request.teletraan_user_id
            log_message = {
                "user": user.name,
                "path": request.path,
                "method": request.method,
                "time": super().formatTime(record),
            }
            return json.dumps(log_message)

        return super().format(record)
