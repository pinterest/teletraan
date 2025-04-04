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

# -*- coding: utf-8 -*-
from django.conf.urls import url
from . import feedback_views
from . import hotfix_views

urlpatterns = [
    # hotfixes
    url(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hotfixes/$",
        hotfix_views.HotfixesView.as_view(),
    ),
    url(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hotfix/"
        r"(?P<id>[a-zA-Z0-9\-_]+)",
        hotfix_views.get_hotfix,
    ),
    url(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/patch/$",
        hotfix_views.patch,
    ),
    url(r"^hotfix/(?P<id>[" r"a-zA-Z0-9\-_]+)/$", hotfix_views.get_hotfix_detail),
    # ratings
    url(r"^submit_feedback/$", feedback_views.submit_feedback),
    url(r"^ratings/$", feedback_views.RatingsView.as_view()),
]
