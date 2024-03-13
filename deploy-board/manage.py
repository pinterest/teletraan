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

#!/usr/bin/env python
import os
import sys

if __name__ == "__main__":
    os.environ.setdefault("DJANGO_SETTINGS_MODULE", "deploy_board.settings")

    #
    # Session key. change it to use real long secure key in production
    #
    os.environ.setdefault("SECRET_KEY", "CHANGEME")

    #
    # Backend Teletraan service url and settings. Change it accordingly in prod
    #

    os.environ.setdefault("TELETRAAN_SERVICE_URL", "http://localhost:8080")
    os.environ.setdefault("TELETRAAN_SERVICE_VERSION", "v1")

    #
    # Logging
    #
    os.environ.setdefault("LOG_DIR", "/tmp/deploy_board")
    os.environ.setdefault("LOG_LEVEL", "DEBUG")
    os.environ.setdefault("DEBUG_MODE", "ON")
    #
    # OAuth based authentication settings. By default, OAuth based authentication is disabled
    #
    os.environ.setdefault("OAUTH_ENABLED", "OFF")

    # Uncomment the following to turn on oauth authentication. Make sure to enable authentication,
    # and possibly authorization on the backend java Teletraan service as well. See Teletraan doc for more details.
    # See also https://developers.google.com/identity/protocols/OpenIDConnect for more open id authentication
    #os.environ.setdefault("OAUTH_ENABLED", "ON")

    # The client ID that you obtain from the provider
    #os.environ.setdefault("OAUTH_CLIENT_ID", 'REPLACEME.apps.googleusercontent.com')
    # The client secret that you obtain from the provider
    #os.environ.setdefault("OAUTH_CLIENT_SECRET", 'REPLACEME')
    # The URI that you specified when register with the provider. Providers will redirect browsers to
    # this URI after successful authentication
    #os.environ.setdefault("OAUTH_CALLBACK", 'http://localhost:8888/auth')
    # The domain that you specified when register with the provider
    #os.environ.setdefault("OAUTH_DOMAIN", 'REPLACEME.com')
    # Provider token endpoint
    #os.environ.setdefault("OAUTH_ACCESS_TOKEN_URL", "https://www.googleapis.com/oauth2/v4/token")
    # Provider authentication endpoint
    #os.environ.setdefault("OAUTH_AUTHORIZE_URL", "https://accounts.google.com/o/oauth2/auth")
    # Space-separated authentication scopes
    #os.environ.setdefault("OAUTH_DEFAULT_SCOPE", "email profile")
    # Provider user information endpoint
    #os.environ.setdefault("OAUTH_USER_INFO_URI", 'https://www.googleapis.com/oauth2/v3/userinfo')
    # The name of the field in the returned userinfo map which value will be used as username.
    # If the field is inside a nested userinfo map, use space-separated field names, e.g. "user email"
    #os.environ.setdefault("OAUTH_USERNAME_INFO_KEY", "email")
    # If username is email address, use the part before @ as the final username
    #os.environ.setdefault("OAUTH_EXTRACT_USERNAME_FROM_EMAIL", 'TRUE')

    # If true, display STOPPING and STOPPED hosts on the environment status page
    os.environ.setdefault("DISPLAY_STOPPING_HOSTS", "true")

    from django.core.management import execute_from_command_line
    execute_from_command_line(sys.argv)
