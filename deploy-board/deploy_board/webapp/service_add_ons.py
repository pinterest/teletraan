# Copyright 2017 Pinterest, Inc.
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


from deploy_board.settings import IS_PINTEREST, SERVICE_RATELIMIT_CONFIG_URL, \
                                  STATSBOARD_API_FORMAT, RATELIMIT_ENABLED_METRIC_FORMAT, \
                                  ENABLING_SERVICE_RATELIMIT_URL, KAFKA_MSGS_DELIVERED_METRIC
import urllib2
import simplejson as json
import socket
import time
import os

class ServiceAddOn(object):
    """
    Represents the information of a service add-on for a specific service.
    Currently supported service add-ons: rate limiting, kafka logging.
    """

    # Class constants to represent the state of the service add on.

    OFF = "OFF"
    ON = "ON"
    PARTIAL = "PARTIAL"

    ERROR = "ERROR"
    LOADING = "LOADING"
    UNKNOWN = "UNKNOWN"
    DEFAULT = "DEFAULT"

    # The default timeout for add-on related API calls.
    REQUEST_TIMEOUT_SECS = 30

    def __init__(self,
                 serviceName=None,
                 addOnName=None,
                 buttonUrl=None,
                 tagHoverInfo=None,
                 tagInfo=None,
                 state=UNKNOWN):
        self.serviceName = serviceName
        self.addOnName = addOnName
        self.buttonUrl = buttonUrl
        self.tagHoverInfo = tagHoverInfo
        self.tagInfo = tagInfo
        self.state = state


class RatelimitingAddOn(ServiceAddOn):
    """
    Encapsulates the information managed by the ratelimiting add on tag.
    """
    def __init__(self,
                 serviceName=None,
                 buttonUrl=None,
                 tagHoverInfo=None,
                 tagInfo=None,
                 state=ServiceAddOn.UNKNOWN,
                 rateLimitingReport=None):
        ServiceAddOn.__init__(self,
                              serviceName=serviceName,
                              addOnName="rate_limiting",
                              buttonUrl=buttonUrl,
                              tagHoverInfo=tagHoverInfo,
                              tagInfo=tagInfo,
                              state=state)

        if state == ServiceAddOn.UNKNOWN or rateLimitingReport is None:
            return

        tagHoverInfo_ = "{health} {hosts} {linkBlurb} {questions}"
        health = ""
        hostsFormat = "Hosts on: {on}/{total}, off: {off}/{total}, unknown: {unknown}/{total}."
        hosts = hostsFormat.format(on=rateLimitingReport.totalHostsOn,
                                   off=rateLimitingReport.totalHostsOff,
                                   unknown=rateLimitingReport.totalHostsUnknown,
                                   total=rateLimitingReport.totalHosts)
        linkBlurb = ""
        questions = "Questions @cmp"

        buttonUrl_ = ""
        tagInfo_ = "Rate Limiting: {status}"

        if state == ServiceAddOn.ON:
            health = "Things look good here -- "
            linkBlurb = " Click to see current rate limiting configuration."
            tagInfo_ = tagInfo_.format(status="ON")
            buttonUrl_ = "{url}#{serviceName}".format(url=SERVICE_RATELIMIT_CONFIG_URL, serviceName=serviceName)

        elif state == ServiceAddOn.OFF:
            health = "Looks like ratelimiting is turned off. "
            linkBlurb = " Click to learn how to enable ratelimiting."
            tagInfo_ = tagInfo_.format(status="OFF")
            buttonUrl_ = ENABLING_SERVICE_RATELIMIT_URL

        else: # Partial
            health = "Rate limiting seems to be ON only on some of the hosts here. "
            linkBlurb = " Click to see current rate limiting configuration."
            tagInfo_ = tagInfo_.format(status="PARTIAL")
            buttonUrl_ = "{url}#{serviceName}".format(url=SERVICE_RATELIMIT_CONFIG_URL, serviceName=serviceName)

        tagHoverInfo_ = tagHoverInfo_.format(health=health, hosts=hosts, linkBlurb=linkBlurb, questions=questions)

        self.tagHoverInfo = tagHoverInfo if tagHoverInfo else tagHoverInfo_
        self.buttonUrl = buttonUrl if buttonUrl else buttonUrl_
        self.tagInfo = tagInfo if tagInfo else tagInfo_
        self.rateLimitingReport = rateLimitingReport


class KafkaLoggingAddOn(ServiceAddOn):
    """
    Encapsulates the information managed by the kafka logging add on tag.
    """

    # The maximum number of lognames and topics that can be
    # specified in a single log check.
    MAX_LOGNAMES_IN_HEALTH_QUERY = 10
    MAX_TOPICS_IN_HEALTH_QUERY = 10

    def __init__(self,
                 logHealthReport=None,
                 serviceName=None,
                 buttonUrl=None,
                 tagHoverInfo="Click to check logging to Kafka from this stage.",
                 tagInfo="Log Check",
                 state=ServiceAddOn.UNKNOWN):
        ServiceAddOn.__init__(self,
                              serviceName=serviceName,
                              addOnName="kafka_logging",
                              buttonUrl=buttonUrl,
                              tagHoverInfo=tagHoverInfo,
                              tagInfo=tagInfo,
                              state=state)

        self.logHealthReport = logHealthReport


class LogHealthReport(object):
    """
    The results of a log health query.  Used by the kafka logging add on.
    """

    # Constants represent the result of a specific log health query.
    WARNING = "WARNING"
    STABLE = "STABLE"
    ERROR = "ERROR"

    # The default number of minutes back to check for a log.
    MINS_BACK_TO_CHECK = "120"

    def __init__(self,
                 env=None,
                 stage=None,
                 topics=[],
                 lognames=[],
                 state=None,
                 lastLogMinutesAgo=None,
                 latestLogAgoMinsBeforeWarning=MINS_BACK_TO_CHECK,
                 errorMsg=""):
        self.env = env
        self.stage = stage
        self.topics = topics
        self.lognames = lognames
        self.state = state
        self.lastLogMinutesAgo = lastLogMinutesAgo
        self.latestLogAgoMinsBeforeWarning = latestLogAgoMinsBeforeWarning
        self.errorMsg = errorMsg


class RateLimitingReport(object):
    """
    The results of a ratelimiting status query.  Used by the rate limiting add on.
    """
    def __init__(self,
                 totalHostsOn=None,
                 totalHostsOff=None,
                 totalHostsUnknown=None,
                 totalHosts=None,
                 state=ServiceAddOn.UNKNOWN):
        self.totalHostsOn = totalHostsOn
        self.totalHostsOff = totalHostsOff
        self.totalHostsUnknown = totalHostsUnknown
        self.totalHosts = totalHosts
        self.state = state

def getRatelimitingReport(serviceName, agentStats):
    """
    Current per-stage state assumption for rate limiting:

        +-------------------------+----+-----+---------+------------+
        |  At least one host is...| ON | OFF | UNKNOWN | STATE      |
        +                         +----+-----+---------+------------+
        |                         |  X |     |         |  ON        |
        +                         +----+-----+---------+------------+
        |                         |    |  X  |         |  OFF       |
        +                         +----+-----+---------+------------+
        |                         |    |     |    X    |   ?        |
        +                         +----+-----+---------+------------+
        |                         |  X |  X  |         |  PARTIAL   |
        +                         +----+-----+---------+------------+
        |                         |  X |     |    X    |  ON        |
        +                         +----+-----+---------+------------+
        |                         |    |  X  |    X    |  OFF       |
        +                         +----+-----+---------+------------+
        |                         |  X |  X  |    X    |  PARTIAL   |
        +-------------------------+----+-----+---------+------------+

    """
    commonHostPrefix = getCommonHostPrefix(agentStats)
    totalHosts = len(agentStats)

    # Don't make a claim if can't find a common prefix or there are no hosts on the stage.
    if commonHostPrefix == "" or totalHosts == 0:
        return RateLimitingReport(state=ServiceAddOn.UNKNOWN)

    totalHostsOn = 0
    totalHostsOff = 0
    totalHostsUnknown = 0

    if totalHosts > 1:
        commonHostPrefix += "*"

    metricStr = RATELIMIT_ENABLED_METRIC_FORMAT.format(serviceName=serviceName)
    apiUrl = STATSBOARD_API_FORMAT.format(metric=metricStr,
                                          tags="host=%s" % commonHostPrefix,
                                          startTime="-1min")

    try:
        statsboardData = getStatsboardData(apiUrl)
        for dataSlice in statsboardData:
            dataPoints = dataSlice["datapoints"]
            if dataPoints is None or len(dataPoints) == 0:
                totalHostsUnknown += 1
            elif dataPoints[-1][1] == 1:
                totalHostsOn += 1
            else:
                totalHostsOff += 1
    except:
        # In any error we abstain from making a claim, including request timeouts.
        return RateLimitingReport(state=ServiceAddOn.UNKNOWN)

    if not statsboardDataConsistent(statsboardData, totalHosts):
        return RateLimitingReport(state=ServiceAddOn.UNKNOWN)

    totalHostsUnknown += totalHosts - (totalHostsUnknown + totalHostsOff + totalHostsOn)
    rateLimitingReport = RateLimitingReport(totalHosts=totalHosts,
                                            totalHostsOn=totalHostsOn,
                                            totalHostsOff=totalHostsOff,
                                            totalHostsUnknown=totalHostsUnknown)

    if totalHostsOn > 0 and totalHostsOff == 0:
        rateLimitingReport.state = ServiceAddOn.ON
        return rateLimitingReport

    if totalHostsOff > 0 and totalHostsOn == 0:
        rateLimitingReport.state = ServiceAddOn.OFF
        return rateLimitingReport

    if totalHostsOff > 0 and totalHostsOn > 0:
        rateLimitingReport.state = ServiceAddOn.PARTIAL
        return rateLimitingReport

    return rateLimitingReport


def getLatestLogUnixTime(topics, lognames, numHostsOnStage, commonHostPrefix):
    """

    Returns the latest unix time at which any log in lognames reached
    any topic in topics.  Returns -1 if not found within the default
    time period back to check.

    Returns None if there was an error.

    :param topics:
    :param lognames:
    :param numTotalHosts:
    :param commonHostPrefix:
    :return:
    """
    topicsApiStr = '|'.join(topics)
    lognamesApiStr = '|'.join(lognames)

    if numHostsOnStage > 1:
        commonHostPrefix += "*"

    metricTag = "host=%s,topic=%s,logname=%s" % (commonHostPrefix, topicsApiStr, lognamesApiStr)
    startTime = "-%smin" % LogHealthReport.MINS_BACK_TO_CHECK
    apiUrl = STATSBOARD_API_FORMAT.format(metric=KAFKA_MSGS_DELIVERED_METRIC,
                                          tags=metricTag,
                                          startTime=startTime)

    earliestMessages = []
    try:
        statsboardData = getStatsboardData(apiUrl)
        for dataSlice in statsboardData:
            dataPoints = dataSlice["datapoints"]
            for k in reversed(range(len(dataPoints))):
                if dataPoints[k][1] > 0:
                    earliestMessages.append(dataPoints[k])
                    break
    except:
        return None

    if not statsboardDataConsistent(statsboardData, numHostsOnStage):
        return None

    if len(earliestMessages) == 0:
        return -1

    return max(earliestMessages, key=lambda d: d[0])[0]

def getLogHealthReport(configStr, report):
    """
    Given an agent report and a configuration string,
    gets a report containing information about the status of the logs
    indicated in the configStr.

    Currently, this information is how long ago certain logs were
    last received by certain topics, where the logs and topics
    are specified in the configStr format.

    :param report:
    :param configStr:
    :return:
    """
    commonHostPrefix = getCommonHostPrefix(report.agentStats)
    total_hosts = len(report.agentStats)

    if total_hosts == 0:
        return LogHealthReport(state=LogHealthReport.ERROR,
                               errorMsg="Could not find any hosts in this stage")

    if not configStr or commonHostPrefix == "":
        # No claim if no config string, no common prefix
        return LogHealthReport(state=ServiceAddOn.UNKNOWN)

    topicsStr = configStr.split(":")[0]
    lognamesStr = configStr.split(":")[1]

    topics = list(set([x.strip() for x in topicsStr.split(",")]))
    lognames = list(set([x.strip() for x in lognamesStr.split(",")]))

    if not logCheckInputValid(topics, lognames):
        return LogHealthReport(topics=topics,
                               lognames=lognames,
                               state=LogHealthReport.ERROR,
                               errorMsg="Invalid topics or lognames")

    lastLogUnixTime = getLatestLogUnixTime(topics, lognames, total_hosts, commonHostPrefix)
    lastLogMinutesAgo = None
    errorMsg = ""

    if not lastLogUnixTime:
        state = LogHealthReport.ERROR
        errorMsg = "There was a problem fetching data from Statsboard"
    elif lastLogUnixTime == -1:
        state = LogHealthReport.WARNING
    else:
        state = LogHealthReport.STABLE
        lastLogMinutesAgo = int((time.time() - lastLogUnixTime) / 60 + 1)

    return LogHealthReport(env=report.envName,
                           stage=report.stageName,
                           topics=topics,
                           lognames=lognames,
                           state=state,
                           lastLogMinutesAgo=lastLogMinutesAgo,
                           errorMsg=errorMsg)

def getRatelimitingAddOn(serviceName, report):
    # TODO: keep for now, until testing phase is over.
    if serviceName == "helloworlddummyservice-server" or serviceName == "sample-service-0":
        serviceName = "helloworlddummyservice"

    # TODO: keep for now, until testing phase is over.
    if serviceName == "genesis_services_shared":
        serviceName = report.stageName

    serviceName = serviceName.lower()
    rateLimitingReport = getRatelimitingReport(serviceName, report.agentStats)
    return RatelimitingAddOn(serviceName=serviceName,
                             state=rateLimitingReport.state,
                             rateLimitingReport=rateLimitingReport)

def getKafkaLoggingAddOn(serviceName, report, configStr=None):
    serviceName = serviceName.lower()
    logHealthReport = getLogHealthReport(configStr, report)
    url = "/env/%s/%s/check_log_status" % (report.envName, report.stageName)
    return KafkaLoggingAddOn(serviceName=serviceName,
                             buttonUrl=url,
                             state=ServiceAddOn.DEFAULT,
                             logHealthReport=logHealthReport)


""" --- Utility functions live below here --- """


def logCheckInputValid(topics, lognames):
    """
        - Neither topics nor lognames can be empty
        - If either contains "*", it must be the case that
          it is the only entry there.
        - Cannot specify more than a certain number of topics or lognames.
        - No string in topics or lognames can be empty or have whitespace.

    :param topics:
    :param lognames:
    :return:
    """
    if len(topics) == 0 or len(lognames) == 0:
        return False

    if len(topics) > KafkaLoggingAddOn.MAX_TOPICS_IN_HEALTH_QUERY or \
            len(lognames) > KafkaLoggingAddOn.MAX_LOGNAMES_IN_HEALTH_QUERY:
        return False

    for i in range(len(topics)):
        if not topics[i]:
            return False
        if topics[i] == "*":
            if len(topics) > 1:
                return False

    for i in range(len(lognames)):
        if not lognames[i]:
            return False
        if lognames[i] == "*":
            if len(lognames) > 1:
                return False

    return True

def getCommonHostPrefix(agentStats):
    """
    Utility function returns the common prefix of all the hosts
    in the environment represented by "agentStats"
    :param agentStats:
    :return:
    """
    hosts = []
    for agentStat in agentStats:
        if "hostName" in agentStat.agent:
            hosts.append(agentStat.agent["hostName"])

    if len(hosts) == 0:
        return ""

    return os.path.commonprefix(hosts)

def statsboardDataConsistent(statsboardData, numHostsOnCurrStage):
    """
    Utility function validates the consistency of statsboard data fetched
    for use by service add ons.

    :param statsboardData:
    :param numHostsOnCurrStage:
    :return:
    """

    if not statsboardData:
        return False

    hostTypes = []
    hostsFound = []

    for dataSlice in statsboardData:
        if "host" in dataSlice["tags"]:
            hostsFound.append(dataSlice["tags"]["host"])

        if "host_type" in dataSlice["tags"]:
            hostTypes.append(dataSlice["tags"]["host_type"])

        # There should be no errors in each data slice.
        if "error" in dataSlice:
            return False

    # There should only be one host type on each stage.
    if len(set(hostTypes)) > 1:
        return False

    # We should not get results for more hosts than are on the current stage.
    if len(set(hostsFound)) > numHostsOnCurrStage:
        return False

    return True

def getStatsboardData(apiUrl):
    """
    Given a statsboard API url, returns a list of lists containing
    statsboard data.

    :param apiUrl:
    :return:
    """
    url = urllib2.urlopen(apiUrl, timeout=ServiceAddOn.REQUEST_TIMEOUT_SECS)
    j = json.loads(url.read())
    data = []
    for i in range(len(j)):
        data.append(j[i])
    return data





