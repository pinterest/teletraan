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
    from deploy_board.settings import DEFAULT_PROVIDER, DEFAULT_CMP_IMAGE, \
        DEFAULT_CMP_HOST_TYPE, DEFAULT_CMP_PINFO_ENVIRON, DEFAULT_CMP_ACCESS_ROLE, DEFAULT_CELL, \
        DEFAULT_PLACEMENT, USER_DATA_CONFIG_SETTINGS_WIKI, TELETRAAN_CLUSTER_READONLY_FIELDS
import json
import logging

from helpers import baseimages_helper, hosttypes_helper, securityzones_helper, placements_helper, \
    autoscaling_groups_helper, groups_helper, cells_helper
from helpers import clusters_helper, environs_helper, environ_hosts_helper
from helpers.exceptions import NotAuthorizedException, TeletraanException
import common
import traceback

log = logging.getLogger(__name__)

DEFAULT_PAGE_SIZE = 200


class EnvCapacityBasicCreateView(View):
    def get(self, request, name, stage):
        host_types = hosttypes_helper.get_by_provider(
            request, DEFAULT_PROVIDER)
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        security_zones = securityzones_helper.get_by_provider_and_cell_name(
            request, DEFAULT_PROVIDER, DEFAULT_CELL)
        placements = placements_helper.get_by_provider_and_cell_name(
            request, DEFAULT_PROVIDER, DEFAULT_CELL)
        default_base_image = get_base_image_info_by_name(request, DEFAULT_CMP_IMAGE, DEFAULT_CELL)
        env = environs_helper.get_env_by_stage(request, name, stage)

        capacity_creation_info = {
            'environment': env,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'baseImages': default_base_image,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER,
            'defaultHostType': DEFAULT_CMP_HOST_TYPE,
            'defaultSeurityZone': DEFAULT_PLACEMENT
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
            if 'configs' in cluster_info:
                for field in TELETRAAN_CLUSTER_READONLY_FIELDS:
                    if field in cluster_info['configs']:
                        log.error("Teletraan does not support user to change %s %s" % (field, cluster_info[field]))
                        raise TeletraanException("Teletraan does not support user to create %s" % s)

            clusters_helper.create_cluster_with_env(request, cluster_name, name, stage, cluster_info)

            log.info("Associate cluster_name to environment")
            # Update cluster info
            environs_helper.update_env_basic_config(
                request, name, stage, data={"clusterName": cluster_name})

            log.info("Update capacity to the environment")
            # set up env and group relationship
            environs_helper.add_env_capacity(
                request, name, stage, capacity_type="GROUP", data=cluster_name)
            return HttpResponse("{}", content_type="application/json")
        except NotAuthorizedException as e:
            log.error("Have an NotAuthorizedException error {}".format(e))
            return HttpResponse(e, status=403, content_type="application/json")
        except Exception as e:
            log.error("Have an error {}".format(e))
            return HttpResponse(e, status=500, content_type="application/json")


class EnvCapacityAdvCreateView(View):
    def get(self, request, name, stage):
        host_types = hosttypes_helper.get_by_provider(
            request, DEFAULT_PROVIDER)
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        security_zones = securityzones_helper.get_by_provider_and_cell_name(
            request, DEFAULT_PROVIDER, DEFAULT_CELL)
        placements = placements_helper.get_by_provider_and_cell_name(
            request, DEFAULT_PROVIDER, DEFAULT_CELL)
        cells = cells_helper.get_by_provider(request, DEFAULT_PROVIDER)
        base_images = get_base_image_info_by_name(request, DEFAULT_CMP_IMAGE, DEFAULT_CELL)
        base_images_names = baseimages_helper.get_image_names(
            request, DEFAULT_PROVIDER, DEFAULT_CELL)

        env = environs_helper.get_env_by_stage(request, name, stage)
        provider_list = baseimages_helper.get_all_providers(request)

        capacity_creation_info = {
            'environment': env,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'cells': cells,
            'baseImages': base_images,
            'baseImageNames': base_images_names,
            'defaultBaseImage': DEFAULT_CMP_IMAGE,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER,
            'defaultCell': DEFAULT_CELL,
            'defaultHostType': DEFAULT_CMP_HOST_TYPE,
            'defaultSeurityZone': DEFAULT_PLACEMENT,
            'providerList': provider_list,
            'configList': get_aws_config_name_list_by_image(DEFAULT_CMP_IMAGE)
        }
        # cluster manager
        return render(request, 'configs/new_capacity_adv.html', {
            'env': env,
            'capacity_creation_info': json.dumps(capacity_creation_info),
            'default_cmp_image': DEFAULT_CMP_IMAGE,
            'user_data_config_settings_wiki': USER_DATA_CONFIG_SETTINGS_WIKI,
            'is_pinterest': IS_PINTEREST})

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

            log.info("Update capacity to the environment")
            # set up env and group relationship
            environs_helper.add_env_capacity(
                request, name, stage, capacity_type="GROUP", data=cluster_name)

            return HttpResponse("{}", content_type="application/json")
        except NotAuthorizedException as e:
            log.error("Have an NotAuthorizedException error {}".format(e))
            return HttpResponse(e, status=403, content_type="application/json")
        except Exception as e:
            log.error("Have an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")


class ClusterConfigurationView(View):
    def get(self, request, name, stage):

        cluster_name = '{}-{}'.format(name, stage)
        current_cluster = clusters_helper.get_cluster(request, cluster_name)
        host_types = hosttypes_helper.get_by_provider(
            request, DEFAULT_PROVIDER)
        current_image = baseimages_helper.get_by_id(
            request, current_cluster['baseImageId'])
        # TODO: remove baseImageName and access the prop from baseImage directly.
        current_cluster['baseImageName'] = current_image['abstract_name']
        current_cluster['baseImage'] = current_image
        for host_type in host_types:
            host_type['mem'] = float(host_type['mem']) / 1024

        cells = cells_helper.get_by_provider(request, current_cluster['provider'])
        security_zones = securityzones_helper.get_by_provider_and_cell_name(
            request, current_cluster['provider'], current_cluster['cellName'])
        placements = placements_helper.get_by_provider_and_cell_name(
            request, current_cluster['provider'], current_cluster['cellName'])
        base_images = get_base_image_info_by_name(
            request, current_image['abstract_name'], current_cluster['cellName'])
        base_images_names = baseimages_helper.get_image_names(
            request, current_cluster['provider'], current_cluster['cellName'])

        env = environs_helper.get_env_by_stage(request, name, stage)
        provider_list = baseimages_helper.get_all_providers(request)

        capacity_creation_info = {
            'environment': env,
            'cells': cells,
            'hostTypes': host_types,
            'securityZones': security_zones,
            'placements': placements,
            'baseImages': base_images,
            'baseImageNames': base_images_names,
            'defaultBaseImage': DEFAULT_CMP_IMAGE,
            'defaultCMPConfigs': get_default_cmp_configs(name, stage),
            'defaultProvider': DEFAULT_PROVIDER,
            'providerList': provider_list,
            'readonlyFields': TELETRAAN_CLUSTER_READONLY_FIELDS,
            'configList': get_aws_config_name_list_by_image(DEFAULT_CMP_IMAGE),
            'currentCluster': current_cluster
        }

        return render(request, 'clusters/cluster_configuration.html', {
            'env': env,
            'capacity_creation_info': json.dumps(capacity_creation_info),
            'default_cmp_image': DEFAULT_CMP_IMAGE,
            'user_data_config_settings_wiki': USER_DATA_CONFIG_SETTINGS_WIKI,
            'is_pinterest': IS_PINTEREST})

    def post(self, request, name, stage):
        try:
            env = environs_helper.get_env_by_stage(request, name, stage)
            cluster_name = env.get('clusterName')
            cluster_info = json.loads(request.body)
            log.info("Update Cluster Configuration with {}", cluster_info)

            cluster_name = '{}-{}'.format(name, stage)
            current_cluster = clusters_helper.get_cluster(request, cluster_name)
            log.info("getting current Cluster Configuration is {}", current_cluster)
            if 'configs' in current_cluster and 'configs' in cluster_info:
                for field in TELETRAAN_CLUSTER_READONLY_FIELDS:
                    if field in current_cluster['configs'] and field in cluster_info['configs']:
                        if current_cluster['configs'][field] != cluster_info['configs'][field]:
                            log.error("Teletraan does not support user to update %s %s" % (field, cluster_info['spiffe_id']))
                            raise TeletraanException("Teletraan does not support user to update %s" % field)

                    if field in current_cluster['configs'] and field not in cluster_info['configs']:
                        log.error("Teletraan does not support user to remove %s %s" % (field, cluster_info[field]))
                        raise TeletraanException("Teletraan does not support user to remove %s" % field)

            image = baseimages_helper.get_by_id(request, cluster_info['baseImageId'])
            clusters_helper.update_cluster(request, cluster_name, cluster_info)
        except NotAuthorizedException as e:
            log.error("Have an NotAuthorizedException error {}".format(e))
            return HttpResponse(e, status=403, content_type="application/json")
        except Exception as e:
            log.error("Post to cluster configuration view has an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")
        return HttpResponse(json.dumps(cluster_info), content_type="application/json")


class ClusterCapacityUpdateView(View):
    def post(self, request, name, stage):
        log.info("Update Cluster Capacity with data {}".format(request.body))
        try:
            settings = json.loads(request.body)
            cluster_name = '{}-{}'.format(name, stage)
            log.info("Update cluster {0} with {1}".format(
                cluster_name, settings))
            minSize = int(settings['minsize'])
            maxSize = int(settings['maxsize'])
            clusters_helper.update_cluster_capacity(
                request, cluster_name, minSize, maxSize)
        except NotAuthorizedException as e:
            log.error("Have an NotAuthorizedException error {}".format(e))
            return HttpResponse(e, status=403, content_type="application/json")
        except Exception as e:
            log.error("Post to cluster capacity view has an error {}", e)
            return HttpResponse(e, status=500, content_type="application/json")
        return HttpResponse(json.dumps(settings), content_type="application/json")


def create_base_image(request):
    params = request.POST
    base_image_info = {}
    base_image_info['abstract_name'] = params['abstractName']
    base_image_info['provider_name'] = params['providerName']
    base_image_info['provider'] = params['provider']
    base_image_info['description'] = params['description']
    base_image_info['cell_name'] = params['cellName']
    if 'basic' in params:
        base_image_info['basic'] = True
    else:
        base_image_info['basic'] = False
    baseimages_helper.create_base_image(request, base_image_info)
    return redirect('/clouds/baseimages/')


def get_base_images(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    base_images = baseimages_helper.get_all_with_acceptance(
        request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)
    cells_list = cells_helper.get_by_provider(request, DEFAULT_PROVIDER)

    return render(request, 'clusters/base_images.html', {
        'base_images': base_images,
        'provider_list': provider_list,
        'cells_list': cells_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(base_images) < DEFAULT_PAGE_SIZE,
    })


def get_image_names_by_provider_and_cell(request, provider, cell):
    image_names = baseimages_helper.get_image_names(request, provider, cell)
    return HttpResponse(json.dumps(image_names), content_type="application/json")


def get_images_by_provider_and_cell(request, provider, cell):
    images = baseimages_helper.get_all_by(request, provider, cell)
    return HttpResponse(json.dumps(images), content_type="application/json")


def get_placements_by_provider_and_cell(request, provider, cell):
    data = placements_helper.get_by_provider_and_cell_name(request, provider, cell)
    return HttpResponse(json.dumps(data), content_type="application/json")


def get_security_zones_by_provider_and_cell(request, provider, cell):
    data = securityzones_helper.get_by_provider_and_cell_name(request, provider, cell)
    return HttpResponse(json.dumps(data), content_type="application/json")


def get_image_names(request):
    params = request.GET
    provider = params['provider']
    env_name = params['env']
    stage_name = params['stage']
    cell = params.get('cell', DEFAULT_CELL)
    image_names = baseimages_helper.get_image_names(request, provider, cell)
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
    cell = params.get('cell', DEFAULT_CELL)
    base_images = None
    if 'name' in params:
        name = params['name']
        base_images = baseimages_helper.get_by_name(request, name, cell)

    curr_base_image = None
    if 'curr_base_image' in params:
        curr_base_image = params['curr_base_image']
        image = baseimages_helper.get_by_id(request, curr_base_image)
        curr_image_name = image.get('abstract_name')
        base_images = baseimages_helper.get_by_name(request, curr_image_name, cell)

    contents = render_to_string("clusters/get_base_image.tmpl", {
        'base_images': base_images,
        'curr_base_image': curr_base_image,
    })
    return HttpResponse(json.dumps(contents), content_type="application/json")


def get_base_image_info_by_name(request, name, cell):
    if name.startswith('cmp_base'):
        base_images = baseimages_helper.get_acceptance_by_name(request, name, cell)
        with_acceptance_rs = []
        if base_images:
            for image in base_images:
                r = image.get('baseImage')
                if r:
                    r['acceptance'] = image.get('acceptance', 'UNKNOWN')
                    with_acceptance_rs.append(r)
        return with_acceptance_rs
    return baseimages_helper.get_by_name(request, name, cell)


def get_base_images_by_name_json(request, name):
    cell = DEFAULT_CELL
    params = request.GET
    if params:
        cell = params.get('cell', DEFAULT_CELL)
    base_images = get_base_image_info_by_name(request, name, cell)
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
    return redirect('/clouds/hosttypes/')


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
    security_zone_info['cell_name'] = params.get('cellName', DEFAULT_CELL)
    if 'basic' in params:
        security_zone_info['basic'] = True
    else:
        security_zone_info['basic'] = False
    securityzones_helper.create_security_zone(request, security_zone_info)
    return redirect('/clouds/securityzones/')


def get_security_zones(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    security_zones = securityzones_helper.get_all(request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)
    cells_list = cells_helper.get_by_provider(request, DEFAULT_PROVIDER)

    return render(request, 'clusters/security_zones.html', {
        'security_zones': security_zones,
        'provider_list': provider_list,
        'cells_list': cells_list,
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
    cell = params.get('cell', DEFAULT_CELL)

    security_zones = securityzones_helper.get_by_provider_and_cell_name(request, provider, cell)
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
    placement_info['cell_name'] = params.get('cellName', DEFAULT_CELL)
    if 'basic' in params:
        placement_info['basic'] = True
    else:
        placement_info['basic'] = False
    placements_helper.create_placement(request, placement_info)
    return redirect('/clouds/placements/')


def get_placements(request):
    index = int(request.GET.get('page_index', '1'))
    size = int(request.GET.get('page_size', DEFAULT_PAGE_SIZE))
    placements = placements_helper.get_all(request, index, size)
    provider_list = baseimages_helper.get_all_providers(request)
    cells_list = cells_helper.get_by_provider(request, DEFAULT_PROVIDER)

    return render(request, 'clusters/placements.html', {
        'placements': placements,
        'provider_list': provider_list,
        'cells_list': cells_list,
        'pageIndex': index,
        'pageSize': DEFAULT_PAGE_SIZE,
        'disablePrevious': index <= 1,
        'disableNext': len(placements) < DEFAULT_PAGE_SIZE,
    })


def get_placements_by_provider(request):
    params = request.GET
    provider = params['provider']
    cell = params.get('cell', DEFAULT_CELL)
    curr_placement_arrays = None
    if 'curr_placement' in params:
        curr_placement = params['curr_placement']
        curr_placement_arrays = curr_placement.split(',')

    placements = placements_helper.get_by_provider_and_cell_name(request, provider, cell)
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
    config_map['pinfo_environment'] = DEFAULT_CMP_PINFO_ENVIRON
    config_map['pinfo_team'] = 'cloudeng'
    config_map['pinfo_role'] = 'cmp_base'
    config_map['access_role'] = DEFAULT_CMP_ACCESS_ROLE
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
    environs_helper.update_env_basic_config(
        request, env_name, env_stage, data=env_info)
    return cluster_info


def delete_cluster(request, name, stage):
    cluster_name = common.get_cluster_name(request, name, stage)
    log.info("Delete cluster {}".format(cluster_name))
    clusters_helper.delete_cluster(request, cluster_name)

    # Remove group and env relationship
    environs_helper.remove_env_capacity(
        request, name, stage, capacity_type="GROUP", data=cluster_name)
    return redirect('/env/{}/{}/config/capacity/'.format(name, stage))


def clone_cluster(request, src_name, src_stage):
    external_id = None
    try:
        params = request.POST
        dest_name = params.get('new_environment', src_name)
        dest_stage = params.get('new_stage', src_stage + '_clone')

        src_cluster_name = '{}-{}'.format(src_name, src_stage)
        dest_cluster_name = '{}-{}'.format(dest_name, dest_stage)

        ##0. teletraan service get src env buildName
        src_env = environs_helper.get_env_by_stage(request, src_name, src_stage)
        build_name = src_env.get('buildName', None)
        external_id = environs_helper.create_identifier_for_new_stage(request, dest_name, dest_stage)

        ##1. teletraan service create a new env
        dest_env = environs_helper.create_env(request, {
            'envName': dest_name,
            'stageName': dest_stage,
            'buildName': build_name,
            'externalId': external_id
        })
        log.info('clone_cluster, created a new env %s' % dest_env)

        ##2. rodimus service get src_cluster config
        src_cluster_info = clusters_helper.get_cluster(request, src_cluster_name)
        log.info('clone_cluster, src cluster info %s' % src_cluster_info)
        configs = src_cluster_info.get('configs')
        if configs:
            cmp_group = configs.get('cmp_group')
            if cmp_group:
                cmp_groups_set = set(cmp_group.split(','))
                cmp_groups_set.remove(src_cluster_name)
                cmp_groups_set.remove('CMP')
                cmp_groups_set.add(dest_cluster_name)
                # CMP needs to be the first in the list
                configs['cmp_group'] = ','.join(['CMP'] + list(cmp_groups_set))
                src_cluster_info['configs'] = configs

        ##3. rodimus service post create cluster
        src_cluster_info['clusterName'] = dest_cluster_name
        src_cluster_info['capacity'] = 0
        log.info('clone_cluster, request clone cluster info %s' % src_cluster_info)
        dest_cluster_info = clusters_helper.create_cluster_with_env(request, dest_cluster_name, dest_name, dest_stage, src_cluster_info)
        log.info('clone_cluster, cloned cluster info %s' % dest_cluster_info)

        ##4. teletraan service update_env_basic_config
        environs_helper.update_env_basic_config(request, dest_name, dest_stage,
                                                data={"clusterName": dest_cluster_name}
                                                )
        ##5. teletraan service set up env and group relationship
        environs_helper.update_env_capacity(request, dest_name, dest_stage, capacity_type="GROUP",
                                            data=[dest_cluster_name])

        ##6. get src script_config
        src_script_configs = environs_helper.get_env_script_config(request, src_name, src_stage)
        src_agent_configs = environs_helper.get_env_agent_config(request, src_name, src_stage)
        src_alarms_configs = environs_helper.get_env_alarms_config(request, src_name, src_stage)
        src_metrics_configs = environs_helper.get_env_metrics_config(request, src_name, src_stage)
        src_webhooks_configs = environs_helper.get_env_hooks_config(request, src_name, src_stage)

        ##8. clone all the extra configs
        if src_agent_configs:
            environs_helper.update_env_agent_config(request, dest_name, dest_stage, src_agent_configs)
        if src_script_configs:
            environs_helper.update_env_script_config(request, dest_name, dest_stage, src_script_configs)
        if src_alarms_configs:
            environs_helper.update_env_alarms_config(request, dest_name, dest_stage, src_alarms_configs)
        if src_metrics_configs:
            environs_helper.update_env_metrics_config(request, dest_name, dest_stage, src_metrics_configs)
        if src_webhooks_configs:
            environs_helper.update_env_hooks_config(request, dest_name, dest_stage, src_webhooks_configs)

        return HttpResponse(json.dumps(src_cluster_info), content_type="application/json")
    except NotAuthorizedException as e:
        log.error("Have an NotAuthorizedException error {}".format(e))
        if external_id is not None:
            environs_helper.delete_nimbus_identifier(request, external_id)

        return HttpResponse(e, status=403, content_type="application/json")
    except Exception as e:
        log.error("Failed to clone cluster env_name: %s, stage_name: %s" % (src_name, src_stage))
        log.error(traceback.format_exc())
        if external_id is not None:
            environs_helper.delete_nimbus_identifier(request, external_id)
        return HttpResponse(e, status=500, content_type="application/json")


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
        config_map['root_volume_size'] = 100
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
    return redirect('/env/{}/{}/'.format(name, stage))


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
        groups = environs_helper.get_env_capacity(
            request, name, stage, capacity_type="GROUP")
        for group_name in groups:
            cluster_name = group_name
    clusters_helper.force_terminate_hosts(
        request, cluster_name, host_ids, replace_host)
    return redirect('/env/{}/{}/'.format(name, stage))


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


def get_replacement_summary(request, cluster_name, event, current_capacity):
    host_ids = event.get('host_ids')
    state = event.get('state')
    status = event.get('status')
    progress_type = 'success' if status in [
        'SUCCEEDING', 'SUCCEEDED'] else 'danger'
    if not host_ids:
        num_finished_host_ids = 0
    else:
        num_finished_host_ids = len(host_ids.split(','))
    if state == 'COMPLETED':
        if status == 'SUCCEEDED':
            # successful
            succeeded = num_finished_host_ids
            progress_rate = 100
            msg = event.get('error_message', '')
            return {
                'id': event.get('id'),
                'state': state,
                'status': status,
                'startDate': event.get('start_time'),
                'lastUpdateDate': event.get('last_worked_on'),
                'progressType': progress_type,
                'progressTip': 'Among total {} hosts, {} successfully replaced and {} are pending'.format(
                    succeeded, succeeded, 0),
                'successRatePercentage': progress_rate,
                'successRate': '{}% ({}/{})'.format(progress_rate, succeeded, succeeded),
                'description': msg
            }
        else:
            # failed
            succeeded = num_finished_host_ids
            progress_rate = succeeded * 100 / current_capacity
            msg = event.get('error_message', '')
            return {
                'id': event.get('id'),
                'state': state,
                'status': status,
                'startDate': event.get('start_time'),
                'lastUpdateDate': event.get('last_worked_on'),
                'progressType': progress_type,
                'progressTip': 'Among total {} hosts, {} successfully replaced and {} are pending. Reason: {}'.format(
                    current_capacity, succeeded, current_capacity - succeeded, msg),
                'successRatePercentage': progress_rate,
                'successRate': '{}% ({}/{})'.format(progress_rate, succeeded, current_capacity),
                'description': msg
            }

    else:
        # on-going event
        replaced_and_succeeded_hosts = groups_helper.get_replaced_and_good_hosts(
            request, cluster_name)
        succeeded = len(replaced_and_succeeded_hosts)
        progress_rate = succeeded * 100 / current_capacity
        # its not necessarily error message
        on_going_msg = event.get('error_message')
        return {
            'id': event.get('id'),
            'state': state,
            'status': status,
            'startDate': event.get('start_time'),
            'lastUpdateDate': event.get('last_worked_on'),
            'progressType': progress_type,
            'progressTip': 'Among total {} hosts, {} successfully replaced and {} are pending. {}'.format(
                current_capacity, succeeded, current_capacity - succeeded, on_going_msg),
            'successRatePercentage': progress_rate,
            'successRate': '{}% ({}/{})'.format(progress_rate, succeeded, current_capacity)
        }


def cluster_replacement_progress(request, name, stage):
    env = environs_helper.get_env_by_stage(request, name, stage)

    cluster_name = '{}-{}'.format(name, stage)
    replacement_event = clusters_helper.get_latest_cluster_replacement_progress(
        request, cluster_name)
    if not replacement_event:
        log.info("There is no on-going replacement event for cluster %s." %
                 cluster_name)
        return HttpResponse("There is no on-going replacement.")

    # basic_cluster_info = clusters_helper.get_cluster(request, cluster_name)
    # capacity = basic_cluster_info.get("capacity")
    # should not respect the cluster capacity here, when min != max, the capacity is not a right number
    asg_summary = autoscaling_groups_helper.get_autoscaling_summary(request, cluster_name)
    desired_capacity = None
    if asg_summary:
        desired_capacity = asg_summary.get("desiredCapacity")
    if not desired_capacity:
        error_msg = "cluster %s has wrong desired_capacity: %s, asg_summary: %s" % \
                    (cluster_name, desired_capacity, asg_summary)
        log.error(error_msg)
        return HttpResponse(error_msg, status=500, content_type="application/json")

    replacement_progress = get_replacement_summary(
        request, cluster_name, replacement_event, desired_capacity)

    html = render_to_string('clusters/replace_progress.tmpl', {
        "env": env,
        "replace_progress_report": replacement_progress
    })
    response = HttpResponse(html)
    return response


def cluster_replacement_details(request, name, stage):
    cluster_name = '{}-{}'.format(name, stage)
    replacement_event = clusters_helper.get_latest_cluster_replacement_progress(
        request, cluster_name)
    if not replacement_event:
        return HttpResponse("{}", content_type="application/json")
    return HttpResponse(json.dumps(replacement_event), content_type="application/json")


def view_cluster_replacement_details(request, name, stage, replacement_id):
    env = environs_helper.get_env_by_stage(request, name, stage)
    cluster_name = '{}-{}'.format(name, stage)

    replacement_event = clusters_helper.get_cluster_replacement_info(
        request, cluster_name, replacement_id)
    if not replacement_event:
        raise Exception("Replacement Id: %s Not Found.")

    basic_cluster_info = clusters_helper.get_cluster(request, cluster_name)
    capacity = basic_cluster_info.get("capacity")
    replacement_details = get_replacement_summary(
        request, cluster_name, replacement_event, capacity)
    config_histories = clusters_helper.get_cluster_replacement_config_histories(
        request, cluster_name, replacement_id)
    return render(request, 'clusters/cluster_replace_details.html', {
        "replace": replacement_details,
        "config_histories": config_histories,
        "env": env
    })


def view_cluster_replacement_scaling_activities(request, name, stage):
    cluster_name = '{}-{}'.format(name, stage)
    scaling_activities = autoscaling_groups_helper.get_scaling_activities(
        request, cluster_name, 20, '')
    activities = json.dumps(scaling_activities["activities"])
    return HttpResponse(activities, content_type="application/json")


def view_cluster_replacement_schedule(request, name, stage, replacement_id):
    env = environs_helper.get_env_by_stage(request, name, stage)
    cluster_name = '{}-{}'.format(name, stage)
    schedule = clusters_helper.get_cluster_replacement_schedule(
        request, cluster_name, replacement_id)
    return render(request, 'clusters/replace_schedule.html', {
        "env": env,
        "schedule": schedule
    })


class ClusterHistoriesView(View):
    def get(self, request, name, stage):
        env = environs_helper.get_env_by_stage(request, name, stage)

        cluster_name = '{}-{}'.format(name, stage)
        page_index = request.GET.get('index')
        page_size = request.GET.get('size')
        histories = clusters_helper.get_cluster_replacement_histories(
            request, cluster_name, page_index, page_size)

        replace_summaries = []
        if histories:
            basic_cluster_info = clusters_helper.get_cluster(
                request, cluster_name)
            capacity = basic_cluster_info.get("capacity")

            for history in histories:
                replace_summaries.append(get_replacement_summary(
                    request, cluster_name, history, capacity))

        data = {
            "env": env,
            "replace_summaries": replace_summaries
        }
        return render(request, 'clusters/replace_histories.html', data)
