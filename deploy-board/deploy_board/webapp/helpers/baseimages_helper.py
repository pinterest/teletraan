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

rodimus_client = RodimusClient()

MAX_BASE_IMAGE_UPDATE_EVENTS = 1e4


def promote_image(request, image_id, tag):
    params = [("stage", tag)]
    return rodimus_client.put(
        "/base_images/%s/golden" % image_id,
        request.teletraan_user_id.token,
        params=params,
    )


def demote_image(request, image_id):
    return rodimus_client.delete(
        "/base_images/%s/golden" % image_id, request.teletraan_user_id.token
    )


def cancel_image_update(request, image_id):
    return rodimus_client.put(
        "/base_images/%s/golden/cancel" % image_id, request.teletraan_user_id.token
    )


def get_image_tag_by_id(request, image_id):
    return rodimus_client.get(
        "/base_images/%s/tags" % image_id, request.teletraan_user_id.token
    )


def create_base_image(request, base_image_info):
    return rodimus_client.post(
        "/base_images", request.teletraan_user_id.token, data=base_image_info
    )


def get_all(request, index, size):
    params = [("pageIndex", index), ("pageSize", size)]
    return rodimus_client.get(
        "/base_images", request.teletraan_user_id.token, params=params
    )


def get_all_with_acceptance(request, index, size):
    base_images = get_all(request, index, size)
    golden = dict()
    for img in base_images:
        name = img["abstract_name"]
        cell = img["cell_name"]
        arch = img["arch_name"]

        if name.startswith("cmp_base"):
            key = (name, cell, arch)
            if key not in golden:
                golden_image = get_current_golden_image(request, name, cell, arch)
                golden[key] = golden_image["id"] if golden_image else None
            if img["id"] == golden[key]:
                img["current_golden"] = True

    return base_images


def get_image_names(request, provider, cell_name):
    params = [("provider", provider), ("cellName", cell_name)]
    return rodimus_client.get(
        "/base_images/names", request.teletraan_user_id.token, params=params
    )


def get_image_names_by_arch(request, provider, cell_name, arch_name):
    params = [("provider", provider), ("cellName", cell_name), ("archName", arch_name)]
    return rodimus_client.get(
        "/base_images/names", request.teletraan_user_id.token, params=params
    )


def get_all_by(request, provider, cell_name):
    if cell_name:
        return rodimus_client.get(
            "/base_images/cell/%s" % cell_name, request.teletraan_user_id.token
        )
    params = [("provider", provider)]
    return rodimus_client.get(
        "/base_images", request.teletraan_user_id.token, params=params
    )


def get_by_name(request, name, cell_name, arch_name):
    params = [("cellName", cell_name), ("archName", arch_name)]
    return rodimus_client.get(
        "/base_images/names/%s" % name, request.teletraan_user_id.token, params=params
    )


def get_current_golden_image(request, name, cell, arch):
    return rodimus_client.get(
        "/base_images/names/%s/cells/%s/arches/%s/golden" % (name, cell, arch),
        request.teletraan_user_id.token,
    )


def get_by_provider_name(request, name):
    return rodimus_client.get(
        "/base_images/provider_names/%s" % name, request.teletraan_user_id.token
    )


def get_by_id(request, image_id):
    return rodimus_client.get(
        "/base_images/%s" % image_id, request.teletraan_user_id.token
    )


def get_all_providers(request):
    return rodimus_client.get("/base_images/provider", request.teletraan_user_id.token)


def get_image_update_events_by_new_id(request, image_id):
    events = rodimus_client.get(
        "/base_images/updates/%s" % image_id, request.teletraan_user_id.token
    )

    for event in events:
        event["status"] = generate_image_update_event_status(event)

    return events


# Heuristic way to get the latest update events batch
# TODO: update rodimus for better update events batching
def get_latest_image_update_events(events):
    if not events:
        return events

    # Group update events batch by create_time.
    # Events are sorted by create_time
    # create_time is milisecond timestamp and gets increased by 1 per cluster.
    # The total number of clusters should not be 10K.
    lastest_timestamp = events[0]["create_time"]
    latest_events = [
        event
        for event in events
        if abs(event["create_time"] - lastest_timestamp) < MAX_BASE_IMAGE_UPDATE_EVENTS
    ]

    return latest_events


def get_image_update_events_by_cluster(request, cluster_name):
    events = rodimus_client.get(
        "/base_images/updates/cluster/%s" % cluster_name,
        request.teletraan_user_id.token,
    )
    for event in events:
        event["status"] = generate_image_update_event_status(event)
    return events


def get_latest_succeeded_image_update_event_by_cluster(request, cluster_name):
    events = rodimus_client.get(
        "/base_images/updates/cluster/%s" % cluster_name,
        request.teletraan_user_id.token,
    )
    events = filter(
        lambda x: x["state"] == "COMPLETED" and x["finish_time"] is not None, events
    )
    return max(events, key=lambda x: x["finish_time"], default=None)


def generate_image_update_event_status(event):
    if event["state"] == "INIT":
        if event["start_time"]:
            return "UPDATING"
        else:
            return "INIT"
    elif event["state"] == "COMPLETED":
        if event["error_message"]:
            return "FAILED"
        else:
            return "SUCCEEDED"
    return event["state"]


def get_base_image_update_progress(events):
    if not events:
        return None

    total = len(events)
    succeeded = len([event for event in events if event["status"] == "SUCCEEDED"])
    state = (
        "COMPLETED"
        if all(event["state"] == "COMPLETED" for event in events)
        else "IN PROGRESS"
    )
    success_rate = succeeded * 100 / total

    return {
        "state": state,
        "total": total,
        "succeeded": succeeded,
        "progressTip": "Among total {} clusters, {} successfully updated, {} failed or are pending.".format(
            total, succeeded, total - succeeded
        ),
        "successRatePercentage": success_rate,
        "successRate": "{}% ({}/{})".format(success_rate, succeeded, total),
    }
