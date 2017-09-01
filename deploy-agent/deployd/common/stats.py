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


class DefaultStatsdTimer(object):
    def __enter__(self):
        pass

    def __exit__(self, stats, sample_rate, tags):
        pass


def create_stats_timer(stats, sample_rate, tags):
    if IS_PINTEREST:
        from pinstatsd.statsd import statsd_context_timer
        return statsd_context_timer(entry_name=stats, sample_rate=sample_rate, tags=tags)
    else:
        return DefaultStatsdTimer()


def create_sc_increment(stats, sample_rate, tags):
    if IS_PINTEREST:
        from pinstatsd.statsd import sc
        sc.increment(stats, sample_rate, tags)
    else:
        return
