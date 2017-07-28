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
import cluster_view
import capacity_views

urlpatterns = [
    url(r'^clouds/create_base_image/$', cluster_view.create_base_image),
    url(r'^clouds/baseimages/$', cluster_view.get_base_images),
    url(r'^clouds/get_image_names/$', cluster_view.get_image_names),
    url(r'^clouds/image_names/(?P<provider>[a-zA-Z0-9\-_\.]+)/(?P<region>[a-zA-Z0-9\-_\.]+)/$',
        cluster_view.get_image_names_by_provider_and_region),
    url(r'^clouds/get_base_images/$', cluster_view.get_base_images_by_name),
    url(r'^clouds/get_base_images/(?P<name>[a-zA-Z0-9\-_\.]+)/$', cluster_view.get_base_images_by_name_json),
    url(r'^clouds/get_base_image_info/(?P<name>[a-zA-Z0-9\-_]+)/$', cluster_view.get_base_images_by_name_json),

    url(r'^clouds/create_host_type/$', cluster_view.create_host_type),
    url(r'^clouds/hosttypes/$', cluster_view.get_host_types),
    url(r'^clouds/get_host_types/$', cluster_view.get_host_types_by_provider),
    url(r'^clouds/get_host_type_info/$', cluster_view.get_host_type_info),

    url(r'^clouds/create_security_zone/$', cluster_view.create_security_zone),
    url(r'^clouds/securityzones/$', cluster_view.get_security_zones),
    url(r'^clouds/get_security_zones/$', cluster_view.get_security_zones_by_provider),
    url(r'^clouds/get_security_zone_info/$', cluster_view.get_security_zone_info),

    url(r'^clouds/create_placement/$', cluster_view.create_placement),
    url(r'^clouds/placements/$', cluster_view.get_placements),
    url(r'^clouds/get_placements/$', cluster_view.get_placements_by_provider),
    url(r'^clouds/get_placement_infos/$', cluster_view.get_placement_infos),

    url(r'^env/(?P<src_name>[a-zA-Z0-9\-_]+)/(?P<src_stage>[a-zA-Z0-9\-_]+)/config/clone_cluster/$', cluster_view.clone_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/delete_cluster/$', cluster_view.delete_cluster),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/launch_hosts/$', cluster_view.launch_hosts),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/terminate_hosts/$', cluster_view.terminate_hosts),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/force_terminate_hosts/$', cluster_view.force_terminate_hosts),

    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/enable_replacement/$', cluster_view.enable_cluster_replacement),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/pause_replacement/$', cluster_view.pause_cluster_replacement),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/resume_replacement/$', cluster_view.resume_cluster_replacement),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/cancel_replacement/$', cluster_view.cancel_cluster_replacement),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/replacement/$',
        cluster_view.ClusterHistoriesView.as_view()),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/get_replacement_details/$', cluster_view.cluster_replacement_details),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/get_replacement_progress/$', cluster_view.cluster_replacement_progress),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/replacement/(?P<replacement_id>[a-zA-Z0-9\-_]+)/details/$',
        cluster_view.view_cluster_replacement_details),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/replacement/scaling_activities/$',
        cluster_view.view_cluster_replacement_scaling_activities),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/replacement/(?P<replacement_id>[a-zA-Z0-9\-_]+)/schedule/$',
        cluster_view.view_cluster_replacement_schedule),

    # CMP specific util
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/cluster/capacity/$', cluster_view.ClusterCapacityUpdateView.as_view()),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/newcapacity/$', cluster_view.EnvCapacityBasicCreateView.as_view()),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/newcapacity/advanced/$',
        cluster_view.EnvCapacityAdvCreateView.as_view()),
    url(r'^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/cluster/config/$',
        cluster_view.ClusterConfigurationView.as_view()),
]
