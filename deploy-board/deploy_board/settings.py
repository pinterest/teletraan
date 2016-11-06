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

"""
Django settings for deploy_board project.

For more information on this file, see
https://docs.djangoproject.com/en/1.6/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/1.6/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os
import logging

logger = logging.getLogger(__name__)

BASE_DIR = os.path.dirname(__file__)

PROJECT_PATH = BASE_DIR

TEMPLATE_DIRS = (
    os.path.join(BASE_DIR, 'templates'),
)

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = os.getenv("SECRET_KEY", None)
SESSION_ENGINE = 'django.contrib.sessions.backends.signed_cookies'
# OAuth
OAUTH_ENABLED_STR = os.getenv("OAUTH_ENABLED", "OFF")
if OAUTH_ENABLED_STR == "OFF":
    OAUTH_ENABLED = False
else:
    OAUTH_ENABLED = True
    OAUTH_CLIENT_ID = os.getenv("OAUTH_CLIENT_ID")
    OAUTH_CLIENT_SECRET = os.getenv("OAUTH_CLIENT_SECRET")
    OAUTH_CALLBACK = os.getenv("OAUTH_CALLBACK")
    OAUTH_DOMAIN = os.getenv("OAUTH_DOMAIN")
    OAUTH_CLIENT_TYPE = os.getenv("OAUTH_CLIENT_TYPE")
    OAUTH_USER_INFO_URI = os.getenv("OAUTH_USER_INFO_URI")
    OAUTH_USER_INFO_KEY = os.getenv("OAUTH_USER_INFO_KEY")
    OAUTH_ACCESS_TOKEN_URL = os.getenv("OAUTH_ACCESS_TOKEN_URL")
    OAUTH_AUTHORIZE_URL = os.getenv("OAUTH_AUTHORIZE_URL")
    OAUTH_DEFAULT_SCOPE = os.getenv("OAUTH_DEFAULT_SCOPE")
    OAUTH_USERNAME_INFO_KEY = os.getenv("OAUTH_USERNAME_INFO_KEY")

# Teletraan backend service url
TELETRAAN_SERVICE_URL = os.getenv("TELETRAAN_SERVICE_URL")
TELETRAAN_SERVICE_VERSION = os.getenv("TELETRAAN_SERVICE_VERSION")
TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN = os.getenv("TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN", None)
TELETRAAN_HOST_INFORMATION_URL = os.getenv("HOST_INFORMATION_URL")

# CMDB vars
CMDB_API_HOST = os.getenv("CMDB_API_HOST")
CMDB_INSTANCE_URL = os.getenv("CMDB_INSTANCE_URL")
CMDB_UI_HOST = os.getenv("CMDB_UI_HOST")
PHOBOS_URL = os.getenv("PHOBOS_URL")

LOG_DIR = os.getenv("LOG_DIR")
LOG_LEVEL = os.getenv("LOG_LEVEL")

# Change to your domain or hosts
if LOG_LEVEL == 'DEBUG':
    DEBUG = True
    TEMPLATE_DEBUG = True
    ALLOWED_HOSTS = []
else:
    ALLOWED_HOSTS = ['*']

LOGGING = {
    'version': 1,
    'disable_existing_loggers': True,
    'formatters': {
        'standard': {
            'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s'
        },
    },
    'handlers': {
        'default': {
            'level': LOG_LEVEL,
            'class': 'logging.handlers.RotatingFileHandler',
            'filename': '%s/service.log' % LOG_DIR,
            'maxBytes': 1024 * 1024 * 5,  # 5 MB
            'backupCount': 5,
            'formatter': 'standard',
        },
        'request_handler': {
            'level': LOG_LEVEL,
            'class': 'logging.handlers.RotatingFileHandler',
            'filename': '%s/access.log' % LOG_DIR,
            'maxBytes': 1024 * 1024 * 5,  # 5 MB
            'backupCount': 5,
            'formatter': 'standard',
        },
        'console': {
            'level': LOG_LEVEL,
            'class': 'logging.StreamHandler',
        },
    },
    'loggers': {
        '': {
            'handlers': ['default', 'console'],
            'level': LOG_LEVEL,
            'propagate': True
        },
        'django.request': {
            'handlers': ['request_handler'],
            'level': LOG_LEVEL,
            'propagate': False
        },
    }
}

# Application definition
INSTALLED_APPS = (
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
    'deploy_board.webapp',
)

oauth_middleware = 'deploy_board.webapp.security.DelegatedOAuthMiddleware'
if TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN:
    oauth_middleware = 'deploy_board.webapp.security.FixedOAuthMiddleware'

MIDDLEWARE_CLASSES = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    oauth_middleware,
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
    'deploy_board.webapp.error_views.ExceptionHandlerMiddleware',
)

TEMPLATE_CONTEXT_PROCESSORS = (
    "django.contrib.auth.context_processors.auth",
    "django.core.context_processors.debug",
    "django.core.context_processors.i18n",
    "django.core.context_processors.media",
    "django.core.context_processors.static",
    "django.contrib.messages.context_processors.messages",
    "django.core.context_processors.request",
)

ROOT_URLCONF = 'deploy_board.urls'

WSGI_APPLICATION = 'deploy_board.wsgi.application'

LANGUAGE_CODE = 'en-us'

TIME_ZONE = 'UTC'

USE_I18N = True

USE_L10N = True

USE_TZ = True

# The number of days since the build publish date required to trigger an old build version warning message
OLD_BUILD_WARNING_THRESHOLD_DAYS = 10

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.6/howto/static-files/
STATIC_URL = '/static/'
STATICFILES_DIRS = (
    os.path.join(PROJECT_PATH, "static"),
)

MEDIA_ROOT = os.path.join(BASE_DIR, 'media')
MEDIA_URL = '/media/'

# Site global metrics
SITE_METRICS_CONFIGS = []
# Deep Teletraan backend health check url
TELETRAAN_SERVICE_HEALTHCHECK_URL = os.getenv("TELETRAAN_SERVICE_HEALTHCHECK_URL", None)

# Pinterest specific settings
IS_PINTEREST = True if os.getenv("IS_PINTEREST", "false") == "true" else False
BUILD_URL = "https://jenkins.pinadmin.com/job/"

# use Rodimus if present
RODIMUS_SERVICE_URL = os.getenv("RODIMUS_SERVICE_URL", None)
RODIMUS_SERVICE_VERSION = os.getenv("RODIMUS_SERVICE_VERSION", None)

if IS_PINTEREST:
    # use knox if present
    KNOX_SESSION_ID = os.getenv("KNOX_SESSION_ID")
    if KNOX_SESSION_ID:
        from knox import Knox

        SECRET_KEY = Knox().get_primary(KNOX_SESSION_ID)

    ADMIN_OAUTH_SECRET_KNOX_ID = os.getenv("ADMIN_OAUTH_SECRET_KNOX_ID")
    if ADMIN_OAUTH_SECRET_KNOX_ID:
        from knox import Knox

        OAUTH_CLIENT_SECRET = Knox().get_primary(ADMIN_OAUTH_SECRET_KNOX_ID)

    # Site health metrics
    REQUESTS_URL = os.getenv("REQUESTS_URL")
    SUCCESS_RATE_URL = os.getenv("SUCCESS_RATE_URL")
    LATENCY_URL = os.getenv("LATENCY_URL")
    SITE_METRICS_CONFIGS = [
        {"title": "Requests", "url": REQUESTS_URL,
         "specs": [{"min": 0, "max": 50000, "color": "Red"},
                   {"min": 50000, "max": 80000, "color": "Yellow"},
                   {"min": 80000, "max": 200000, "color": "Green"}]},
        {"title": "Success", "url": SUCCESS_RATE_URL,
         "specs": [{"min": 90, "max": 98, "color": "Red"},
                   {"min": 98, "max": 99, "color": "Yellow"},
                   {"min": 99, "max": 100, "color": "Green"}]},
        {"title": "Latency", "url": LATENCY_URL,
         "specs": [{"min": 800, "max": 1000, "color": "Red"},
                   {"min": 600, "max": 800, "color": "Yellow"},
                   {"min": 300, "max": 600, "color": "Green"}]}
    ]

    # Pinterest ngapp2 status file
    NGAPP_PRE_DEPLOY_STATUS_NODE = "varnish_pre_deploy_status"
    NGAPP_POST_DEPLOY_STATUS_NODE = "varnish_post_deploy_status"
    NGAPP_ROLLBACK_STATUS_NODE = "varnish_rollback_status"
    NGAPP_DEPLOY_CHANNEL = "deploys"

    DEFAULT_START_TIME = "-1d"

    #Pinterest Default Cloud Provider
    DEFAULT_PROVIDER = 'AWS'

    #Pinterest Default AMI image name
    DEFAULT_CMP_IMAGE = 'cmp_base'

    # Pinterest default placement strategy mapping.
    PINTEREST_SECURITY_GROUP_PLACEMENTMAPPING = {
        'dev-private-service': ['dev-private-service-us-east-1a',
                                'dev-private-service-us-east-1c',
                                'dev-private-service-us-east-1d',
                                'dev-private-service-us-east-1e'],
        'dev-public-service': ['dev-public-service-us-east-1a',
                               'dev-public-service-us-east-1c',
                               'dev-public-service-us-east-1d',
                               'dev-public-service-us-east-1e'],
        'dev-private-storage': ['dev-private-service-us-east-1a',
                                'dev-private-service-us-east-1c',
                                'dev-private-service-us-east-1d',
                                'dev-private-service-us-east-1e'],
        'prod-private-service': ['prod-private-service-us-east-1a',
                                 'prod-private-service-us-east-1c',
                                 'prod-private-service-us-east-1d',
                                 'prod-private-service-us-east-1e',
                                 'prod-private-tools-us-east-1a',
                                 'prod-private-tools-us-east-1c',
                                 'prod-private-tools-us-east-1d',
                                 'prod-private-tools-us-east-1e'],
        'prod-private-tools': ['prod-private-service-us-east-1a',
                               'prod-private-service-us-east-1c',
                               'prod-private-service-us-east-1d',
                               'prod-private-service-us-east-1e',
                               'prod-private-tools-us-east-1a',
                               'prod-private-tools-us-east-1c',
                               'prod-private-tools-us-east-1d',
                               'prod-private-tools-us-east-1e'],
        'prod-public-service': ['prod-public-service-us-east-1a',
                                'prod-public-service-us-east-1c',
                                'prod-public-service-us-east-1d',
                                'prod-public-service-us-east-1e'],
        'prod-private-storage': ['prod-private-service-us-east-1a',
                                 'prod-private-service-us-east-1c',
                                 'prod-private-service-us-east-1d',
                                 'prod-private-service-us-east-1e',
                                 'prod-private-tools-us-east-1a',
                                 'prod-private-tools-us-east-1c',
                                 'prod-private-tools-us-east-1d',
                                 'prod-private-tools-us-east-1e'],
        'rtp-prod-private-service': ['prod-private-service-us-east-1a',
                                     'prod-private-service-us-east-1c',
                                     'prod-private-service-us-east-1d',
                                     'prod-private-service-us-east-1e',
                                     'prod-private-tools-us-east-1a',
                                     'prod-private-tools-us-east-1c',
                                     'prod-private-tools-us-east-1d',
                                     'prod-private-tools-us-east-1e']
    }
