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

from django.middleware.csrf import get_token
from django.shortcuts import render, redirect
from django.template.loader import render_to_string
from django.http import HttpResponse
from django.contrib import messages
import json
import logging

from helpers import baseimages_helper, hosttypes_helper, securityzones_helper, placements_helper
from helpers import clusters_helper, environs_helper
import common

log = logging.getLogger(__name__)

DEFAULT_PAGE_SIZE = 200
PROVIDER_AWS = 'AWS'


def create_base_image(request):
    params = request.POST
    base_image_info = {}
    base_image_info['abstract_name'] = params['abstractName']
    base_image_info['provider_name'] = params['providerName']
    base_image_info['provider'] = params['provider']
    base_image_info['description'] = params['description']
    if 'basic' in params:
        base_image_info['basic'] = True
    else:
        base_image_info['basic'] = False
    baseimages_helper.create_base_image(request, base_image_info)
    return redirect('/clouds/baseimages')


def get_base_images(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    base_images = baseimages_helper.get_all(request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)

    return render(request, 'clusters/base_images.html', {
        'base_images': base_images,
        'provider_list': provider_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(base_images) < DEFAULT_PAGE_SIZE,
    })


def get_image_names(request):
    params = request.GET
    provider = params['provider']
    image_names = baseimages_helper.get_image_names(request, provider)
    curr_image_name = None
    curr_base_image = None
    if 'curr_base_image' in params:
        curr_base_image = params['curr_base_image']
        image = baseimages_helper.get_by_id(request, curr_base_image)
        curr_image_name = image.get('abstract_name')

    contents = render_to_string("clusters/get_image_name.tmpl", {
        'image_names': image_names,
        'curr_image_name': curr_image_name,
        'curr_base_image': curr_base_image,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_base_images_by_name(request):
    params = request.GET
    base_images = None
    if 'name' in params:
        name = params['name']
        base_images = baseimages_helper.get_by_name(request, name)

    curr_base_image = None
    if 'curr_base_image' in params:
        curr_base_image = params['curr_base_image']
        image = baseimages_helper.get_by_id(request, curr_base_image)
        curr_image_name = image.get('abstract_name')
        base_images = baseimages_helper.get_by_name(request, curr_image_name)

    contents = render_to_string("clusters/get_base_image.tmpl", {
        'base_images': base_images,
        'curr_base_image': curr_base_image,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_base_image_info(request):
    base_images = baseimages_helper.get_by_name(request, request.GET.get('name'))
    contents = render_to_string("clusters/get_base_image_info.tmpl", {
        'base_images': base_images,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def create_host_type(request):
    params = request.POST
    host_type_info = {}
    host_type_info['abstract_name'] = params['abstractName']
    host_type_info['provider_name'] = params['providerName']
    host_type_info['provider'] = params['provider']
    host_type_info['description'] = params['description']
    host_type_info['mem'] = float(params['mem']) * 1024
    host_type_info['core'] = int(params['core'])
    host_type_info['storage'] = params['storage']
    if 'basic' in params:
        host_type_info['basic'] = True
    else:
        host_type_info['basic'] = False
    hosttypes_helper.create_host_type(request, host_type_info)
    return redirect('/clouds/hosttypes')


def get_host_types(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    host_types = hosttypes_helper.get_all(request, index, size)
    for host_type in host_types:
        host_type['mem'] = float(host_type['mem']) / 1024
    provider_list = baseimages_helper.get_all_providers(request)

    return render(request, 'clusters/host_types.html', {
        'host_types': host_types,
        'provider_list': provider_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(host_types) < DEFAULT_PAGE_SIZE,
    })


def get_host_types_by_provider(request):
    params = request.GET
    provider = params['provider']
    curr_host_type = None
    if 'curr_host_type' in params:
        curr_host_type = params['curr_host_type']

    host_types = hosttypes_helper.get_by_provider(request, provider)
    for host_type in host_types:
        host_type['mem'] = float(host_type['mem']) / 1024

    contents = render_to_string("clusters/get_host_type.tmpl", {
        'host_types': host_types,
        'curr_host_type': curr_host_type,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_host_type_info(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    host_types = hosttypes_helper.get_all(request, index, size)
    for host_type in host_types:
        host_type['mem'] = float(host_type['mem']) / 1024
    contents = render_to_string("clusters/get_host_type_info.tmpl", {
        'host_types': host_types,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def create_security_zone(request):
    params = request.POST
    security_zone_info = {}
    security_zone_info['abstract_name'] = params['abstractName']
    security_zone_info['provider_name'] = params['providerName']
    security_zone_info['provider'] = params['provider']
    security_zone_info['description'] = params['description']
    if 'basic' in params:
        security_zone_info['basic'] = True
    else:
        security_zone_info['basic'] = False
    securityzones_helper.create_security_zone(request, security_zone_info)
    return redirect('/clouds/securityzones')


def get_security_zones(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    security_zones = securityzones_helper.get_all(request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)

    return render(request, 'clusters/security_zones.html', {
        'security_zones': security_zones,
        'provider_list': provider_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(security_zones) < DEFAULT_PAGE_SIZE,
    })


def get_security_zones_by_provider(request):
    params = request.GET
    provider = params['provider']
    curr_security_zone = None
    if 'curr_security_zone' in params:
        curr_security_zone = params['curr_security_zone']

    security_zones = securityzones_helper.get_by_provider(request, provider)
    contents = render_to_string("clusters/get_security_zone.tmpl", {
        'security_zones': security_zones,
        'curr_security_zone': curr_security_zone,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_security_zone_info(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    security_zones = securityzones_helper.get_all(request, index, size)
    contents = render_to_string("clusters/get_security_zone_info.tmpl", {
        'security_zones': security_zones,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def create_placement(request):
    params = request.POST
    placement_info = {}
    placement_info['abstract_name'] = params['abstractName']
    placement_info['provider_name'] = params['providerName']
    placement_info['provider'] = params['provider']
    placement_info['description'] = params['description']
    if 'basic' in params:
        placement_info['basic'] = True
    else:
        placement_info['basic'] = False
    placements_helper.create_placement(request, placement_info)
    return redirect('/clouds/placements')


def get_placements(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    placements = placements_helper.get_all(request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)

    return render(request, 'clusters/placements.html', {
        'placements': placements,
        'provider_list': provider_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(placements) < DEFAULT_PAGE_SIZE,
    })


def get_placements_by_provider(request):
    params = request.GET
    provider = params['provider']
    curr_placement_arrays = None
    if 'curr_placement' in params:
        curr_placement = params['curr_placement']
        curr_placement_arrays = curr_placement.split(',')

    placements = placements_helper.get_by_provider(request, provider)
    contents = render_to_string("clusters/get_placement.tmpl", {
        'placements': placements,
        'curr_placement_arrays': curr_placement_arrays,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_placement_infos(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    placements = placements_helper.get_all(request, index, size)
    contents = render_to_string("clusters/get_placement_infos.tmpl", {
        'placements': placements,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def parse_configs(query_dict):
    configs = {}
    for key, value in query_dict.iteritems():
        if not value:
            continue
        if key.startswith('TELETRAAN_'):
            name = key[len('TELETRAAN_'):]
            configs[name] = value
    return configs


def create_cluster(request, name, stage):
    params = request.POST
    cluster_info = {}
    cluster_info['capacity'] = params['capacity']
    cluster_info['base_image_id'] = params['baseImageId']
    cluster_info['provider'] = params['provider']
    cluster_info['host_type_id'] = params['hostTypeId']
    cluster_info['security_zone_id'] = params['securityZoneId']
    cluster_info['placement_id'] = ",".join(params.getlist('placementId'))

    user_data_configs = parse_configs(params)
    if 'assignPublicIp' in params:
        user_data_configs['cmp_public_ip'] = 'yes'

    if 'role' in params and params['role'] != '':
        user_data_configs['cmp_role'] = params['role']
    else:
        user_data_configs['cmp_role'] = 'base'
    config_id = clusters_helper.update_advanced_configs(request, name, stage, user_data_configs)
    cluster_info['config_id'] = config_id
    clusters_helper.create_cluster(request, name, stage, cluster_info)

    env_info = {}
    if 'isDocker' in params:
        env_info['isDocker'] = True
    else:
        env_info['isDocker'] = False
    environs_helper.update_env_basic_config(request, name, stage, data=env_info)
    return get_basic_cluster(request, name, stage)


def update_cluster(request, name, stage):
    params = request.POST
    cluster_info = {}
    cluster_info['capacity'] = params['capacity']
    cluster_info['base_image_id'] = params['baseImageId']
    cluster_info['provider'] = params['provider']
    cluster_info['host_type_id'] = params['hostTypeId']
    cluster_info['security_zone_id'] = params['securityZoneId']
    cluster_info['placement_id'] = ",".join(params.getlist('placementId'))
    cluster_info['config_id'] = params['configId']

    user_data_configs = parse_configs(params)
    if 'assignPublicIp' in params:
        user_data_configs['cmp_public_ip'] = 'yes'

    if 'role' in params and params['role'] != '':
        user_data_configs['cmp_role'] = params['role']

    if user_data_configs:
        clusters_helper.update_advanced_configs(request, name, stage, user_data_configs)
    clusters_helper.update_cluster(request, name, stage, cluster_info)

    env_info = {}
    if 'isDocker' in params:
        env_info['isDocker'] = True
    else:
        env_info['isDocker'] = False
    environs_helper.update_env_basic_config(request, name, stage, data=env_info)
    return get_basic_cluster(request, name, stage)


def get_basic_cluster(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    provider_list = baseimages_helper.get_all_providers(request)
    basic_cluster_info = clusters_helper.get_cluster(request, name, stage)

    html = render_to_string('clusters/clusters.tmpl', {
        'env': env,
        'stages': stages,
        'provider_list': provider_list,
        'basic_cluster_info': basic_cluster_info,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(html), content_type="application/json")


def get_cluster(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    provider_list = baseimages_helper.get_all_providers(request)
    basic_cluster_info = clusters_helper.get_cluster(request, name, stage)

    return render(request, 'clusters/clusters.html', {
        'env': env,
        'stages': stages,
        'provider_list': provider_list,
        'basic_cluster_info': basic_cluster_info,
    })


def delete_cluster(request, name, stage):
    clusters_helper.delete_cluster(request, name, stage)
    return redirect('/env/{}/{}/config/clusters/'.format(name, stage))


def get_advanced_cluster(request):
    params = request.GET
    provider = params['provider']
    adv = int(params['adv'])
    name = params['env']
    stage = params['stage']
    advanced_cluster_info = None
    if adv == 1 and provider == PROVIDER_AWS:
        advanced_cluster_info = {}
        config_maps = clusters_helper.get_advanced_configs(request, name, stage)
        if config_maps:
            if config_maps.get('cmp_role'):
                advanced_cluster_info['role'] = config_maps.get('cmp_role')
                config_maps.pop('cmp_role', None)
            if config_maps.get('cmp_public_ip') == 'yes':
                advanced_cluster_info['assignPublicIp'] = True
                config_maps.pop('cmp_public_ip', None)
            if config_maps:
                advanced_cluster_info['userDataConfigs'] = config_maps
        else:
            advanced_cluster_info['role'] = 'base'
    contents = render_to_string('clusters/get_advanced_config.tmpl', {
        'provider': provider,
        'advanced_cluster_info': advanced_cluster_info,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def launch_hosts(request, name, stage):
    params = request.POST
    num = int(params['num'])
    basic_cluster_info = clusters_helper.get_cluster(request, name, stage)
    cluster_capacity = 0
    if basic_cluster_info:
        placement_id_str = basic_cluster_info.get('placement_id')
        placement_ids = placement_id_str.split(',')
        for placement_id in placement_ids:
            placement_info = placements_helper.get_by_id(request, placement_id)
            cluster_capacity += placement_info.get('capacity')

    if num < cluster_capacity:
        clusters_helper.launch_hosts(request, name, stage, num)
    else:
        content = 'The placement capacity is full. ' \
                  'Please contact your friendly Teletraan owners for immediate assistance!'
        messages.add_message(request, messages.ERROR, content)
    return redirect('/env/{}/{}/'.format(name, stage))


def terminate_hosts(request, name, stage):
    get_params = request.GET
    post_params = request.POST
    host_ids = None
    if 'host_id' in get_params:
        host_ids = [get_params.get('host_id')]

    if 'hostIds' in post_params:
        hosts_str = post_params['hostIds']
        host_ids = [x.strip() for x in hosts_str.split(',')]
    clusters_helper.terminate_hosts(request, name, stage, host_ids)
    return redirect('/env/{}/{}'.format(name, stage))


def force_terminate_hosts(request, name, stage):
    get_params = request.GET
    post_params = request.POST
    host_ids = None
    if 'host_id' in get_params:
        host_ids = [get_params.get('host_id')]

    if 'hostIds' in post_params:
        hosts_str = post_params['hostIds']
        host_ids = [x.strip() for x in hosts_str.split(',')]
    clusters_helper.force_terminate_hosts(request, name, stage, host_ids)
    return redirect('/env/{}/{}'.format(name, stage))


def enable_cluster_replacement(request, name, stage):
    clusters_helper.enable_cluster_replacement(request, name, stage)
    return redirect('/env/{}/{}/config/clusters/'.format(name, stage))


def pause_cluster_replacement(request, name, stage):
    clusters_helper.pause_cluster_replacement(request, name, stage)
    return redirect('/env/{}/{}/config/clusters/'.format(name, stage))


def resume_cluster_replacement(request, name, stage):
    clusters_helper.resume_cluster_replacement(request, name, stage)
    return redirect('/env/{}/{}/config/clusters/'.format(name, stage))


def cancel_cluster_replacement(request, name, stage):
    clusters_helper.cancel_cluster_replacement(request, name, stage)
    return redirect('/env/{}/{}/config/clusters/'.format(name, stage))
