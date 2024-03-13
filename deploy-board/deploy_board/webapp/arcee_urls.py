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
from . import group_view, host_views
from . import util_views

urlpatterns = [
    # auto scaling alarms endpoint
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/create_group/$',
        group_view.create_launch_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/add_metrics/$',
        group_view.add_alarms),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_metrics/$',
        group_view.update_alarms),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/delete_metrics/$',
        group_view.delete_alarms),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/config_history/$', group_view.get_config_history),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/get_config_comparison/',
        group_view.get_config_comparison),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/show_config_comparison/',
        group_view.show_config_comparison),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/group_info/$', group_view.get_group_info),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/group_size/$', group_view.get_group_size),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/scaling_activity/$',
        group_view.get_scaling_activities),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/scaling_activities/$',
        group_view.ScalingActivityView.as_view()),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/more_scaling_activities/$',
        group_view.get_more_scaling_activities),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/attach_instance/$', group_view.attach_instances),
    url(r'^aws_info/get_ami_ids/$', group_view.get_aws_settings),
    url(r'^aws_info/get_configs/$', group_view.get_configs),
    url(r'^specs/get_sgs/$', group_view.get_sg_settings),
    url(r'^specs/get_types/$', group_view.get_instance_type_settings),
    url(r'^specs/get_subnets/$', group_view.get_subnets_settings),
    url(r'^group_latency_stats/(?P<group_name>[a-zA-Z0-9\-_]+)/$', util_views.get_latency_metrics),
    url(r'^group_launch_rate/(?P<group_name>[a-zA-Z0-9\-_]+)/$', util_views.get_launch_rate),
    url(r'^pas_stats/(?P<group_name>[a-zA-Z0-9\-_]+)/$', util_views.get_pas_metrics),


    # instances related
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/add_instance/$', group_view.add_instance),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/instance_action/', group_view.instance_action_in_asg),

    # health check related
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/health_check_activities/$', group_view.get_health_check_activities),
    url(r'^groups/health_check/(?P<id>[a-zA-Z0-9\-_]+)$', group_view.get_health_check_details),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/manually_health_check/$', group_view.create_manually_health_check),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/scaling_down/enable/$', group_view.enable_scaling_down_event),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/scaling_down/disable/$', group_view.disable_scaling_down_event),

    # groups related
    url(r'^groups/$', group_view.group_landing),
    url(r'^groups/names', group_view.get_group_names),
    url(r'^groups/search/(?P<group_name>[a-zA-Z0-9\-_]+)/$', group_view.search_groups),
    url(r'^groups/search/$', group_view.group_landing),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/host-az-dist/$', group_view.get_host_az_dist),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/host-ami-dist/$', group_view.get_host_ami_dist),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/$', group_view.GroupDetailView.as_view()),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/config/$', group_view.GroupConfigView.as_view()),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/envs/', group_view.get_envs),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/gen_asg_setting', group_view.gen_asg_setting),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/disable_asg/$', group_view.disable_asg),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/resume_asg/$', group_view.resume_asg),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/create_asg/$', group_view.create_asg),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/delete_asg/$', group_view.delete_asg),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/get_deleted_asg_status/$',
        group_view.get_deleted_asg_status),
    url(r'^groups/(?P<groupname>[a-zA-Z0-9\-_]+)/host/(?P<hostname>[a-zA-Z0-9\-_]+)', host_views.GroupHostDetailView.as_view()),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_launch_config/$',
        group_view.get_launch_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_asg_config/$',
        group_view.get_asg_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_asg_policy/$',
        group_view.get_policy),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_metrics/$',
        group_view.get_alarms),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/create_launch_config/$',
        group_view.create_launch_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_launch_config/$',
        group_view.update_launch_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_group_config/$',
        group_view.update_group_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_config/$',
        group_view.update_asg_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_policy/$',
        group_view.update_policy),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/policies/(?P<policy_name>[a-zA-Z0-9\-_]+)$',
        group_view.delete_policy),    
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_pas_config/$',
        group_view.update_pas_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_pas_config/$',
        group_view.get_pas_config),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_scheduled_actions/$',
        group_view.get_scheduled_actions),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/get_suspended_processes/$',
        group_view.get_suspended_processes),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/suspend_process/(?P<process_name>[a-zA-Z\-_]+)$',
        group_view.suspend_process),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/resume_process/(?P<process_name>[a-zA-Z\-_]+)$',
        group_view.resume_process),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/delete_scheduled_actions/$',
        group_view.delete_scheduled_actions),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/add_scheduled_actions/$',
        group_view.add_scheduled_actions),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/autoscaling/update_scheduled_actions/$',
        group_view.update_scheduled_actions),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/terminate/all/$', group_view.terminate_all_hosts),
    url(r'^groups/(?P<group_name>[a-zA-Z0-9\-_]+)/hosts', group_view.get_terminating_hosts_by_group),
]
