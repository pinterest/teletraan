# Copyright 2021 Pinterest, Inc.
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

from deployd.common.stats import (
    MetricCache,
    Stat,
    MetricClient,
    TimeElapsed,
    MetricCacheConfigurationError,
    MetricClientConfigurationError,
)
from deployd import __version__
import unittest
import os
import json
import socket
from time import sleep
from unittest import mock


class TestMetricCacheExceptions(unittest.TestCase):
    def test__MetricCacheConfigurationError(self):
        with self.assertRaises(MetricCacheConfigurationError):
            MetricCache(path=None)


class TestMetricCache(unittest.TestCase):
    path = "tests/unit/deploy/common/test_stats.cache"
    data = "testdata"

    def test_exists(self):
        cache = MetricCache(self.path)
        self.assertTrue(cache.exists())

    def test_is_empty(self):
        cache = MetricCache(self.path)
        cache.truncate()
        self.assertTrue(cache.is_empty())

    def test_read(self):
        cache = MetricCache(self.path)
        cache.truncate()
        with open(self.path, "w") as fh:
            fh.write("{}\n".format(self.data))
        count = 0
        for _ in cache.read():
            count += 1
        self.assertEqual(count, 1)

    def test_write(self):
        cache = MetricCache(self.path)
        cache.truncate()
        cache.write(self.data)
        with open(self.path, "r") as fh:
            validate = fh.read()
        expect = "{}\n".format(self.data)
        self.assertEqual(validate, expect)

    def tearDown(self):
        MetricCache(self.path)
        os.remove(self.path)


class TestStat(unittest.TestCase):
    mtype = "increment"
    name = "name"
    value = 1
    sample_rate = 1.0
    tags = {"key": "value"}
    data = (
        '{{"mtype": "{0}", '
        '"name": "{1}", '
        '"value": {2}, '
        '"sample_rate": {3}, '
        '"tags": {4}}}'
    )

    def test_serialize(self):
        stat = Stat(
            mtype=self.mtype,
            name=self.name,
            value=self.value,
            sample_rate=self.sample_rate,
            tags=self.tags,
        )
        self.assertIsInstance(stat.serialize(), str)

    def test__deserialize(self):
        data = self.data.format(
            self.mtype, self.name, self.value, self.sample_rate, json.dumps(self.tags)
        )
        stat = Stat(
            mtype=None, name=None, value=None, sample_rate=None, tags=None, ins=data
        )
        self.assertTrue(stat._deserialize())
        self.assertEqual(stat.mtype, self.mtype)
        self.assertEqual(stat.name, self.name)
        self.assertEqual(stat.sample_rate, self.sample_rate)
        self.assertEqual(stat.tags, self.tags)
        invalid_str = "{1:"
        stat = Stat(
            mtype=None,
            name=None,
            value=None,
            sample_rate=None,
            tags=None,
            ins=invalid_str,
        )
        with self.assertRaises(json.decoder.JSONDecodeError):
            stat._deserialize()
        invalid_prop = b"{\x48:\x69}"
        stat = Stat(
            mtype=None,
            name=None,
            value=None,
            sample_rate=None,
            tags=None,
            ins=invalid_prop,
        )
        with self.assertRaises(json.decoder.JSONDecodeError):
            stat._deserialize()
        invalid_type_1 = list()
        stat = Stat(
            mtype=None,
            name=None,
            value=None,
            sample_rate=None,
            tags=None,
            ins=invalid_type_1,
        )
        with self.assertRaises(TypeError):
            stat._deserialize()
        invalid_type_2 = 1
        stat = Stat(
            mtype=None,
            name=None,
            value=None,
            sample_rate=None,
            tags=None,
            ins=invalid_type_2,
        )
        with self.assertRaises(TypeError):
            stat._deserialize()

    def test_deserialize(self):
        stat = Stat(mtype=None, name=None, value=None, sample_rate=None, tags=None)
        data = self.data.format(
            self.mtype, self.name, self.value, self.sample_rate, json.dumps(self.tags)
        )
        self.assertTrue(stat.deserialize(ins=data))
        invalid_str = "{1:"
        self.assertFalse(stat.deserialize(ins=invalid_str))
        invalid_type = []
        self.assertFalse(stat.deserialize(ins=invalid_type))


class TestMetricClientExceptions(unittest.TestCase):
    def test__MetricClientConfigurationError(self):
        with self.assertRaises(MetricClientConfigurationError):
            MetricClient(port=None, cache_path=None)


class TestMetricClient(unittest.TestCase):
    cache_path = "tests/unit/deploy/common/test_stats.cache"
    port = 5000

    def setUp(self):
        self.client = MetricClient(port=self.port, cache_path=self.cache_path)

    def tearDown(self):
        MetricCache(self.cache_path)
        os.remove(self.cache_path)

    def test__add_default_tags(self):
        tag_version = {"deploy_agent_version": __version__}
        tags = self.client._add_default_tags()
        if not __version__:
            self.assertEqual(tags, None)
        else:
            self.assertEqual(tags, tag_version)
        tag_test = {"test": "data"}
        tags = self.client._add_default_tags(tag_test)
        self.assertEqual(tags, dict(tag_version, **tag_test))

    @mock.patch("socket.socket.connect_ex")
    def test_is_healthy(self, mock_connect_ex):
        mock_connect_ex.return_value = 0
        self.assertTrue(self.client.is_healthy())
        mock_connect_ex.return_value = 50
        self.assertFalse(self.client.is_healthy())
        mock_connect_ex.raiseError.side_effect = Exception(socket.error)
        self.assertFalse(self.client.is_healthy())


class TestTimeElapsed(unittest.TestCase):
    def setUp(self):
        self.elapsed = TimeElapsed()

    def test__is_paused(self):
        self.assertFalse(self.elapsed._is_paused())

    def test_get(self):
        then = self.elapsed.get()
        self.assertFalse(self.elapsed._is_paused())
        sleep(2)
        now = self.elapsed.get()
        self.assertTrue(now > then)

        self.elapsed.pause()
        self.assertTrue(self.elapsed._is_paused())
        self.assertEqual(self.elapsed.get(), self.elapsed._time_elapsed)

    def test_since_pause(self):
        self.assertFalse(self.elapsed._is_paused())
        self.assertEqual(self.elapsed.since_pause(), float(0))

        sleep(2)
        self.elapsed.pause()
        self.assertTrue(self.elapsed._is_paused())
        self.assertTrue(self.elapsed.since_pause() > 0)

    def test_pause(self):
        self.assertFalse(self.elapsed._is_paused())
        self.elapsed.pause()
        sleep(2)
        self.assertTrue(self.elapsed._is_paused())

    def test_resume(self):
        self.assertFalse(self.elapsed._is_paused())
        self.elapsed.resume()
        self.assertFalse(self.elapsed._is_paused())

        self.elapsed.pause()
        self.assertTrue(self.elapsed._is_paused())
        self.elapsed.resume()
        self.assertFalse(self.elapsed._is_paused())
