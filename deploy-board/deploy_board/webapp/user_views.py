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
"""Collection of roles related views
"""
import json
from django.http import HttpResponse
from django.middleware.csrf import get_token

from django.shortcuts import render
from django.template.loader import render_to_string
from .helpers import users_helper
from deploy_board.settings import IS_PINTEREST
import logging
import re

# TODO call backend instead of hardcode
ALL_ROLES = ['OPERATOR', 'ADMIN']
logger = logging.getLogger(__name__)


def get_users_config(request, name):
    user_types = request.GET.get('user_types')
    users = users_helper.get_env_users(request, name, user_types)

    return render(request, 'users/users_config.html', {
        "env_name": name,
        "users": users,
        "user_types": user_types,
        "roles": ALL_ROLES,
        "pinterest": IS_PINTEREST
    })


def get_users(request, name):
    user_types = request.GET.get('user_types')
    users = users_helper.get_env_users(request, name, user_types)
    html = render_to_string('users/users_config.tmpl', {
        "env_name": name,
        "users": users,
        "user_types": user_types,
        "roles": ALL_ROLES,
        "csrf_token": get_token(request),
        "pinterest": IS_PINTEREST
    })
    return HttpResponse(json.dumps({'html': html}), content_type="application/json")


def get_user_token(request, name, user_name):
    user_types = request.GET.get('user_types')
    user = users_helper.get_env_user(request, name, user_name, user_types)
    return HttpResponse(user['token'], content_type="application/text")


def update_users_config(request, name):
    user_types = request.GET.get('user_types')
    # First, retrive all the original users
    origin_user_dict = {}
    for key, value in request.POST.items():
        if key.startswith('TELETRAAN_ORIGIN_'):
            user_name = key[len('TELETRAAN_ORIGIN_'):]
            origin_user_dict[user_name] = value

    # Then, loop and all the new values, figure out which needs to delete, which needs to update
    updated_user_dict = {}
    new_user_dict = {}
    for key, value in request.POST.items():
        if key.startswith('TELETRAAN_NEW_'):
            user_name = key[len('TELETRAAN_NEW_'):].lower()
            if ' ' in user_name:
                raise Exception('Name cannot contain spaces!')
            unsupported_pattern = re.compile("[^a-zA-Z0-9\\-_]+")
            if unsupported_pattern.search(user_name):
                unsupported_chars = unsupported_pattern.findall(user_name)
                logger.info("Invalid username: %s", user_name)
                raise Exception('Script Name cannot contain unsupported characters: %s' % (unsupported_chars))
            if user_name in origin_user_dict:
                if value != origin_user_dict[user_name]:
                    updated_user_dict[user_name] = value
                origin_user_dict.pop(user_name, None)
            else:
                new_user_dict[user_name] = value

    # Finally update the backend accordingly
    for key, value in origin_user_dict.items():
        users_helper.delete_env_user(request, name, key, user_types)
    for key, value in updated_user_dict.items():
        users_helper.update_env_user(request, name, key, value, user_types)
    for key, value in new_user_dict.items():
        users_helper.create_env_user(request, name, key, value, user_types)

    users = users_helper.get_env_users(request, name, user_types)
    html = render_to_string('users/users_config.tmpl', {
        "env_name": name,
        "users": users,
        "user_types": user_types,
        "roles": ALL_ROLES,
        "csrf_token": get_token(request),
    })

    return HttpResponse(json.dumps({'html': html}), content_type="application/json")
