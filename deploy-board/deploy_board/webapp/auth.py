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

import json
import logging
import oauthlib
from oauthlib.common import add_params_to_uri
import oauthlib.oauth2
import random
import time
import base64

try:
    import urllib2 as http
except ImportError:
    from urllib import request as http

log = logging.getLogger("oauth")
STATE_LENGTH = 32
# Default with Google endpoints
OAUTH_ACCESS_TOKEN_URL = "https://auth.pinadmin.com/oauth/token/"
OAUTH_AUTHORIZE_URL = "https://auth.pinadmin.com/oauth/authorize/"
DEFAULT_SCOPE = "user"

logger = logging.getLogger(__name__)

try:
    random = random.SystemRandom()
except NotImplementedError:
    log.error("No system level randomness available. PRNG in software is not secure.")


def get_random_string(
    length=12,
    allowed_chars="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
):
    """
    Returns a securely generated random string.

    The default length of 12 with the a-z, A-Z, 0-9 character set returns
    a 71-bit value. log_2((26+26+10)^12) =~ 71 bits

    Derived from django.utils.crypto

    Warning: Should only be used with systemrandom. PRNG is predictable
    """
    return "".join(random.choice(allowed_chars) for i in range(length))


def is_equal(a, b):
    """Constant time string compare"""
    if len(a) != len(b):
        return False

    result = 0
    for x, y in zip(a, b):
        result |= ord(x) ^ ord(y)
    return result == 0


class OAuthException(Exception):
    pass


class OAuthExpiredTokenException(Exception):
    """
    Used to raise an exception when the access token has expired and auth.pinadmin.com returns 401 error code
    """

    pass


class OAuthHandler(object):
    def token_getter(self, **kwargs):
        raise NotImplementedError

    def token_setter(self, token, expires, **kwargs):
        raise NotImplementedError

    def state_generator(self, **kwargs):
        raise NotImplementedError

    def state_getter(self, **kwargs):
        raise NotImplementedError

    def state_setter(self, value, **kwargs):
        raise NotImplementedError

    def state_remove(self, **kwargs):
        raise NotImplementedError

    def token_remove(self, **kwargs):
        raise NotImplementedError


class SessionOauthHandler(OAuthHandler):
    def __init__(
        self,
        state_length=32,
        state_name="oauth_state",
        token_name="oauth_token",
        expires_name="oauth_expires",
    ):
        self.state_length = state_length
        self.state_name = state_name
        self.token_name = token_name
        self.expires_name = expires_name

    def state_getter(self, session, **kwargs):
        state = session.get(self.state_name)
        return state

    def state_setter(self, value, session, **kwargs):
        session[self.state_name] = value

    def state_generator(self, **kwargs):
        return get_random_string(self.state_length)

    def token_setter(self, token, expires, session, **kwargs):
        session[self.token_name] = token
        session[self.expires_name] = expires

    def token_getter(self, session, **kwargs):
        return session.get(self.token_name), session.get(self.expires_name)

    def state_remove(self, session, **kwargs):
        if self.state_name in session:
            del session[self.state_name]

    def token_remove(self, session, **kwargs):
        if self.expires_name in session:
            del session[self.expires_name]
        if self.token_name in session:
            del session[self.token_name]


class OAuth(object):
    """Communicates with the oauth providers"""

    def __init__(
        self,
        secret,
        key,
        callback_url,
        domain,
        access_token_url=OAUTH_ACCESS_TOKEN_URL,
        authorize_url=OAUTH_AUTHORIZE_URL,
        oauth_handler=SessionOauthHandler(),
        scope=DEFAULT_SCOPE,
    ):
        self.access_token_url = (
            access_token_url if access_token_url else OAUTH_ACCESS_TOKEN_URL
        )
        self.secret = secret
        self.key = key
        self.authorize_url = authorize_url if authorize_url else OAUTH_AUTHORIZE_URL
        self.callback_url = callback_url
        self.oauth_handler = oauth_handler
        self.scope = scope if scope else DEFAULT_SCOPE
        self.domain = domain

    def get_client(self, token=None):
        if token and isinstance(token, (tuple, list)):
            token = {"access_token": token[0]}
        client = oauthlib.oauth2.WebApplicationClient(self.key, token=token)
        return client

    def validate_token(self, **kwargs):
        token, expires = self.oauth_handler.token_getter(**kwargs)
        return token and expires and expires > time.time()

    def api_get(self, url, data=None, **kwargs):
        method = "GET"
        client = self.get_client(self.oauth_handler.token_getter(**kwargs))

        uri, headers, body = client.add_token(url, http_method=method, body=data)
        resp, content = self.http_request(
            uri, data=str(body) if body else None, method=method, headers=headers
        )

        resp_data = json.loads(content)
        return resp_data

    def handle_oauth2_response(self, code, state_with_data, **kwargs):
        old_state = self.oauth_handler.state_getter(**kwargs)
        self.oauth_handler.state_remove(**kwargs)

        if old_state is None:
            raise OAuthException("Invalid state")

        state, enc_data = (
            state_with_data[: len(old_state)],
            state_with_data[len(old_state) :],
        )
        if not is_equal(old_state, state):
            raise OAuthException("Invalid state")

        args = {
            "code": code,
            "client_secret": self.secret,
            "redirect_uri": self.callback_url,
        }
        client = self.get_client()
        body = client.prepare_request_body(**args)
        resp, content = self.http_request(
            self.access_token_url,
            data=str(body) if body else None,
            method="POST",
        )
        if resp.code == 401:
            # When auth.pinadmin.com returns a 401 error. remove token and redirect to / page
            raise OAuthExpiredTokenException("Expired Token")

        if resp.code not in (200, 201):
            raise OAuthException("Invalid OAuth response")
        try:
            resp_data = json.loads(content.decode())
        except ValueError:
            raise OAuthException("Invalid OAuth response")

        expires = time.time() + resp_data["expires_in"]
        self.oauth_handler.token_setter(resp_data["access_token"], expires, **kwargs)

        try:
            return json.loads(base64.b64decode(enc_data).decode())
        except ValueError:
            return None

    def get_authorization_url(self, data=None, **kwargs):
        client = self.get_client()
        scope = str(self.scope)

        # generate and set state
        state = self.oauth_handler.state_generator(**kwargs)
        self.oauth_handler.state_setter(state, **kwargs)

        # hack to add data to state
        encoded_data = base64.b64encode(json.dumps(data).encode("utf-8"))
        state_with_data = state + encoded_data.decode("utf-8")
        return client.prepare_request_uri(
            self.authorize_url,
            redirect_uri=self.callback_url,
            scope=scope,
            state=state_with_data,
            hd=self.domain,
        )

    @staticmethod
    def http_request(uri, headers=None, data=None, method=None):
        if headers is None:
            headers = {}

        if data and not method:
            method = "POST"
        elif not method:
            method = "GET"

        if method == "GET" and data:
            uri = add_params_to_uri(uri, data)
            data = None

        log.debug("Request %r with %r method" % (uri, method))
        data_encoded = data.encode("utf-8") if data else None
        req = http.Request(uri, headers=headers, data=data_encoded)
        req.get_method = lambda: method.upper()
        try:
            resp = http.urlopen(req)
            content = resp.read()
            resp.close()
            return resp, content
        except http.HTTPError as resp:
            content = resp.read()
            resp.close()
            return resp, content

    def oauth_data(self, user_info_uri, **kwargs):
        try:
            return self.api_get(user_info_uri, **kwargs)
        except Exception:
            raise OAuthException

    def logout(self, **kwargs):
        self.oauth_handler.token_remove(**kwargs)
