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

from deploy_board.webapp.helpers.rodimus_client import RodimusClient


class BlessedStatusValues:
    ONBOARDING = ("ONBOARDING",)
    SUSTAINING = ("SUSTAINING",)
    DECOMMISSIONING = "DECOMMISSIONING"

    @classmethod
    def get_all_statuses(cls):
        return [
            item
            for item in cls.__dict__
            if not callable(getattr(cls, item)) and not item.startswith("__")
        ]


rodimus_client = RodimusClient()


def create_host_type(request, host_type_info):
    return rodimus_client.post(
        "/host_types", request.teletraan_user_id.token, data=host_type_info
    )


def modify_host_type(request, host_type_id, host_type_info):
    return rodimus_client.put(
        "/host_types/%s" % host_type_id,
        request.teletraan_user_id.token,
        data=host_type_info,
    )


def get_all(request, index, size):
    params = [("pageIndex", index), ("pageSize", size)]
    return rodimus_client.get(
        "/host_types", request.teletraan_user_id.token, params=params
    )


def get_by_provider(request, provider):
    return rodimus_client.get(
        "/host_types/provider/%s" % provider, request.teletraan_user_id.token
    )


def get_by_arch(request, arch):
    return rodimus_client.get(
        "/host_types/arch/%s" % arch, request.teletraan_user_id.token
    )


def get_by_id(request, host_type_id):
    return rodimus_client.get(
        "/host_types/%s" % host_type_id, request.teletraan_user_id.token
    )
