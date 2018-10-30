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

from django.http import HttpResponseRedirect
from django.shortcuts import render

from auth import OAuth
from auth import OAuthException, OAuthExpiredTokenException
from deploy_board import settings
import traceback
import logging
from common import UserIdentity

logger = logging.getLogger(__name__)


class DelegatedOAuthMiddleware(object):
    def __init__(self):
        if settings.OAUTH_ENABLED:
            self.is_oauth_enabled = True

            logger.info("clientid = %s" % settings.OAUTH_CLIENT_ID)
            self.oauth = OAuth(
                key=settings.OAUTH_CLIENT_ID,
                secret=settings.OAUTH_CLIENT_SECRET,
                callback_url=settings.OAUTH_CALLBACK,
                domain=settings.OAUTH_DOMAIN,
                access_token_url=settings.OAUTH_ACCESS_TOKEN_URL,
                authorize_url=settings.OAUTH_AUTHORIZE_URL,
                scope=settings.OAUTH_DEFAULT_SCOPE
            )
            logger.info("Successfully created OAuth!")
        else:
            logger.info("OAuth is not enabled!")
            self.is_oauth_enabled = False

    def process_request(self, request):
        if request.path.startswith('/auth/'):
            logger.debug("Bypass OAuth redirect request " + request.path)
            return None

        if request.path.startswith('/health_check/'):
            logger.debug("Bypass health_check request " + request.path)
            return None

        if not self.is_oauth_enabled:
            anonymous = UserIdentity(name="anonymous")
            request.teletraan_user_id = anonymous
            return None

        # extract employee oauth token, redirect to OAuth if missing or invalid
        if self.oauth.validate_token(session=request.session):
            username = request.session.get('teletraan_user')
            token = request.session.get('oauth_token')
            userId = UserIdentity(name=username, token=token)
            request.teletraan_user_id = userId
            return None
        else:
            # TODO call logout to remove session cleanly
            # self.logout(request)
            data = {'origin_path': request.get_full_path()}
            url = self.oauth.get_authorization_url(session=request.session, data=data)
            logger.debug("Redirect oauth for authentication!, url = " + url)
            return HttpResponseRedirect(url)


class FixedOAuthMiddleware(object):
    """
    Use test oauth credentials to talk to backend instead of getting them
    from the OAuth.
    """
    def __init__(self):
        if not settings.OAUTH_ENABLED and settings.TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN:
            self._token = settings.TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN
            logger.debug("Using fixed OAuth credentials")
        else:
            self._token = None

    def process_request(self, request):
        if self._token:
            request.teletraan_user_id = UserIdentity(name="anonymous", token=self._token)
        return None

    def logout(self, request):
        pass


def login_authorized(request):
    logger.debug("Redirect back from oauth!")
    if not settings.OAUTH_ENABLED:
        logger.error("OAuth is not enabled!")
        return HttpResponseRedirect('/')

    oauth = OAuth(
        key=settings.OAUTH_CLIENT_ID,
        secret=settings.OAUTH_CLIENT_SECRET,
        callback_url=settings.OAUTH_CALLBACK,
        domain=settings.OAUTH_DOMAIN,
        access_token_url=settings.OAUTH_ACCESS_TOKEN_URL,
        authorize_url=settings.OAUTH_AUTHORIZE_URL,
        scope=settings.OAUTH_DEFAULT_SCOPE
    )

    code = request.GET.get('code')
    state = request.GET.get('state')
    try:
        data = oauth.handle_oauth2_response(code, state, session=request.session)
        user_name = oauth.oauth_data(user_info_uri=settings.OAUTH_USER_INFO_URI, session=request.session)
        # extract user_name from oauth_data based on OAUTH_USERNAME_INFO_KEY and OAUTH_EXTRACT_USERNAME_FROM_EMAIL
        if settings.OAUTH_USERNAME_INFO_KEY:
            keys = settings.OAUTH_USERNAME_INFO_KEY.split()
            for key in keys:
                user_name = user_name[key]
        if settings.OAUTH_EXTRACT_USERNAME_FROM_EMAIL is not None and settings.OAUTH_EXTRACT_USERNAME_FROM_EMAIL == "TRUE":
            user_name = user_name.split("@")[0]

    except OAuthException as e:
        # failed to login for some reason, do something
        logger.error(traceback.format_exc())
        return render(request, 'oauth_failure.html', {
            "message": e.message,
        })

    except OAuthExpiredTokenException as e:
        # When auth.pinadmin.com returns a 401 error
        logger.error(traceback.format_exc())

        # remove access token from session cookie and redirect to / page
        # this will cause a re trigger of auth.pinadmin.com login process
        oauth.oauth_handler.token_remove(session=request.session)
        return HttpResponseRedirect("/")

    logger.debug("get user_name %s and data %s back from oauth!" % (user_name, data))
    request.session['teletraan_user'] = user_name

    if data and 'origin_path' in data:
        return HttpResponseRedirect(data['origin_path'])

    return HttpResponseRedirect('/')

def logout(request):
    logger.debug("Logout %s!" % request.session.get("teletraan_user", "anonymous"))
    if not settings.OAUTH_ENABLED:
        logger.error("OAuth is not enabled!")
        return HttpResponseRedirect('/')

    oauth = OAuth(
        key=settings.OAUTH_CLIENT_ID,
        secret=settings.OAUTH_CLIENT_SECRET,
        callback_url=settings.OAUTH_CALLBACK,
        domain=settings.OAUTH_DOMAIN,
        access_token_url=settings.OAUTH_ACCESS_TOKEN_URL,
        authorize_url=settings.OAUTH_AUTHORIZE_URL,
        scope=settings.OAUTH_DEFAULT_SCOPE
    )

    oauth.logout(session=request.session)
    if 'teletraan_user' in request.session:
        del request.session['teletraan_user']

    return HttpResponseRedirect('/loggedout/')


class PRRMiddleware:
    def __init__(self):
        pass

    def process_response(self, request, response):
        # Code to be executed for each request before
        # the view (and later middleware) are called.
        response['Pragma'] = "no-cache"
        response['Cache-Control'] = "no-cache, no-store, must-revalidate"
        response['Expires'] = "Thu, 01 Jan 1970 00:00:00 GMT;"
        response['X-Content-Type-Options'] = "nosniff"
        response['Strict-Transport-Security'] = "max-age=631138519"

        return response
