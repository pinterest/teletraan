#!/usr/bin/env python
from django.core.management import execute_from_command_line
from gevent import monkey
import os
from os.path import dirname
import sys


# Correct setup to work locally against integ

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "deploy_board.settings")
# os.environ.setdefault("HTTPS", "on")
# os.environ.setdefault("RODIMUS_SERVICE_PROXY_HTTPS", "https://rodimus-integ.pinadmin.com")
# os.environ.setdefault("TELETRAAN_SERVICE_PROXY_HTTPS", "https://teletraan-integ.pinadmin.com")
os.environ.setdefault("TELETRAAN_SERVICE_FIXED_OAUTH_TOKEN", "")
os.environ.setdefault("SECRET_KEY", "")
os.environ.setdefault("ENV_STAGE", "integ")
os.environ.setdefault("TELETRAAN_SERVICE_URL", "https://teletraan-integ.pinadmin.com")
os.environ.setdefault("TELETRAAN_SERVICE_VERSION", "v1")
basedir = dirname(dirname(__file__))
os.environ.setdefault("BASE_DIR", basedir)

os.environ.setdefault("IS_PINTEREST", "true")

os.environ.setdefault("TELETRAAN_SERVICE_HEALTHCHECK_URL", "https://teletraan-integ.pinadmin.com/healthcheck")
os.environ.setdefault("TELETRAAN_TRANSFER_OWNERSHIP_URL", "https://nimbus.pinadmin.com/identifiers/transfer")
os.environ.setdefault("TELETRAAN_RESOURCE_OWNERSHIP_WIKI_URL",
                      "https://w.pinadmin.com/pages/viewpage.action?pageId=51519072")
os.environ.setdefault("TELETRAAN_PROJECT_URL_FORMAT",
                      "https://nimbus.pinadmin.com/projects/overview/{projectName}/platform-teletraan")
os.environ.setdefault("TELETRAAN_CLUSTER_READONLY_FIELDS", "spiffe_id,nimbus_id")

os.environ.setdefault("BUILD_URL", "https://jenkins.pinadmin.com/job/")

os.environ.setdefault("RODIMUS_SERVICE_URL", "https://rodimus-integ.pinadmin.com")
os.environ.setdefault("RODIMUS_SERVICE_VERSION", "v1")
os.environ.setdefault("NIMBUS_SERVICE_URL", "https://nimbus.pinadmin.com")
os.environ.setdefault("NIMBUS_SERVICE_VERSION", "v1")

os.environ.setdefault(
    "REQUESTS_URL", "https://statsboard.pinadmin.com/api/v2/query?&target=sum:aws.elb.teletraan-integ.RequestCount&alias=m0")
os.environ.setdefault(
    "SUCCESS_RATE_URL", "https://statsboard.pinadmin.com/api/v2/query?&target=sum%3Aaws.elb.teletraan-integ.HTTPCode_Backend_2XX&alias=elb_2xx&target=sum%3Aaws.elb.teletraan-integ.HTTPCode_Backend_3XX&alias=elb_3xx&target=sum%3Aaws.elb.teletraan-integ.HTTPCode_Backend_4XX&alias=elb_4xx&target=sum%3Aaws.elb.teletraan-integ.HTTPCode_Backend_5XX&alias=elb_5xx&cmd=good+%3D+elb_2xx+%2B+elb_3xx+%2B+elb_4xx%0Aall+%3D+elb_2xx+%2B+elb_3xx+%2B+elb_4xx+%2B+elb_5xx%0Aall+%3D+all%5Ball+%3E+100%5D%0Asr+%3D+%28good%2Fall%29+*+100%0Areturn+sr")
os.environ.setdefault(
    "LATENCY_URL", "https://statsboard.pinadmin.com/api/v2/query?&target=p99:aws.elb.teletraan-integ.Latency&alias=m0")
os.environ.setdefault("HOST_INFORMATION_URL",
                      "http://cmdbui.pinadmin.com:80")
os.environ.setdefault("CMDB_API_HOST", "https://cmdbapi.pinadmin.com")
os.environ.setdefault("CMDB_INSTANCE_URL", "/v2/instance/")
os.environ.setdefault("CMDB_UI_HOST", "https://cmdbui.pinadmin.com")
os.environ.setdefault("PHOBOS_URL", "https://phobos.pinadmin.com/phobos/")
os.environ.setdefault("DEFAULT_CMP_PINFO_ENVIRON", "dev")
os.environ.setdefault("SERVICE_RATELIMIT_CONFIG_URL",
                      "https://www.pinterest.com/220calave/ratelimit/")
os.environ.setdefault("ENABLING_SERVICE_RATELIMIT_URL",
                      "https://w.pinadmin.com/display/IN/Service+Framework%3A+How+to+add+rate+limiting+to+your+service")
os.environ.setdefault("KAFKA_MSGS_DELIVERED_METRIC",
                      "ostrich.counters.singer.writer.num_kafka_messages_delivery_success")
os.environ.setdefault(
    "USER_DATA_CONFIG_SETTINGS_WIKI",
    "https://w.pinadmin.com/display/IN/Launching+a+new+service#Launchinganewservice-3.UserdataAdvancedSettingsconfigsreference")

# Some format constants.
os.environ.setdefault("STATSBOARD_API_FORMAT",
                      "https://statsboard.pinadmin.com/api/v1/query?target={metric}{{{tags}}}&start={startTime}")
os.environ.setdefault("STATSBOARD_API_PREFIX",
                      "https://statsboard.pinadmin.com/api/")
os.environ.setdefault("STATSBOARD_HOST_TYPE_API_FORMAT",
                      "https://statsboard.pinadmin.com/api/v1/teletraan_host_types?env={env}&stage={stage}")
os.environ.setdefault("STATSBOARD_HUB_URL_ENDPOINT_FORMAT",
                      "https://statsboard.pinadmin.com/hub?host_type={hostType}")
os.environ.setdefault("RATELIMIT_ENABLED_METRIC_FORMAT",
                      "ostrich.gauges.{serviceName}.common.ratelimitenabled")

os.environ.setdefault('DEFAULT_CMP_PLACEMENT', "dev-private-service")
os.environ.setdefault('DEFAULT_CMP_PINFO_ENVIRON', "integ")
os.environ.setdefault('DEFAULT_CMP_ACCESS_ROLE', "eng-prod")

monkey.patch_all()
execute_from_command_line(sys.argv)