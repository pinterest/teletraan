from django.http import HttpResponse
from django.views.generic import View
from django.shortcuts import render, redirect

from . import common
import logging
import json
from .helpers import environ_hosts_helper, environs_helper

log = logging.getLogger(__name__)


class HostTagsView(View):
    def get(self, request, name, stage):
        envs = environs_helper.get_all_env_stages(request, name)
        stages, env = common.get_all_stages(envs, stage)
        context = {"envs": envs, "env": env, "stages": stages}
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(
            request, name, stage
        )
        if deploy_constraint:
            max_parallel = deploy_constraint.get("maxParallel", None)
            tag_name = deploy_constraint.get("constraintKey", None)
            context["tag_name"] = tag_name
            context["max_parallel"] = max_parallel
            context["state"] = deploy_constraint.get("state", "UNKNOWN")
            context["constraint_type"] = deploy_constraint.get(
                "constraintType", environs_helper.DEPLOY_CONSTRAINT_TYPES[0]
            )
            context["show_remove_btn"] = True
        else:
            context["show_remove_btn"] = False

        context["constraintTypes"] = environs_helper.DEPLOY_CONSTRAINT_TYPES
        return render(request, "environs/env_host_tags.html", context)


def get_host_tags_progress(request, name, stage, tag_name):
    host_tags = environ_hosts_helper.get_host_tags(request, name, stage, tag_name)
    return render(
        request,
        "hosts/host_tags_sync_progress.tmpl",
        {"host_tags": host_tags, "envName": name, "stageName": stage},
    )


def get_host_ec2tags(request, name, stage):
    ec2_host_tags = environ_hosts_helper.get_ec2_host_tags(request, name, stage)
    return HttpResponse(json.dumps(ec2_host_tags), content_type="application/json")


def add_constraint(request, name, stage):
    max_parallel = request.POST.get("max_parallel")
    tag_name = request.POST.get("tag_name")
    constraint_type = request.POST.get(
        "constraint_type", environs_helper.DEPLOY_CONSTRAINT_TYPES[0]
    )
    try:
        environ_hosts_helper.create_deploy_constraint(
            request,
            name,
            stage,
            {
                "constraintKey": tag_name,
                "maxParallel": max_parallel,
                "constraintType": constraint_type,
            },
        )
        return redirect("/env/{}/{}/constraint/".format(name, stage))
    except Exception as e:
        log.error("add constraint failed with {}", e)
        return redirect("/env/{}/{}/constraint/".format(name, stage))


def edit_constraint(request, name, stage):
    try:
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(
            request, name, stage
        )
        if not deploy_constraint:
            return HttpResponse(
                json.dumps({"html": "Failed to find deploy constraint."}),
                status=404,
                content_type="application/json",
            )
        max_parallel = request.POST.get("max_parallel")
        constraint_type = request.POST.get(
            "constraint_type", environs_helper.DEPLOY_CONSTRAINT_TYPES[0]
        )
        environ_hosts_helper.update_deploy_constraint(
            request,
            name,
            stage,
            {"maxParallel": max_parallel, "constraintType": constraint_type},
        )
        return redirect("/env/{}/{}/constraint/".format(name, stage))
    except Exception as e:
        log.error("get constraint failed with {}", e)
        return HttpResponse(
            json.dumps({"html": "Failed to get deploy constraint."}),
            status=500,
            content_type="application/json",
        )


def remove_constraint(request, name, stage):
    try:
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(
            request, name, stage
        )
        tag_name = deploy_constraint.get("constraintKey")
        environ_hosts_helper.remove_deploy_constraint(request, name, stage)
        environ_hosts_helper.remove_host_tags(request, name, stage, tag_name)
        return redirect("/env/{}/{}/constraint/".format(name, stage))
    except Exception as e:
        log.error("remove constraint failed with {}", e)
        return redirect("/env/{}/{}/constraint/".format(name, stage))


def get_constraint(request, name, stage):
    try:
        deploy_constraint = environ_hosts_helper.get_deploy_constraint(
            request, name, stage
        )
        return HttpResponse(
            json.dumps(deploy_constraint), status=200, content_type="application/json"
        )
    except Exception as e:
        log.error("get constraint failed with {}", e)
        return HttpResponse(
            json.dumps({"html": "Failed to get deploy constraint."}),
            status=500,
            content_type="application/json",
        )
