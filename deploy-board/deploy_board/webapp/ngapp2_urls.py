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
import ngapp2_view

urlpatterns = [
    url(r'^ngapp2/deploy/$', ngapp2_view.NgappView.as_view()),
    url(r'^ngapp2/new_deploy/$', ngapp2_view.NgappView.as_view()),
    url(r'^ngapp2/promote_to/(?P<deploy_id>['r'a-zA-Z0-9\-_]+)/(?P<build>[a-zA-Z0-9\-_]+)$',
        ngapp2_view.promote_to_prod),
    url(r'^ngapp2/deploy_to/(?P<name>[a-zA-Z0-9\-_]+)/$', ngapp2_view.deploy_to_canary),
    url(r'^ngapp2/update_deploy_progress/$', ngapp2_view.NgappStatusView.as_view()),
    url(r'^ngapp2/deploys/$', ngapp2_view.get_all_deploys),
    url(r'^ngapp2/compare_deploys/$', ngapp2_view.compare_deploys),
]
