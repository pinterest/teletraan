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
https://docs.djangoproject.com/en/1.11/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/1.11/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os
import logging

logger = logging.getLogger(__name__)

BASE_DIR = os.path.dirname(__file__)

PROJECT_PATH = BASE_DIR

TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS' : [os.path.join(BASE_DIR, 'templates')],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.contrib.auth.context_processors.auth',
            ],
        },
    },
]

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
    OAUTH_EXTRACT_USERNAME_FROM_EMAIL = os.getenv("OAUTH_EXTRACT_USERNAME_FROM_EMAIL")

# Teletraan backend service url
TELETRAAN_SERVICE_URL = os.getenv("TELETRAAN_SERVICE_URL")
TELETRAAN_SERVICE_VERSION = os.getenv("TELETRAAN_SERVICE_VERSION")
TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN = os.getenv("TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN", None)
TELETRAAN_HOST_INFORMATION_URL = os.getenv("HOST_INFORMATION_URL")

# CMDB vars
CMDB_API_HOST = os.getenv("CMDB_API_HOST", "http://localhost:8080/")
CMDB_INSTANCE_URL = os.getenv("CMDB_INSTANCE_URL", "api/cmdb/getinstance/")
CMDB_UI_HOST = os.getenv("CMDB_UI_HOST", "localhost")
PHOBOS_URL = os.getenv("PHOBOS_URL")

# Serviceframework add-on vars
SERVICE_RATELIMIT_CONFIG_URL = os.getenv("SERVICE_RATELIMIT_CONFIG_URL")
STATSBOARD_API_FORMAT = os.getenv("STATSBOARD_API_FORMAT", "OFF")
RATELIMIT_ENABLED_METRIC_FORMAT = os.getenv("RATELIMIT_ENABLED_METRIC_FORMAT", "OFF")
ENABLING_SERVICE_RATELIMIT_URL = os.getenv("ENABLING_SERVICE_RATELIMIT_URL", "OFF")
KAFKA_MSGS_DELIVERED_METRIC = os.getenv("KAFKA_MSGS_DELIVERED_METRIC", "OFF")
DASHBOARD_URL_ENDPOINT_FORMAT = os.getenv("DASHBOARD_URL_ENDPOINT_FORMAT","OFF")

# For rolling out new features
GUINEA_PIG_ENVS = os.getenv("GUINEA_PIG_ENVS", "").split(",")
KAFKA_LOGGING_ADD_ON_ENVS = os.getenv("KAFKA_LOGGING_ADD_ON_ENVS", "").split(",")

LOG_DIR = os.getenv("LOG_DIR")
LOG_LEVEL = os.getenv("LOG_LEVEL")

# Change to your domain or hosts
if LOG_LEVEL == 'DEBUG':
    DEBUG = True
    TEMPLATE_DEBUG = True
    ALLOWED_HOSTS = ['*']
else:
    ALLOWED_HOSTS = ['*']

LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
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
            'formatter': 'standard',
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
    'csp.middleware.CSPMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    oauth_middleware,
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
    'deploy_board.webapp.error_views.ExceptionHandlerMiddleware',
    'deploy_board.webapp.security.PRRMiddleware'
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
# https://docs.djangoproject.com/en/1.11/howto/static-files/
STATIC_URL = '/static/'
STATICFILES_DIRS = (
    os.path.join(PROJECT_PATH, "static"),
)
#STATIC_ROOT = os.path.join(PROJECT_PATH, 'static')

MEDIA_ROOT = os.path.join(BASE_DIR, 'media')
MEDIA_URL = '/media/'

# Site global metrics
SITE_METRICS_CONFIGS = []
# Deep Teletraan backend health check url
TELETRAAN_SERVICE_HEALTHCHECK_URL = os.getenv("TELETRAAN_SERVICE_HEALTHCHECK_URL", None)

# Show hosts that are STOPPING or STOPPED in the environments page
DISPLAY_STOPPING_HOSTS = os.getenv("DISPLAY_STOPPING_HOSTS", "true")

# Pinterest specific settings
IS_PINTEREST = True if os.getenv("IS_PINTEREST", "false") == "true" else False
BUILD_URL = os.getenv("BUILD_URL", None)
USER_DATA_CONFIG_SETTINGS_WIKI = os.getenv("USER_DATA_CONFIG_SETTINGS_WIKI", None)

TELETRAAN_DISABLE_CREATE_ENV_PAGE = True if os.getenv("TELETRAAN_DISABLE_CREATE_ENV_PAGE", "false") == "true" else False
TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL = os.getenv("TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL", None)
IS_DURING_CODE_FREEZE = True if os.getenv("TELETRAAN_CODE_FREEZE", "false") == "true" else False
TELETRAAN_CODE_FREEZE_URL = os.getenv("TELETRAAN_CODE_FREEZE_URL", None)
TELETRAAN_JIRA_SOURCE_URL = os.getenv("TELETRAAN_JIRA_SOURCE_URL", None)
TELETRAAN_TRANSFER_OWNERSHIP_URL = os.getenv("TELETRAAN_TRANSFER_OWNERSHIP_URL", None)
TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL = os.getenv("TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL", None)
TELETRAAN_PROJECT_URL_FORMAT = os.getenv("TELETRAAN_PROJECT_URL_FORMAT", None)

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

    DEFAULT_START_TIME = "-1d"

    #Pinterest Default Cloud Provider
    DEFAULT_PROVIDER = 'AWS'

    #Pinterest Default AMI image name
    DEFAULT_CMP_IMAGE = 'cmp_base-ebs-18.04'

    #Pinterest Default Host Type
    DEFAULT_CMP_HOST_TYPE = 'EbsComputeLo(Recommended)'

    DEFAULT_CELL = 'aws-us-east-1'
    DEFAULT_PLACEMENT = os.getenv('DEFAULT_CMP_PLACEMENT')

    #Pinterest Default Puppet Environment
    DEFAULT_CMP_PINFO_ENVIRON = os.getenv('DEFAULT_CMP_PINFO_ENVIRON')
    DEFAULT_CMP_ACCESS_ROLE = os.getenv('DEFAULT_CMP_ACCESS_ROLE')

    #CSP Config
    CSP_SCRIPT_SRC = ("'self'", "https://*.gstatic.com/ https://cdn.jsdelivr.net/ https://www.google.com/ 'unsafe-inline' 'unsafe-eval'")
    CSP_DEFAULT_SRC = ("'self'")
    CSP_CONNECT_SRC = ("'self'")
    CSP_EXCLUDE_URL_PREFIXES = ('/api-docs',)
    CSP_STYLE_SRC = ("'self'", "https://*.gstatic.com/ https://cdn.jsdelivr.net/ 'unsafe-inline'")

    # Nimbus service url
    NIMBUS_SERVICE_URL = os.getenv("NIMBUS_SERVICE_URL", None)
    NIMBUS_EGRESS_URL = os.getenv("NIMBUS_EGRESS_URL", None)
    NIMBUS_USE_EGRESS = (os.getenv("NIMBUS_USE_EGRESS", 'False').lower() == 'true')
    NIMBUS_SERVICE_VERSION = os.getenv("NIMBUS_SERVICE_VERSION", None)

    DEFAULT_CLUSTER_TYPE = "PRODUCTION"
