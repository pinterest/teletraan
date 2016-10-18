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
from deploy_board.settings import IS_PINTEREST
import json
import logging

from helpers import baseimages_helper, hosttypes_helper, securityzones_helper, placements_helper
from helpers import clusters_helper, environs_helper, environ_hosts_helper
import common

log = logging.getLogger(__name__)

DEFAULT_PAGE_SIZE = 200
PROVIDER_AWS = 'AWS'
CMP_DOCKER_IMAGE = 'CMP-DOCKER'


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
    env_name = params['env']
    stage_name = params['stage']
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
        'provider': provider,
        'env_name': env_name,
        'stage_name': stage_name,
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


def get_default_cmp_configs(name, stage):
    config_map = {}
    config_map['aws_role'] = 'base'
    config_map['cmp_group'] = 'CMP,{}-{}'.format(name, stage)
    config_map['pinfo_environment'] = 'prod'
    config_map['pinfo_team'] = 'cloudeng'
    config_map['pinfo_role'] = 'cmp_docker'
    return config_map


def parse_cluster_info(request, env_name, env_stage, cluster_name):
    params = request.POST
    cluster_info = {}
    cluster_info['capacity'] = params['capacity']
    cluster_info['baseImageId'] = params['baseImageId']
    cluster_info['provider'] = params['provider']
    cluster_info['hostType'] = params['hostTypeId']
    cluster_info['securityZone'] = params['securityZoneId']
    cluster_info['placement'] = ",".join(params.getlist('placementId'))

    # Update cluster name and isDocker in env
    env_info = {}
    env_info['clusterName'] = cluster_name
    if 'isDocker' in params:
        env_info['isDocker'] = True
    else:
        env_info['isDocker'] = False
    environs_helper.update_env_basic_config(request, env_name, env_stage, data=env_info)
    return cluster_info


# TODO merge cmp functions and templates
def create_cmp_cluster(request, name, stage):
    params = request.POST
    cluster_name = '{}-{}'.format(name, stage)
    cluster_info = parse_cluster_info(request, name, stage, cluster_name)
    config_map = get_default_cmp_configs(name, stage)
    user_data_configs = parse_configs(params)
    config_map.update(user_data_configs)
    cluster_info['configs'] = config_map
    clusters_helper.create_cluster(request, cluster_name, cluster_info)

    # set up env and group relationship
    environs_helper.add_env_capacity(request, name, stage, capacity_type="GROUP", data=cluster_name)
    return get_cmp_cluster(request, name, stage)


def create_cluster(request, name, stage):
    params = request.POST
    cluster_name = '{}-{}'.format(name, stage)
    cluster_name = cluster_name.lower()
    cluster_info = parse_cluster_info(request, name, stage, cluster_name)
    user_data_configs = parse_configs(params)
    cluster_info['configs'] = user_data_configs
    clusters_helper.create_cluster(request, cluster_name, cluster_info)

    # set up env and group relationship
    environs_helper.add_env_capacity(request, name, stage, capacity_type="GROUP", data=cluster_name)
    return get_basic_cluster(request, name, stage)


def update_cmp_cluster(request, name, stage):
    params = request.POST
    cluster_name = common.get_cluster_name(request, name, stage)
    cluster_info = parse_cluster_info(request, name, stage, cluster_name)
    config_map = get_default_cmp_configs(name, stage)
    user_data_configs = parse_configs(params)
    config_map.update(user_data_configs)
    cluster_info['configs'] = config_map
    clusters_helper.update_cluster(request, cluster_name, cluster_info)
    return get_cmp_cluster(request, name, stage)


def update_cluster(request, name, stage):
    params = request.POST
    cluster_name = common.get_cluster_name(request, name, stage)
    cluster_info = parse_cluster_info(request, name, stage, cluster_name)
    user_data_configs = parse_configs(params)
    cluster_info['configs'] = user_data_configs
    clusters_helper.update_cluster(request, cluster_name, cluster_info)
    return get_basic_cluster(request, name, stage)


def get_new_cmp_cluster(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    provider_list = baseimages_helper.get_all_providers(request)
    base_images = baseimages_helper.get_by_name(request, CMP_DOCKER_IMAGE)
    html = render_to_string('clusters/cmp_cluster_creation.tmpl', {
        'env': env,
        'envs': envs,
        'stages': stages,
        'provider_list': provider_list,
        'base_images': base_images,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(html), content_type="application/json")


def get_cmp_cluster(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    provider_list = baseimages_helper.get_all_providers(request)
    basic_cluster_info = clusters_helper.get_cluster(request, env.get('clusterName'))
    base_images = baseimages_helper.get_by_name(request, CMP_DOCKER_IMAGE)
    html = render_to_string('clusters/cmp_cluster.tmpl', {
        'env': env,
        'envs': envs,
        'stages': stages,
        'provider_list': provider_list,
        'basic_cluster_info': basic_cluster_info,
        'base_images': base_images,
        'csrf_token': get_token(request),
    })
    return HttpResponse(json.dumps(html), content_type="application/json")


def get_basic_cluster(request, name, stage):
    envs = environs_helper.get_all_env_stages(request, name)
    stages, env = common.get_all_stages(envs, stage)
    provider_list = baseimages_helper.get_all_providers(request)
    basic_cluster_info = clusters_helper.get_cluster(request, env.get('clusterName'))
    html = render_to_string('clusters/clusters.tmpl', {
        'env': env,
        'envs': envs,
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
    basic_cluster_info = clusters_helper.get_cluster(request, env.get('clusterName'))
    adv = False
    is_cmp = False
    if basic_cluster_info:
        base_image_id = basic_cluster_info.get('baseImageId')
        base_image = baseimages_helper.get_by_id(request, base_image_id)
        if base_image.get('abstract_name') != CMP_DOCKER_IMAGE:
            adv = True
        else:
            is_cmp = True

    params = request.GET
    if params.get('adv'):
        adv = params.get('adv')

    return render(request, 'clusters/clusters.html', {
        'env': env,
        'envs': envs,
        'stages': stages,
        'provider_list': provider_list,
        'basic_cluster_info': basic_cluster_info,
        'adv': adv,
        'is_cmp': is_cmp,
    })


def delete_cluster(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.delete_cluster(request, cluster_name)

    # Update isDocker and cluster name in
    env_info = {}
    env_info['clusterName'] = ''
    env_info['isDocker'] = False
    environs_helper.update_env_basic_config(request, name, stage, data=env_info)

    # Remove group and env relationship
    environs_helper.remove_env_capacity(request, name, stage, capacity_type="GROUP", data=cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def get_aws_config_name_list_by_image(image_name):
    config_map = {}
    config_map['aws_role'] = 'base'
    config_map['assign_public_ip'] = 'true'
    if IS_PINTEREST:
        config_map['pinfo_environment'] = 'prod'
        config_map['raid'] = 'true'
        config_map['raid_mount'] = '/mnt'
        config_map['raid_device'] = '/dev/md0'
        config_map['raid_fs'] = 'xfs'
        config_map['ebs'] = 'true'
        config_map['ebs_size'] = 500
        config_map['ebs_mount'] = '/backup'
        config_map['ebs_volume_type'] = 'gp2'
        if image_name == CMP_DOCKER_IMAGE:
            config_map['pinfo_role'] = 'cmp_docker'
            config_map['pinfo_team'] = 'cloudeng'
        else:
            config_map['pinfo_role'] = ''
            config_map['pinfo_team'] = ''
    return config_map


def get_advanced_cluster(request):
    params = request.GET
    provider = params['provider']
    name = params['env']
    stage = params['stage']
    image_name = params['image_name']
    cluster_name = common.get_cluster_name(request, name, stage)
    config_list = None
    advanced_cluster_info = {}
    if provider == PROVIDER_AWS:
        config_list = get_aws_config_name_list_by_image(image_name)
        basic_cluster_info = clusters_helper.get_cluster(request, cluster_name)
        if not basic_cluster_info:
            if image_name != CMP_DOCKER_IMAGE:
                advanced_cluster_info['aws_role'] = 'base'
            else:
                advanced_cluster_info = get_default_cmp_configs(name, stage)
        else:
            if image_name != CMP_DOCKER_IMAGE:
                advanced_cluster_info = basic_cluster_info.get('configs')
            else:
                cmp_configs = get_default_cmp_configs(name, stage)
                advanced_cluster_info = basic_cluster_info.get('configs')
                for key, value in cmp_configs.iteritems():
                    if advanced_cluster_info.get(key) == value:
                        advanced_cluster_info.pop(key)
    contents = render_to_string('clusters/get_advanced_config.tmpl', {
        'provider': provider,
        'advanced_cluster_info': advanced_cluster_info,
        'config_list': config_list,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def launch_hosts(request, name, stage):
    params = request.POST
    num = int(params['num'])
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.launch_hosts(request, cluster_name, num)
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
    environ_hosts_helper.stop_service_on_host(request, name, stage, host_ids)
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

    if 'replaceHost' in post_params:
        replace_host = True
    else:
        replace_host = False

    cluster_name = common.get_cluster_name(request, name, stage)
    if not cluster_name:
        groups = environs_helper.get_env_capacity(request, name, stage, capacity_type="GROUP")
        for group_name in groups:
            cluster_name = group_name
    clusters_helper.force_terminate_hosts(request, cluster_name, host_ids, replace_host)
    return redirect('/env/{}/{}'.format(name, stage))


def enable_cluster_replacement(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.enable_cluster_replacement(request, cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def pause_cluster_replacement(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.pause_cluster_replacement(request, cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def resume_cluster_replacement(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.resume_cluster_replacement(request, cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def cancel_cluster_replacement(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    clusters_helper.cancel_cluster_replacement(request, cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))
