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
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [os.path.join(BASE_DIR, "templates")],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = os.getenv("SECRET_KEY", None)
SESSION_ENGINE = "django.contrib.sessions.backends.signed_cookies"
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
TELETRAAN_SERVICE_PROXY_HTTP = os.getenv("TELETRAAN_SERVICE_PROXY_HTTP", None)
TELETRAAN_SERVICE_PROXY_HTTPS = os.getenv("TELETRAAN_SERVICE_PROXY_HTTPS", None)
TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN = os.getenv(
    "TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN", None
)
TELETRAAN_HOST_INFORMATION_URL = os.getenv("HOST_INFORMATION_URL")
TELETRAAN_SERVICE_USE_BEARER = (
    os.getenv("TELETRAAN_SERVICE_USE_BEARER", "false").lower() == "true"
)  # default is False

# CMDB vars
CMDB_API_HOST = os.getenv("CMDB_API_HOST", "http://localhost:8080/")
CMDB_INSTANCE_URL = os.getenv("CMDB_INSTANCE_URL", "api/cmdb/getinstance/")
CMDB_UI_HOST = os.getenv("CMDB_UI_HOST", "localhost")
PHOBOS_URL = os.getenv("PHOBOS_URL")

# Serviceframework add-on vars
SERVICE_RATELIMIT_CONFIG_URL = os.getenv("SERVICE_RATELIMIT_CONFIG_URL")
STATSBOARD_API_FORMAT = os.getenv("STATSBOARD_API_FORMAT", "OFF")
STATSBOARD_API_PREFIX = os.getenv("STATSBOARD_API_PREFIX", "https://")
STATSBOARD_HOST_TYPE_API_FORMAT = os.getenv("STATSBOARD_HOST_TYPE_API_FORMAT", "OFF")
STATSBOARD_HUB_URL_ENDPOINT_FORMAT = os.getenv(
    "STATSBOARD_HUB_URL_ENDPOINT_FORMAT", "OFF"
)
RATELIMIT_ENABLED_METRIC_FORMAT = os.getenv("RATELIMIT_ENABLED_METRIC_FORMAT", "OFF")
ENABLING_SERVICE_RATELIMIT_URL = os.getenv("ENABLING_SERVICE_RATELIMIT_URL", "OFF")
KAFKA_MSGS_DELIVERED_METRIC = os.getenv("KAFKA_MSGS_DELIVERED_METRIC", "OFF")

# For rolling out new features
GUINEA_PIG_ENVS = os.getenv("GUINEA_PIG_ENVS", "").split(",")
KAFKA_LOGGING_ADD_ON_ENVS = os.getenv("KAFKA_LOGGING_ADD_ON_ENVS", "").split(",")
PUPPET_CONFIG_REPOSITORY = os.getenv("PUPPET_CONFIG_REPOSITORY")
PUPPET_HIERA_PATHS = os.getenv("PUPPET_HIERA_PATHS")
CONFLICTING_DEPLOY_SERVICE_WIKI_URL = os.getenv(
    "CONFLICTING_DEPLOY_SERVICE_WIKI_URL", "http://localhost:8080"
)

LOG_DIR = os.getenv("LOG_DIR")
LOG_LEVEL = os.getenv("LOG_LEVEL")
DEBUG_MODE = os.getenv("DEBUG_MODE")

# Change to your domain or hosts
DEBUG = DEBUG_MODE == "ON"

if LOG_LEVEL == "DEBUG":
    TEMPLATE_DEBUG = True
    ALLOWED_HOSTS = ["*"]
else:
    ALLOWED_HOSTS = ["*"]

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "standard": {"format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s"},
        "json": {
            "()": "deploy_board.webapp.helpers.settings_logging.StructuredMessage",
            "format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        },
        "audit_log_formatter": {
            "()": "deploy_board.webapp.helpers.settings_logging.RequestJsonFormatter",
            "format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        },
    },
    "handlers": {
        "default": {
            "level": LOG_LEVEL,
            "class": "concurrent_log_handler.ConcurrentRotatingFileHandler",
            "filename": "%s/service.log" % LOG_DIR,
            "maxBytes": 1024 * 1024 * 5,  # 5 MB
            "backupCount": 5,
            "formatter": "json",
        },
        "django.request": {
            "level": LOG_LEVEL,
            "class": "concurrent_log_handler.ConcurrentRotatingFileHandler",
            "filename": "%s/request.log" % LOG_DIR,
            "maxBytes": 1024 * 1024 * 5,  # 5 MB
            "backupCount": 5,
            "formatter": "json",
        },
        "django.server": {
            "level": LOG_LEVEL,
            "class": "concurrent_log_handler.ConcurrentRotatingFileHandler",
            "filename": "%s/server.log" % LOG_DIR,
            "maxBytes": 1024 * 1024 * 5,  # 5 MB
            "backupCount": 5,
            "formatter": "json",
        },
        "django.template": {
            "level": LOG_LEVEL,
            "class": "concurrent_log_handler.ConcurrentRotatingFileHandler",
            "filename": "%s/template.log" % LOG_DIR,
            "maxBytes": 1024 * 1024 * 5,  # 5 MB
            "backupCount": 5,
            "formatter": "json",
        },
        "audit_log_handler": {
            "level": LOG_LEVEL,
            "class": "concurrent_log_handler.ConcurrentRotatingFileHandler",
            "filename": "%s/audit.log" % LOG_DIR,
            "maxBytes": 1024 * 1024 * 5,  # 5 MB
            "backupCount": 5,
            "formatter": "audit_log_formatter",
        },
        "console": {
            "level": LOG_LEVEL,
            "class": "logging.StreamHandler",
            "formatter": "standard",
        },
    },
    "loggers": {
        "": {"handlers": ["default", "console"], "level": LOG_LEVEL, "propagate": True},
        "django.request": {
            "handlers": ["django.request"],
            "level": LOG_LEVEL,
            "propagate": False,
        },
        "requests.packages.urllib3.connectionpool": {
            "handlers": ["django.request"],
            "level": LOG_LEVEL,
            "propagate": False,
        },
        "django.server": {
            "handlers": ["django.server"],
            "level": LOG_LEVEL,
            "propagate": False,
        },
        "django.template": {
            "handlers": ["django.template"],
            "level": LOG_LEVEL,
            "propagate": False,
        },
        "deploy_board.webapp.security": {
            "handlers": ["default"],
            "level": "INFO",
            "propagate": True,
        },
        "deploy_board.audit": {
            "handlers": ["audit_log_handler"],
            "level": "INFO",
            "propagate": False,
        },
    },
}

# Application definition
INSTALLED_APPS = (
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "deploy_board.webapp",
)

oauth_middleware = "deploy_board.webapp.security.DelegatedOAuthMiddleware"
if TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN:
    oauth_middleware = "deploy_board.webapp.security.FixedOAuthMiddleware"

MIDDLEWARE = (
    "csp.middleware.CSPMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    oauth_middleware,
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
    "deploy_board.webapp.error_views.ExceptionHandlerMiddleware",
    "deploy_board.webapp.security.PRRMiddleware",
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


ROOT_URLCONF = "deploy_board.urls"

WSGI_APPLICATION = "deploy_board.wsgi.application"

LANGUAGE_CODE = "en-us"

TIME_ZONE = "UTC"

USE_I18N = True

USE_L10N = True

USE_TZ = True

# The number of days since the build publish date required to trigger an old build version warning message
OLD_BUILD_WARNING_THRESHOLD_DAYS = 10

# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.11/howto/static-files/
STATIC_URL = "/static/"
STATICFILES_DIRS = (os.path.join(PROJECT_PATH, "static"),)
# STATIC_ROOT = os.path.join(PROJECT_PATH, 'static')

MEDIA_ROOT = os.path.join(BASE_DIR, "media")
MEDIA_URL = "/media/"

# Site global metrics
SITE_METRICS_CONFIGS = []
# Deep Teletraan backend health check url
TELETRAAN_SERVICE_HEALTHCHECK_URL = os.getenv("TELETRAAN_SERVICE_HEALTHCHECK_URL", None)

# Show hosts that are STOPPING or STOPPED in the environments page
DISPLAY_STOPPING_HOSTS = os.getenv("DISPLAY_STOPPING_HOSTS", "true")

# Pinterest specific settings
IS_PINTEREST = True if os.getenv("IS_PINTEREST", "false") == "true" else False
BUILD_URL = os.getenv("BUILD_URL", None)
BUILDKITE_BUILD_URL = "https://buildkite.com/pinterest"
USER_DATA_CONFIG_SETTINGS_WIKI = os.getenv("USER_DATA_CONFIG_SETTINGS_WIKI", None)
ACCESS_ROLE_LIST = os.getenv("ACCESS_ROLE_LIST", None)


TELETRAAN_DISABLE_CREATE_ENV_PAGE = (
    True if os.getenv("TELETRAAN_DISABLE_CREATE_ENV_PAGE", "false") == "true" else False
)
TELETRAAN_DISABLE_BACKUP_INSTANCE_TYPES = (
    True
    if os.getenv("TELETRAAN_DISABLE_BACKUP_INSTANCE_TYPES", "false") == "true"
    else False
)
TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL = os.getenv(
    "TELETRAAN_REDIRECT_CREATE_ENV_PAGE_URL", None
)
TELETRAAN_CLUSTER_READONLY_FIELDS = os.getenv(
    "TELETRAAN_CLUSTER_READONLY_FIELDS", "spiffe_id"
).split(",")
IS_DURING_CODE_FREEZE = (
    True if os.getenv("TELETRAAN_CODE_FREEZE", "false") == "true" else False
)
TELETRAAN_CODE_FREEZE_URL = os.getenv("TELETRAAN_CODE_FREEZE_URL", None)
TELETRAAN_JIRA_SOURCE_URL = os.getenv("TELETRAAN_JIRA_SOURCE_URL", None)
TELETRAAN_TRANSFER_OWNERSHIP_URL = os.getenv("TELETRAAN_TRANSFER_OWNERSHIP_URL", None)
TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL = os.getenv(
    "TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL", None
)
TELETRAAN_PROJECT_URL_FORMAT = os.getenv("TELETRAAN_PROJECT_URL_FORMAT", None)

# use Rodimus if present
RODIMUS_SERVICE_URL = os.getenv("TELETRAAN_RODIMUS_SERVICE_URL", None)
RODIMUS_SERVICE_VERSION = os.getenv("TELETRAAN_RODIMUS_SERVICE_VERSION", None)
RODIMUS_SERVICE_PROXY_HTTP = os.getenv("TELETRAAN_RODIMUS_SERVICE_PROXY_HTTP", None)
RODIMUS_SERVICE_PROXY_HTTPS = os.getenv("TELETRAAN_RODIMUS_SERVICE_PROXY_HTTPS", None)
RODIMUS_SERVICE_USE_BEARER = (
    os.getenv("TELETRAAN_RODIMUS_SERVICE_USE_BEARER", "true").lower() != "false"
)  # default is True
RODIMUS_CLUSTER_REPLACEMENT_WIKI_URL = os.getenv(
    "TELETRAAN_RODIMUS_CLUSTER_REPLACEMENT_WIKI_URL", None
)
RODIMUS_AUTO_CLUSTER_REFRESH_WIKI_URL = os.getenv(
    "TELETRAAN_RODIMUS_AUTO_CLUSTER_REFRESH_WIKI_URL", None
)

UNAUTHORIZED_ERROR_TEXT = "Access denied. Please check if you have the required permission in the Admin panel."

if IS_PINTEREST:
    # use knox if present
    KNOX_SESSION_ID = os.getenv("KNOX_SESSION_ID")
    if KNOX_SESSION_ID:
        from knox import Knox

        SECRET_KEY = Knox(KNOX_SESSION_ID).get_primary()

    ADMIN_OAUTH_SECRET_KNOX_ID = os.getenv("ADMIN_OAUTH_SECRET_KNOX_ID")
    if ADMIN_OAUTH_SECRET_KNOX_ID:
        from knox import Knox

        OAUTH_CLIENT_SECRET = Knox(ADMIN_OAUTH_SECRET_KNOX_ID).get_primary()

    # Site health metrics
    REQUESTS_URL = os.getenv("REQUESTS_URL")
    SUCCESS_RATE_URL = os.getenv("SUCCESS_RATE_URL")
    LATENCY_URL = os.getenv("LATENCY_URL")
    SITE_METRICS_CONFIGS = [
        {
            "title": "Requests",
            "url": REQUESTS_URL,
            "specs": [
                {"min": 0, "max": 50000, "color": "Red"},
                {"min": 50000, "max": 80000, "color": "Yellow"},
                {"min": 80000, "max": 200000, "color": "Green"},
            ],
        },
        {
            "title": "Success",
            "url": SUCCESS_RATE_URL,
            "specs": [
                {"min": 90, "max": 98, "color": "Red"},
                {"min": 98, "max": 99, "color": "Yellow"},
                {"min": 99, "max": 100, "color": "Green"},
            ],
        },
        {
            "title": "Latency",
            "url": LATENCY_URL,
            "specs": [
                {"min": 800, "max": 1000, "color": "Red"},
                {"min": 600, "max": 800, "color": "Yellow"},
                {"min": 0, "max": 600, "color": "Green"},
            ],
        },
    ]

    DEFAULT_START_TIME = "-1d"

    # Pinterest Default Cloud Provider
    DEFAULT_PROVIDER = "AWS"

    # Pinterest Default AMI image name
    DEFAULT_CMP_IMAGE = "cmp_base"
    DEFAULT_CMP_ARM_IMAGE = "cmp_base"

    # Pinterest Default setting whether to use launch template or not
    DEFAULT_USE_LAUNCH_TEMPLATE = True

    # Pinterest Default Host Type
    # TODO: This is a description of the host type but is nonunique. However, it cannot be replaced by host_type ID since it is unique per service database.
    # TODO: The model for host type should be rebuilt based on a unique abstract factor such as ec2 instance type, for now we should keep expected behavior.
    DEFAULT_CMP_HOST_TYPE = "m7a.xlarge"
    DEFAULT_CMP_ARM_HOST_TYPE = "c7g.large"
    HOST_TYPE_ROADMAP_LINK = os.getenv("HOST_TYPE_ROADMAP_LINK")

    DEFAULT_CELL = "aws-us-east-1"
    DEFAULT_ARCH = "x86_64"
    DEFAULT_PLACEMENT = os.getenv("DEFAULT_CMP_PLACEMENT")

    # Pinterest Default Puppet Environment
    DEFAULT_CMP_PINFO_ENVIRON = os.getenv("DEFAULT_CMP_PINFO_ENVIRON")
    DEFAULT_CMP_ACCESS_ROLE = os.getenv("DEFAULT_CMP_ACCESS_ROLE")

    # CSP Config
    CSP_SCRIPT_SRC = (
        "'self'",
        "https://*.gstatic.com/ https://cdn.jsdelivr.net/ https://www.google.com/ 'unsafe-inline' 'unsafe-eval'",
    )
    CSP_DEFAULT_SRC = "'self'"
    CSP_CONNECT_SRC = "'self'"
    CSP_EXCLUDE_URL_PREFIXES = ("/api-docs",)
    CSP_STYLE_SRC = (
        "'self'",
        "https://*.gstatic.com/ https://cdn.jsdelivr.net/ 'unsafe-inline'",
    )

    # Nimbus service url
    NIMBUS_SERVICE_URL = os.getenv("NIMBUS_SERVICE_URL", None)
    NIMBUS_EGRESS_URL = os.getenv("NIMBUS_EGRESS_URL", None)
    NIMBUS_USE_EGRESS = os.getenv("NIMBUS_USE_EGRESS", "False").lower() == "true"
    NIMBUS_SERVICE_VERSION = os.getenv("NIMBUS_SERVICE_VERSION", None)

    IMAGE_PROVIDER_NAME_URL = os.getenv("IMAGE_PROVIDER_NAME_URL", None)

    DEFAULT_CLUSTER_TYPE = "PRODUCTION"

    # Auto AMI Update
    ENABLE_AMI_AUTO_UPDATE = 1

    # Stage Type Info Link
    STAGE_TYPE_INFO_LINK = os.getenv("STAGE_TYPE_INFO_LINK")

    # Primary Account
    AWS_PRIMARY_ACCOUNT = os.getenv("AWS_PRIMARY_ACCOUNT", "998131032990")
    # Sub Account
    AWS_SUB_ACCOUNT = os.getenv("AWS_SUB_ACCOUNT", "562567494283")

    UNAUTHORIZED_ERROR_TEXT = "Access denied. To resolve this issue, please follow: http://pinch.pinadmin.com/teletraan-auth-tsg"
