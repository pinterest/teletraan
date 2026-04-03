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
from django.urls import re_path
from . import alarm_views
from . import accounts_views
from . import capacity_views
from . import config_map_views
from . import env_config_views
from . import env_views
from . import metrics_views
from . import promote_views
from . import webhook_views
from . import deploy_views
from . import build_views
from . import host_views
from . import util_views
from . import security
from . import user_views
from . import docs_views
from . import cluster_view
from . import schedule_views
from . import host_tags_views

urlpatterns = [
    # deploy related
    re_path(r"^deploy/inline_update/$", deploy_views.inline_update),
    re_path(r"^deploy/(?P<deploy_id>[a-zA-Z0-9\-_]+)", deploy_views.DeployView.as_view()),
    re_path(r"^deploys/ongoing/$", deploy_views.get_ongoing_deploys),
    re_path(r"^deploys/ongoing_sidecar/$", deploy_views.get_ongoing_sidecar_deploys),
    re_path(r"^deploys/dailycount", deploy_views.get_daily_deploy_count),
    re_path(r"^env/create/$", env_views.post_create_env),
    # envs related
    re_path(r"^envs/recent/$", env_views.get_recent_envs),
    re_path(r"^envs/names/$", env_views.get_env_names),
    re_path(r"^envs/deploys/$", env_views.get_all_deploys),
    re_path(r"^envs/search/(?P<filter>[a-zA-Z0-9\-_]+)/$", env_views.search_envs),
    re_path(r"^envs/search/$", env_views.EnvListView.as_view()),
    re_path(r"^envs/$", env_views.EnvListView.as_view()),
    re_path(r"^envs/enable/$", env_views.enable_all_env_change),
    re_path(r"^envs/disable/$", env_views.disable_all_env_change),
    re_path(r"^envs/get_tag_message/$", env_views.get_tag_message),
    # env related
    re_path(r"^env/(?P<name>[a-zA-Z0-9\-_]+)/add_stage/$", env_views.post_add_stage),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/get_users_config/$",
        user_views.get_users_config,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/update_users_config/$",
        user_views.update_users_config,
    ),
    re_path(r"^env/(?P<name>[a-zA-Z0-9\-_]+)/get_users/$", user_views.get_users),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/get_user_token/(?P<user_name>[a-zA-Z0-9\-_]+)/$",
        user_views.get_user_token,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/deploy/$",
        env_views.EnvLandingView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/$",
        env_views.EnvLandingView.as_view(),
    ),
    re_path(r"^env/(?P<name>[a-zA-Z0-9\-_]+)/$", env_views.EnvLandingView.as_view()),
    # stage related
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/new_deploy/$",
        env_views.EnvNewDeployView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/update_deploy_progress/$",
        env_views.update_deploy_progress,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/check_log_status/$",
        env_views.logging_status,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/check_log_status_action/$",
        env_views.check_logging_status,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/update_service_add_ons/$",
        env_views.update_service_add_ons,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/deploys/$",
        env_views.get_env_deploys,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/new_commits/$",
        env_views.get_new_commits,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/compare_deploys/$",
        env_views.compare_deploys,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/compare_deploys_2/$",
        env_views.compare_deploys_2,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/pred_deploys/$",
        env_views.get_pred_deploys,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/(?P<buildId>[a-zA-Z0-9\-_]+)"
        r"/get_duplicate_commit_message/$",
        deploy_views.get_duplicate_commit_deploy_message,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/"
        r"(?P<buildId>[a-zA-Z0-9\-_]+)/warn_for_deploy/$",
        env_views.warn_for_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/builds/$",
        env_views.get_builds,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/groups/$",
        env_views.get_groups,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/enable/$",
        env_views.enable_env_change,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/disable/$",
        env_views.disable_env_change,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/restart/$",
        env_views.restart,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/pause/$",
        env_views.pause,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/resume/$",
        env_views.resume,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/rollback/$",
        env_views.rollback,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/rollback_to/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)",
        env_views.rollback_to,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/promote_to/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)$",
        env_views.promote_to,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/promote/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)$",
        env_views.promote,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/build/"
        r"(?P<build_id>[a-zA-Z0-9\-_]+)",
        env_views.deploy_build,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/commit/"
        r"(?P<commit>[a-zA-Z0-9\-_]+)",
        env_views.deploy_commit,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/deploy/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)",
        env_views.get_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/get_service_metrics/$",
        util_views.get_service_metrics,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/get_service_alarms/$",
        util_views.get_service_alarms,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config_history/$",
        env_views.get_env_config_history,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/get_config_comparison/",
        env_views.get_config_comparison,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/show_config_comparison/",
        env_views.show_config_comparison,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/remove/$",
        env_views.remove_stage,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/$",
        env_views.get_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/update_schedule/$",
        env_views.update_schedule,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/delete_schedule/$",
        env_views.delete_schedule,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/schedule/$",
        env_views.get_deploy_schedule,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/override_session/$",
        env_views.override_session,
    ),
    # environment configs
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/$",
        env_config_views.EnvConfigView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/auto_deploy/$",
        promote_views.EnvPromoteConfigView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/capacity/$",
        capacity_views.EnvCapacityConfigView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/map/$",
        config_map_views.EnvConfigMapView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/alarm/$",
        alarm_views.EnvAlarmView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/metrics/$",
        metrics_views.EnvMetricsView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/webhooks/$",
        webhook_views.EnvWebhooksView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/config/schedule/$",
        schedule_views.EnvScheduleView.as_view(),
    ),
    # host tags related
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/constraint/",
        host_tags_views.HostTagsView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/host_tags/(?P<tag_name>[a-zA-Z0-9\-:_]+)",
        host_tags_views.get_host_tags_progress,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/host_ec2_tags",
        host_tags_views.get_host_ec2tags,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/add_constraint",
        host_tags_views.add_constraint,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/edit_constraint",
        host_tags_views.edit_constraint,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/remove_constraint",
        host_tags_views.remove_constraint,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/deploy_constraint",
        host_tags_views.get_constraint,
    ),
    # host related
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/unknowns/$",
        env_views.get_unknown_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/provision/$",
        env_views.get_provisioning_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/all/$",
        env_views.get_all_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/failed/$",
        env_views.get_failed_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/hosts/sub_account/$",
        env_views.get_sub_account_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)/hosts/$",
        env_views.get_hosts_by_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/reset_failed_hosts/"
        r"(?P<deploy_id>[a-zA-Z0-9\-_]+)/$",
        env_views.reset_failed_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/reset_deploy/"
        r"(?P<host_id>[a-zA-Z0-9\-_]+)/$",
        env_views.reset_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/pause_deploy/"
        r"(?P<host_id>[a-zA-Z0-9\-_]+)/$",
        env_views.pause_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/resume_deploy/"
        r"(?P<host_id>[a-zA-Z0-9\-_]+)/$",
        env_views.resume_deploy,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/reset_hosts/$",
        env_views.reset_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/pause_hosts/$",
        env_views.pause_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/resume_hosts/$",
        env_views.resume_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/host/(?P<hostname>[a-zA-Z0-9\-_]+)",
        host_views.HostDetailView.as_view(),
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/terminate_hosts/$",
        cluster_view.terminate_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/force_terminate_hosts/$",
        cluster_view.force_terminate_hosts,
    ),
    re_path(
        r"^env/(?P<name>[a-zA-Z0-9\-_]+)/(?P<stage>[a-zA-Z0-9\-_]+)/reset_environments/(?P<host_id>[a-zA-Z0-9\-_]+)$",
        env_views.reset_all_environments,
    ),
    re_path(r"^hosts/$", host_views.hosts_list),
    # builds related
    re_path(r"^builds/get_all_builds/$", build_views.get_all_builds),
    re_path(
        r"^builds/search_commit/(?P<commit>[a-zA-Z0-9\-_]+)/$",
        build_views.search_commit,
    ),
    re_path(r"^builds/names/(?P<name>[a-zA-Z0-9\-_.]+)/builds/$", build_views.list_builds),
    re_path(
        r"^builds/names/(?P<name>[a-zA-Z0-9\-_.]+)/branches/$",
        build_views.list_build_branches,
    ),
    re_path(r"^builds/names/$", build_views.get_build_names),
    re_path(r"^builds/(?P<id>[a-zA-Z0-9\-_]+)/$", build_views.get_build),
    re_path(r"^builds/(?P<id>[a-zA-Z0-9\-_]+)/tags/$", build_views.tag_build),
    re_path(r"^builds/$", build_views.builds_landing),
    # Commits related
    re_path(r"^commits/compare_commits/$", build_views.compare_commits),
    re_path(
        r"^commits/compare_commits_datatables/$", build_views.compare_commits_datatables
    ),
    re_path(r"^commits/get_more_commits/$", build_views.get_more_commits),
    # Accounts related
    re_path(r"^clouds/accounts/$", accounts_views.AccountsView.as_view()),
    re_path(
        r"^clouds/accounts/(?P<provider>[a-zA-Z0-9\-]+)/(?P<cell>[a-zA-Z0-9\-]+)/(?P<id>[a-zA-Z0-9\-]+)/",
        accounts_views.AccountDetailsView.as_view(),
    ),
    # metrics
    re_path(r"^get_site_health_metrics/$", util_views.get_site_health_metrics),
    re_path(r"^validate_metrics_url/$", util_views.validate_metrics_url),
    # mix
    re_path(r"^health_check/$", util_views.health_check),
    re_path(r"^auth/$", security.login_authorized),
    re_path(r"^logout/$", security.logout),
    re_path(r"^loggedout/$", util_views.loggedout),
    re_path(r"^api-docs/$", docs_views.SwaggerUIView.as_view()),
    re_path(r"^$", deploy_views.get_landing_page),
]
