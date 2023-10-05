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

import logging
import traceback
import json
from django.shortcuts import render
from django.http import HttpResponse, HttpResponseRedirect
from helpers.exceptions import NotAuthorizedException, FailedAuthenticationException

logger = logging.getLogger(__name__)


# TODO so we are using this as the catch ALL, and report error, as the last resort
# this is fine, except the exception stack trace is not particularly user-friendly
# We should not depends on this too much, but in the code handle as much exceptino
# as we can and generate user friendly message there.
class ExceptionHandlerMiddleware(object):
    def process_exception(self, request, exception):
        logger.exception('Exception thrown when handling request ' + str(request))

        # Error is displayed as a fragment over related feature area
        if request.is_ajax():
            ajax_vars = {'success': False, 'error': exception.message}
            return HttpResponse(json.dumps(ajax_vars), content_type='application/javascript')
        else:
            # Not authorized
            if isinstance(exception, NotAuthorizedException):
                return render(request, 'users/not_authorized.html', {
                    "message": exception.message,
                })

            elif isinstance(exception, FailedAuthenticationException):
                request.session.modified = True
                request.session.flush()
                return HttpResponseRedirect("/")

            return render(request, 'error.html', {
                'message': exception.message,
                'stacktrace': traceback.format_exc(),
            })
