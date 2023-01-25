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


def create_base_image(request, base_image_info):
    return rodimus_client.post("/base_images", request.teletraan_user_id.token, data=base_image_info)


def get_all(request, index, size):
    params = [('pageIndex', index), ('pageSize', size)]
    return rodimus_client.get("/base_images", request.teletraan_user_id.token, params=params)


def get_all_with_acceptance(request, index, size):
    base_images = get_all(request, index, size)
    fetched_names = set()
    fetched_names_cells = set()
    golden = dict()
    name_acceptance_map = {}
    for img in base_images:
        name = img['abstract_name']
        cell = img['cell_name']
        if name not in fetched_names and name.startswith('cmp_base'):
            fetched_names.add(name)
            base_image_infos = get_acceptance_by_name(request, name,
                                                      img.get('cell', None))
            for img_info in base_image_infos:
                name_acceptance_map[img_info['baseImage'][
                    'provider_name']] = img_info.get('acceptance') or 'UNKNOWN'
        img['acceptance'] = name_acceptance_map.get(img['provider_name'],
                                                    'N/A')

        if name.startswith('cmp_base'):
            if (name, cell) not in fetched_names_cells:
                fetched_names_cells.add((name, cell))
                base_image_info = get_current_golden_image(request, name, cell)
                if base_image_info:
                    golden[(name, cell)] = base_image_info['provider_name']
            if (name, cell) in golden and img['provider_name'] == golden[(name, cell)]:
                img['tag'] = 'current'

    return base_images


def get_image_names(request, provider, cell_name):
    params = [('provider', provider), ('cellName', cell_name)]
    return rodimus_client.get("/base_images/names", request.teletraan_user_id.token, params=params)


def get_image_names_by_arch(request, provider, cell_name, arch_name):
    params = [('provider', provider), ('cellName', cell_name), ('archName', arch_name)]
    return rodimus_client.get("/base_images/names", request.teletraan_user_id.token, params=params)


def get_all_by(request, provider, cell_name):
    if cell_name:
        return rodimus_client.get("/base_images/cell/%s" % cell_name, request.teletraan_user_id.token)
    params = [('provider', provider)]
    return rodimus_client.get("/base_images", request.teletraan_user_id.token, params=params)


def get_by_name(request, name, cell_name):
    params = [('cellName', cell_name)]
    return rodimus_client.get("/base_images/names/%s" % name, request.teletraan_user_id.token, params=params)


def get_acceptance_by_name(request, name, cell_name):
    params = [('cellName', cell_name)]
    return rodimus_client.get("/base_images/acceptances/%s" % name, request.teletraan_user_id.token, params=params)

def get_current_golden_image(request, name, cell):
    return rodimus_client.get("/base_images/names/%s/cells/%s/golden" % (name, cell), request.teletraan_user_id.token)

def get_by_provider_name(request, name):
    return rodimus_client.get("/base_images/provider_names/%s" % name, request.teletraan_user_id.token)


def get_by_id(request, image_id):
    return rodimus_client.get("/base_images/%s" % image_id, request.teletraan_user_id.token)


def get_all_providers(request):
    return rodimus_client.get("/base_images/provider", request.teletraan_user_id.token)
