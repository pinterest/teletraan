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

import tests
from time import sleep
from deployd.common.stats import TimeElapsed


class TestTimeElapsed(tests.TestCase):

    def test__is_paused(self):
        elapsed = TimeElapsed()
        self.assertFalse(elapsed._is_paused())

    def test_get(self):
        elapsed = TimeElapsed()
        then = elapsed.get()
        self.assertFalse(elapsed._is_paused())
        sleep(0.5)
        now = elapsed.get()
        self.assertTrue(now > then)

        elapsed.pause()
        self.assertTrue(elapsed._is_paused())
        self.assertEqual(elapsed.get(), elapsed._time_elapsed)

    def test_since_pause(self):
        elapsed = TimeElapsed()
        self.assertFalse(elapsed._is_paused())
        self.assertEqual(elapsed.since_pause(), float(0))

        sleep(0.5)
        elapsed.pause()
        self.assertTrue(elapsed._is_paused())
        self.assertTrue(elapsed.since_pause() > 0)

    def test_pause(self):
        elapsed = TimeElapsed()
        self.assertFalse(elapsed._is_paused())
        elapsed.pause()
        sleep(1)
        self.assertTrue(elapsed._is_paused())

    def test_resume(self):
        elapsed = TimeElapsed()
        self.assertFalse(elapsed._is_paused())
        elapsed.resume()
        self.assertFalse(elapsed._is_paused())

        elapsed.pause()
        self.assertTrue(elapsed._is_paused())
        elapsed.resume()
        self.assertFalse(elapsed._is_paused())
