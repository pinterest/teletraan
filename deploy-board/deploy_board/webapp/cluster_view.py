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
from django.views.generic import View

from deploy_board.settings import IS_PINTEREST
if IS_PINTEREST:
    from deploy_board.settings import DEFAULT_PROVIDER, DEFAULT_CMP_IMAGE
import json
import logging

from helpers import baseimages_helper, hosttypes_helper, securityzones_helper, placements_helper, \
    autoscaling_groups_helper
from helpers import clusters_helper, environs_helper, environ_hosts_helper
import common

log = logging.getLogger(__name__)

DEFAULT_PAGE_SIZE = 200

class EnvCapacityBasicCreateView(View):
    def get(self, request, name, stage):
        host_types = hosttypes_helper.get_by_provider(request, DEFAULT_PROVIDER)
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        security_zones = securityzones_helper.get_by_provider(request, DEFAULT_PROVIDER)
        placements = placements_helper.get_by_provider(request, DEFAULT_PROVIDER)
        default_base_image = baseimages_helper.get_by_name(request,DEFAULT_CMP_IMAGE)
        env = environs_helper.get_env_by_stage(request, name, stage)

        capacity_creation_info = {
            'environment': env,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'baseImages': default_base_image,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER
        }
        # cluster manager
        return render(request, 'configs/new_capacity.html', {
            'env': env,
            'capacity_creation_info': json.dumps(capacity_creation_info)})

    def post(self, request, name, stage):
        log.info("Post to capacity with data {0}".format(request.body))
        try:
            cluster_name = '{}-{}'.format(name, stage)
            cluster_info = json.loads(request.body)

            log.info("Create Capacity in the provider")
            clusters_helper.create_cluster(request, cluster_name, cluster_info)

            log.info("Associate cluster_name to environment")
            # Update cluster info
            environs_helper.update_env_basic_config(request, name, stage, data={"clusterName": cluster_name})

            log.info("Update capacity to the environment")
            # set up env and group relationship
            environs_helper.add_env_capacity(request, name, stage, capacity_type="GROUP", data=cluster_name)
            return HttpResponse("{}", content_type="application/json")
        except Exception as e:
            log.info("Have an error {}".format(e))
            return HttpResponse(e, status=500, content_type="application/json")


class EnvCapacityAdvCreateView(View):
    def get(self, request, name, stage):
        host_types = hosttypes_helper.get_by_provider(request, DEFAULT_PROVIDER)
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        security_zones = securityzones_helper.get_by_provider(request, DEFAULT_PROVIDER)
        placements = placements_helper.get_by_provider(request, DEFAULT_PROVIDER)
        base_images = baseimages_helper.get_by_name(request, DEFAULT_CMP_IMAGE)
        base_images_names = baseimages_helper.get_image_names(request, DEFAULT_PROVIDER)

        env = environs_helper.get_env_by_stage(request, name, stage)
        provider_list = baseimages_helper.get_all_providers(request)

        capacity_creation_info = {
            'environment': env,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'baseImages': base_images,
            'baseImageNames': base_images_names,
            'defaultBaseImage': DEFAULT_CMP_IMAGE,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER,
            'providerList': provider_list,
            'configList': get_aws_config_name_list_by_image(DEFAULT_CMP_IMAGE)
        }
        # cluster manager
        return render(request, 'configs/new_capacity_adv.html', {
            'env': env,
            'capacity_creation_info': json.dumps(capacity_creation_info)})

    def post(self, request, name, stage):
        log.info("Post to capacity with data {0}".format(request.body))
        try:
            cluster_name = '{}-{}'.format(name, stage)
            cluster_info = json.loads(request.body)

            log.info("Create Capacity in the provider")
            clusters_helper.create_cluster(request, cluster_name, cluster_info)

            log.info("Update cluster_name to environment")
            # Update environment
            environs_helper.update_env_basic_config(request, name, stage,
                                                    data={"clusterName": cluster_name, "IsDocker": True})

            return HttpResponse("{}", content_type="application/json")
        except Exception as e:
            log.info("Have an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")


class ClusterConfigurationView(View):
    def get(self, request, name, stage):

        cluster_name = '{}-{}'.format(name, stage)
        current_cluster = clusters_helper.get_cluster(request, cluster_name)
        host_types = hosttypes_helper.get_by_provider(request, DEFAULT_PROVIDER)
        current_image = baseimages_helper.get_by_id(request, current_cluster['baseImageId'])
        current_cluster['baseImageName'] = current_image['abstract_name']
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        security_zones = securityzones_helper.get_by_provider(request, current_cluster['provider'])
        placements = placements_helper.get_by_provider(request, current_cluster['provider'])
        base_images = baseimages_helper.get_by_name(request, current_image['abstract_name'])
        base_images_names = baseimages_helper.get_image_names(request, current_cluster['provider'])


        env = environs_helper.get_env_by_stage(request, name, stage)
        provider_list = baseimages_helper.get_all_providers(request)

        capacity_creation_info = {
            'environment': env,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'baseImages': base_images,
            'baseImageNames': base_images_names,
            'defaultBaseImage': DEFAULT_CMP_IMAGE,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER,
            'providerList': provider_list,
            'configList': get_aws_config_name_list_by_image(DEFAULT_CMP_IMAGE),
            'currentCluster': current_cluster
        }
        return render(request, 'clusters/cluster_configuration.html', {
            'env': env,
            'capacity_creation_info': json.dumps(capacity_creation_info)})

    def post(self, request, name, stage):
        try:
            env = environs_helper.get_env_by_stage(request, name, stage)
            cluster_name = env.get('clusterName')
            cluster_info = json.loads(request.body)
            log.info("Update Cluster Configuration with {}", cluster_info)
            image = baseimages_helper.get_by_id(request, cluster_info['baseImageId'])
            clusters_helper.update_cluster(request, cluster_name, cluster_info)
        except Exception as e:
            log.info("Post to cluster configuration view has an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")
        return HttpResponse(json.dumps(cluster_info), content_type="application/json")


class ClusterCapacityUpdateView(View):
    def post(self, request, name, stage):
        log.info("Update Cluster Capacity with data {}".format(request.body))
        try:
            settings = json.loads(request.body)
            cluster_name = '{}-{}'.format(name, stage)
            log.info("Update cluster {0} with {1}".format(cluster_name, settings))
            minSize = int(settings['minsize'])
            maxSize = int(settings['maxsize'])
            clusters_helper.update_cluster_capacity(request, cluster_name, minSize, maxSize)
        except Exception as e:
            log.info("Post to cluster capacity view has an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")

        return HttpResponse(json.dumps(settings), content_type="application/json")


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


def get_base_image_info(request, name):
    base_images = baseimages_helper.get_by_name(request, name)
    return HttpResponse(json.dumps(base_images), content_type="application/json")


def get_base_images_by_name_json(request, name):
    base_images = baseimages_helper.get_by_name(request, name)
    return HttpResponse(json.dumps(base_images), content_type="application/json")


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
    return HttpResponse(json.dumps(host_types), content_type="application/json")


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
    return HttpResponse(json.dumps(security_zones), content_type="application/json")


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
    return HttpResponse(json.dumps(placements), content_type="application/json")


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
    config_map['iam_role'] = 'base'
    config_map['cmp_group'] = 'CMP,{}-{}'.format(name, stage)
    config_map['pinfo_environment'] = 'prod'
    config_map['pinfo_team'] = 'cloudeng'
    config_map['pinfo_role'] = 'cmp_base'
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



def delete_cluster(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    log.info("Delete cluster {}".format(cluster_name));
    clusters_helper.delete_cluster(request, cluster_name)

    # Remove group and env relationship
    environs_helper.remove_env_capacity(request, name, stage, capacity_type="GROUP", data=cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def get_aws_config_name_list_by_image(image_name):
    config_map = {}
    config_map['iam_role'] = 'base'
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
        if image_name == DEFAULT_CMP_IMAGE:
            config_map['pinfo_role'] = 'cmp_base'
            config_map['pinfo_team'] = 'cloudeng'
        else:
            config_map['pinfo_role'] = ''
            config_map['pinfo_team'] = ''
    return config_map


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
