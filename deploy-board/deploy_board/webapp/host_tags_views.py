from django.http import HttpResponse
from django.views.generic import View
from django.shortcuts import render, redirect

import common
import logging
import json
from helpers import environ_hosts_helper, environs_helper

log = logging.getLogger(__name__)


class HostTagsView(View):
    def get(self, request, name, stage, tag_name):
        ec2_host_tags = environ_hosts_helper.get_ec2_host_tags(request, name, stage, tag_name)
        host_tags = environ_hosts_helper.get_host_tags(request, name, stage, tag_name)
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        context = {
            "envs": envs,
            "env": env,
            "stages": stages,
            "host_tags": host_tags,
            "ec2_host_tags": ec2_host_tags,
            "tag_name": tag_name,
            "show_remove_btn": False
        }
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(request, name, stage)
        if deploy_constraint:
            max_parallel = deploy_constraint.get("maxParallel", None)
            context["max_parallel"] = max_parallel
            context["state"] = deploy_constraint.get("state", "UNKNOWN")
            context["show_remove_btn"] = True

        return render(request, 'environs/env_host_tags.html', context)


def add_constraint(request, name, stage):
    max_parallel = request.POST.get('max_parallel')
    tag_name = request.POST.get('tag_name')
    try:
        environ_hosts_helper.create_deploy_constraint(request, name, stage, {
            'constraintKey': tag_name,
            'maxParallel': max_parallel
        })
        return redirect('/env/{}/{}/host_tags/{}'.format(name, stage, tag_name))
    except Exception as e:
        log.error("add constraint failed with {}", e)
        return redirect('/env/{}/{}/host_tags/{}'.format(name, stage, tag_name))


def edit_constraint(request, name, stage):
    try:
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(request, name, stage)
        if not deploy_constraint:
            return HttpResponse(json.dumps({'html': 'Failed to find deploy constraint.'}), status=404,
                                content_type="application/json")
        max_parallel = request.POST.get("max_parallel")
        environ_hosts_helper.update_deploy_constraint(request, name, stage, {
            'maxParallel': max_parallel
        })
        tag_name = deploy_constraint.get('constraintKey')
        return redirect('/env/{}/{}/host_tags/{}'.format(name, stage, tag_name))
    except Exception as e:
        log.error("get constraint failed with {}", e)
        return HttpResponse(json.dumps({'html': 'Failed to get deploy constraint.'}), status=500,
                            content_type="application/json")


def remove_constraint(request, name, stage, tag_name):
    try:
        environ_hosts_helper.remove_deploy_constraint(request, name, stage)
        environ_hosts_helper.remove_host_tags(request, name, stage, tag_name)
        return redirect('/env/{}/{}/host_tags/{}'.format(name, stage, tag_name))
    except Exception as e:
        log.error("remove constraint failed with {}", e)
        return redirect('/env/{}/{}/host_tags/{}'.format(name, stage, tag_name))


def get_constraint(request, name, stage):
    try:
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(request, name, stage)
        return HttpResponse(json.dumps(deploy_constraint), status=200,
                            content_type="application/json")
    except Exception as e:
        log.error("get constraint failed with {}", e)
        return HttpResponse(json.dumps({'html': 'Failed to get deploy constraint.'}), status=500,
                            content_type="application/json")
