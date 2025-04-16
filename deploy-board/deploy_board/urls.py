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

from django.conf.urls import include, url

from django.contrib import admin
from django.conf.urls.static import static
from django.conf import settings

from .settings import IS_PINTEREST

admin.autodiscover()


if IS_PINTEREST:
    urlpatterns = [
        url(r"^", include("deploy_board.webapp.urls")),
        url(r"^", include("deploy_board.webapp.mix_urls")),
        url(r"^", include("deploy_board.webapp.arcee_urls")),
        url(r"^", include("deploy_board.webapp.cluster_urls")),
    ] + static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
else:
    urlpatterns = [
        url(r"^", include("deploy_board.webapp.urls")),
    ]
