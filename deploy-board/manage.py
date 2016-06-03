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

    #
    # OAuth based authentication settings. By default, OAuth based authentication is disabled
    # See documentation for how to enable OAuth
    #
    os.environ.setdefault("OAUTH_ENABLED", "OFF")
    #os.environ.setdefault("OAUTH_ENABLED", "ON")

    #Provider for OAUTH. Currently supports 'pinadmin' (internal) and 'googlev2'
    #os.environ.setdefault("OAUTH_PROVIDER", "<OAUTH_PROVIDER>")

    #os.environ.setdefault("OAUTH_CLIENT_ID", "<OAUTH_CLIENT_ID>")
    #os.environ.setdefault("OAUTH_CALLBACK", "<DEPLOYBOARD_URL>/auth")
    #os.environ.setdefault("OAUTH_DOMAIN", "<YOUR_DOMAIN>")
    #os.environ.setdefault("OAUTH_CLIENT_TYPE", "<CLIENT_TYPE>")
    #os.environ.setdefault("OAUTH_USER_INFO_URI", "<USER_INFO_URL>")
    #os.environ.setdefault("OAUTH_ACCESS_TOKEN_URL", "<ACCESS_TOKEN_URL>")
    #os.environ.setdefault("OAUTH_AUTHORIZE_URL", "<OAUTH_AUTH_URL>")
    #os.environ.setdefault("OAUTH_DEFAULT_SCOPE", "<DEFAULT_SCOPE>")
    #os.environ.setdefault("OAUTH_CLIENT_SECRET", "<SECRET_KEY>")    
    # Key for user response, which key is associated with the username in the response. Emails are parsed for prefix.
    #os.environ.setdefault("OAUTH_USERNAME_INFO_KEY", "<USERNAME_KEY>"")

    from django.core.management import execute_from_command_line
    execute_from_command_line(sys.argv)
