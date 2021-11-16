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

from deployd import IS_PINTEREST
import timeit


class DefaultStatsdTimer(object):
    def __enter__(self):
        pass

    def __exit__(self, stats, sample_rate=1.0, tags=None):
        pass


def create_stats_timer(stats, sample_rate=1.0, tags=None):
    if IS_PINTEREST:
        from pinstatsd.statsd import statsd_context_timer
        return statsd_context_timer(entry_name=stats, sample_rate=sample_rate, tags=tags)
    else:
        return DefaultStatsdTimer()


def create_sc_timing(stat, value, sample_rate=1.0, tags=None):
    if IS_PINTEREST:
        from pinstatsd.statsd import sc
        return sc.timing(stat, value, sample_rate=sample_rate, tags=tags)
    else:
        return


def create_sc_increment(stats, sample_rate=1.0, tags=None):
    if IS_PINTEREST:
        from pinstatsd.statsd import sc
        sc.increment(stats, sample_rate, tags)
    else:
        return


def create_sc_gauge(stat, value, sample_rate=1.0, tags=None):
    if IS_PINTEREST:
        from pinstatsd.statsd import sc
        sc.gauge(stat, value, sample_rate=sample_rate, tags=tags)
    else:
        return


class TimeElapsed:
    """ keep track of elapsed time in seconds """

    def __init__(self):
        self._time_start = self._timer()
        self._time_now = None
        self._time_elapsed = float(0)
        self._time_pause = None

    def get(self):
        """ total elapsed running time, accuracy in seconds """
        if self._is_paused():
            return self._time_elapsed
        self._time_now = self._timer()
        self._time_elapsed += float(self._time_now - self._time_start)
        self._time_start = self._time_now
        return int(self._time_elapsed)

    def _is_paused(self):
        """ timer pause state """
        if self._time_pause:
            return True
        return False

    @staticmethod
    def _timer():
        """ timer in seconds """
        return timeit.default_timer()

    def since_pause(self):
        """ time elapsed since pause """
        if self._is_paused():
            return float(self._timer() - self._time_pause)
        return float(0)

    def pause(self):
        """ pause timer if not paused """
        if not self._is_paused():
            self._time_pause = self._timer()

    def resume(self):
        """ resume timer if paused """
        if self._is_paused():
            self._time_start = self._timer()
            self._time_pause = None
