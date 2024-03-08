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
from typing import Generator, Optional, Union
from deployd import __version__, IS_PINTEREST, METRIC_PORT_HEALTH, METRIC_CACHE_PATH, STATSBOARD_URL
import timeit
import socket
import json
import os
import requests 

if IS_PINTEREST:
    from pinstatsd.statsd import sc, sc_v2
else:
    class sc:
        @staticmethod
        def increment(name, sample_rate, tags):
            pass

        @staticmethod
        def gauge(name, value, sample_rate=None, tags=None):
            pass

        @staticmethod
        def timing(name, value, sample_rate=None, tags=None):
            pass

    class sc_v2(sc):
        pass

log = logging.getLogger(__name__)


class DefaultStatsdTimer(object):
    def __enter__(self):
        pass

    def __exit__(self, stats, sample_rate=1.0, tags=None):
        pass


class TimingOnlyStatClient:
    """ timing only stat client in order to use caching """
    @staticmethod
    def timing(*args, **kwargs) -> None:
        client = MetricClient()
        return client.send_context_timer(*args, **kwargs)


def create_stats_timer(name, sample_rate=1.0, tags=None):
    if IS_PINTEREST:
        from pinstatsd.statsd import statsd_context_timer
        timer = statsd_context_timer(entry_name=name,
                                     sample_rate=sample_rate,
                                     tags=tags,
                                     stat_client=TimingOnlyStatClient())
        return timer
    else:
        return DefaultStatsdTimer()


def create_sc_timing(name, value, sample_rate=1.0, tags=None) -> None:
    if IS_PINTEREST:
        mtype = 'timing'
        client = MetricClient()
        client.send(mtype=mtype,
                    name=name,
                    value=value,
                    sample_rate=sample_rate,
                    tags=tags)
    else:
        return


def create_sc_increment(name, sample_rate=1.0, tags=None) -> None:
    if IS_PINTEREST:
        mtype = 'increment'
        client = MetricClient()
        client.send(mtype=mtype,
                    name=name,
                    sample_rate=sample_rate,
                    tags=tags)
    else:
        return


def create_sc_gauge(name, value, sample_rate=1.0, tags=None) -> None:
    if IS_PINTEREST:
        mtype = 'gauge'
        client = MetricClient()
        client.send(mtype=mtype,
                    name=name,
                    value=value,
                    sample_rate=sample_rate,
                    tags=tags)
    else:
        return

def send_statsboard_metric(name, value, tags=None) -> None: 
    tags['host'] = socket.gethostname()
    tags_params = [f"{tag}={tags[tag]}" for tag in tags] 
    tags_str = ",".join(tags_params)
    url = (
        f"{STATSBOARD_URL}put/"
        f"{name}?value={value}"
        f"&tags={tags_str}"
    )

    resp = requests.put(url)
    if resp.status_code == 200:
        log.info("Successfully send the metric to statsboard")
    
class MetricCacheConfigurationError(ValueError):
    """ Raised when MetricCache has missing configuration """
    def __init__(self, name, value) -> None:
        msg = '{} is {}'.format(name, value)
        super(MetricCacheConfigurationError, self).__init__(msg)


class MetricCache:
    """ local cache for metrics
        creates empty cache file
    """
    def __init__(self, path=METRIC_CACHE_PATH) -> None:
        if not path:
            raise MetricCacheConfigurationError('path', path)
        self.path = path
        # maximum cache size in bytes
        self.max_size = (10 * 1024 * 1024)
        # init cache
        if not self.exists():
            self.truncate()

    def limit(self) -> bool:
        """ check to see if cache file has exceeded maximum size
            return: bool
        """
        return os.path.getsize(self.path) > self.max_size

    def exists(self) -> bool:
        """ cache file exists and is read/write
            return: bool
        """
        if os.access(self.path, os.F_OK) and \
           os.access(self.path, os.R_OK) and \
           os.access(self.path, os.W_OK):
            return True
        return False

    def is_empty(self) -> bool:
        """ check if the cache not empty
            return: bool
        """
        return not os.stat(self.path).st_size > 0

    def read(self) -> Generator:
        """ read metrics from cache, then delete
            return: generator
        """
        with open(self.path, 'r') as fh:
            for line in fh:
                yield Stat(ins=line)

    def write(self, output) -> None:
        """ write metrics to cache file respecting max cache size
            appends newline to metric
        """
        if self.limit():
            msg = 'cache file {} has exceeded maximum file size of {}'
            log.error(msg.format(self.path, self.max_size))
            return
        with open(self.path, 'a') as fh:
            fh.write('{}\n'.format(output))

    def truncate(self) -> None:
        """ purge cache file """
        with open(self.path, 'w') as fh:
            fh.truncate()


class Stat:
    """ stat class for simple data management, dataclasses are py3.7+
        supports all methods for stats
    """

    def __init__(self, mtype=None, name=None, value=None, sample_rate=None, tags=None, ins=None) -> None:
        self.mtype = mtype
        self.name = name
        self.value = value
        self.sample_rate = sample_rate
        self.tags = tags
        self.ins = ins
        try:
            self.JSONDecodeError = json.decoder.JSONDecodeError
        except AttributeError:
            # python2 support
            self.JSONDecodeError = ValueError

    def serialize(self) -> str:
        """ serialize for cache writing """
        obj = dict()
        obj['mtype'] = self.mtype
        if self.value is not None:
            # value can be 0
            obj['value'] = self.value
        obj['name'] = self.name
        obj['sample_rate'] = self.sample_rate
        obj['tags'] = self.tags
        return json.dumps(obj)

    def _deserialize(self) -> bool:
        """ read in json, setting defaults for a stat
            return: bool
        """
        obj = json.loads(self.ins)
        if isinstance(obj, dict):
            self.mtype = obj.get('mtype', None)
            self.name = obj.get('name', None)
            self.value = obj.get('value', None)
            self.sample_rate = obj.get('sample_rate', None)
            self.tags = obj.get('tags', None)
            return True
        return False

    def deserialize(self, ins=None) -> bool:
        """ attempt to deserialize
            :param: ins json as str
            return: bool, False on error
        """
        if ins:
            self.ins = ins
        try:
            valid = self._deserialize()
            if valid:
                return True
        except self.JSONDecodeError:
            return False
        except TypeError:
            return False
        return False


class MetricClientConfigurationError(ValueError):
    """ Raised when MetricClient has missing configuration """
    def __init__(self, name, value) -> None:
        msg = '{} is {}'.format(name, value)
        super(MetricClientConfigurationError, self).__init__(msg)


class MetricClient:
    """ metrics client wrapper, enables disk cache """

    def __init__(self, port=METRIC_PORT_HEALTH, cache_path=METRIC_CACHE_PATH) -> None:
        if not port:
            raise MetricClientConfigurationError('port', port)
        self.port = port
        self.cache = MetricCache(path=cache_path)
        self.stat = None

    @staticmethod
    def _add_default_tags(tags=None) -> Optional[dict]:
        """ add default tags to stats
            :param: tags as dict
            return: dict
        """
        if not __version__:
            # defensive case, should not be hit
            return tags
        if __version__ and not tags:
            tags = dict()
        tags['deploy_agent_version'] = __version__
        return tags

    @staticmethod
    def _parse_stat(mtype=None, name=None, value=None, sample_rate=None, tags=None) -> Stat:
        """ return Stat for given kwargs """
        return Stat(mtype=mtype,
                    name=name,
                    value=value,
                    sample_rate=sample_rate,
                    tags=tags)

    def _send_mtype(self) -> None:
        """ send metric to sc using corrected
            calls per metric type
        """
        func = getattr(sc, self.stat.mtype)
        func_v2 = getattr(sc_v2, self.stat.mtype)

        # remove specific tags
        if isinstance(self.stat.tags, dict):
            self.stat.tags.pop('host', None)

        # name suffix to differentiate sc from sc_v2
        name_sc_v2 = '{}.cluster'.format(self.stat.name)

        if self.stat.mtype == 'increment':
            # v2 is called first due to tag mutability
            func_v2(name_sc_v2, self.stat.sample_rate, self.stat.tags)
            func(self.stat.name, self.stat.sample_rate, self.stat.tags)
        elif self.stat.mtype == 'gauge' or self.stat.mtype == 'timing':
            func_v2(name_sc_v2, self.stat.value, sample_rate=self.stat.sample_rate, tags=self.stat.tags)
            func(self.stat.name, self.stat.value, sample_rate=self.stat.sample_rate, tags=self.stat.tags)
        else:
            msg = 'encountered unsupported mtype:{} while sending name:{}, value:{}, sample_rate:{}, tags:{}'
            log.error(msg.format(self.stat.mtype, self.stat.name, self.stat.value, self.stat.sample_rate, self.stat.tags))

    def _send(self) -> None:
        """ send metric to sc """
        try:
            self._send_mtype()
        except Exception as error:
            log.error('unable to send metric: {}'.format(error))

    def _flush_cache(self) -> None:
        """ read from cache, send every metric, truncate cache """
        log.warning('flushing metrics from cache')
        for stat in self.cache.read():
            if not stat.deserialize():
                # unable to parse stat, skip
                log.error('unable to parse stat: {}'.format(stat))
                continue
            self.stat = stat
            self._send()
        # truncate cache file after flush
        self.cache.truncate()

    def send_context_timer(self, name, value, sample_rate=None, tags=None) -> None:
        """ convert a context_timer to timing
            for cacheability
        """
        self.send(mtype='timing', name=name, value=value, sample_rate=sample_rate, tags=tags)

    def send(self, mtype=None, name=None, value=None, sample_rate=None, tags=None) -> None:
        """ add default tags, send metric, write to, or flush cache
            depending on health check """
        tags = self._add_default_tags(tags)
        if self.is_healthy():
            # trigger flush cache first
            if self.cache and (self.cache.exists() and not self.cache.is_empty()):
                self._flush_cache()
            # send metric
            self.stat = self._parse_stat(mtype=mtype, name=name, value=value, sample_rate=sample_rate, tags=tags)
            self._send()
        else:
            # health check failed, write stat to cache
            stat = self._parse_stat(mtype=mtype, name=name, value=value, sample_rate=sample_rate, tags=tags)
            self.cache.write(stat.serialize())

    def is_healthy(self) -> bool:
        """ health-check by connecting to local IPv4 TCP listening socket
            return: bool
        """
        sock = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
        # socket context manager was added in python3.2
        passing = False
        try:
            indicator = sock.connect_ex((str(), int(self.port)))
            if indicator == 0:
                passing = True
        except socket.error as error:
            log.error('metric health-check failed: {}'.format(error))
            passing = False
        finally:
            sock.close()
        return passing


class TimeElapsed:
    """ keep track of elapsed time in seconds """

    def __init__(self) -> None:
        self._time_start = self._timer()
        self._time_now = None
        self._time_elapsed = float(0)
        self._time_pause = None

    def get(self) -> Union[float, int]:
        """ total elapsed running time, accuracy in seconds
            return: int
        """
        if self._is_paused():
            return self._time_elapsed
        self._time_now = self._timer()
        self._time_elapsed += float(self._time_now - self._time_start)
        self._time_start = self._time_now
        return int(self._time_elapsed)

    def _is_paused(self) -> bool:
        """ timer pause state
            return: bool
        """
        if self._time_pause:
            return True
        return False

    @staticmethod
    def _timer() -> float:
        """ timer in seconds
            return: float
        """
        return timeit.default_timer()

    def since_pause(self) -> float:
        """ time elapsed since pause
            return: float
        """
        if self._is_paused():
            return float(self._timer() - self._time_pause)
        return float(0)

    def pause(self) -> None:
        """ pause timer if not paused
            return: None
        """
        if not self._is_paused():
            self._time_pause = self._timer()

    def resume(self) -> None:
        """ resume timer if paused
            return: None
        """
        if self._is_paused():
            self._time_start = self._timer()
            self._time_pause = None
