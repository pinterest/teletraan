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
"""Collection of all private builds related calls
"""
from deploy_board.webapp.helpers.deployclient import DeployClient
import builds_helper, environs_helper, s3_helper
import datetime
import time
import os
from django.conf import settings
from django.http import HttpResponse

def handle_uploaded_build(request, file, name, stage): 
    env = environs_helper.get_env_by_stage(request, name, stage)
    build_name = env['buildName']

    save_dir = 'private_builds' # all private builds will be temporarily saved to media/private_builds
    save_path = os.path.join(settings.MEDIA_ROOT, save_dir) # absolute path
    save_url = os.path.join(settings.MEDIA_URL, save_dir) # relative path
    file_name = file.name

    if not os.path.isdir(save_path):
        os.makedirs(save_path)

    if file_name.endswith('.tar.gz'): 
        file_extension = "tar.gz"
    else: 
        file_extension = file_name.split('.').pop()

    file_name = '%s.%s' % (build_name + '-' + request.POST['commitIdFull'][0:7], file_extension)
    build_url = save_path + "/" + file_name

    # upload onto Teletraan UI server
    try:
        destination = open(build_url, 'wb+')
        for chunk in file.chunks():
            destination.write(chunk)
        destination.close()
    except:
        return HttpResponse("Error uploading to Teletraan server", status=422)

    # upload onto S3/Pinrepo 
    return _upload_to_s3(request, build_url, file_name, build_name, name, stage)


def _upload_to_s3(request, build_url, file_name, build_name, name, stage):
    try:
        bucket_name = build_name + '-private'
        s3 = s3_helper.S3Helper('pinterest-builds') # get pinterest-build bucket
        key = '%s/%s' % (bucket_name, file_name)
        s3.upload_file(key, build_url)
    except:
        os.remove(build_url)
        return HttpResponse("Error uploading to Amazon S3 -- please check your connection and access rights.", status=422)
    
    # wait until file is finished succcesfully uploading into the bucket. Sets timeout to 10 seconds
    total_wait_time = 0
    while not s3.exists(key): 
        time.sleep(10)
        total_wait_time += 10
        if (total_wait_time > 50000):
            os.remove(build_url)
            return HttpResponse("Upload has timed out. Please check your connection to Amazon S3.", status=422)
    # deletes private build tarball from django server
    os.remove(build_url)
    return _upload_to_teletraan(request, build_url, file_name, build_name, name, stage) 
    

def _upload_to_teletraan(request, build_url, file_name, build_name, name, stage): 
    post_params = request.POST

    build = {}
    build['name'] = build_name
    build['repo'] = 'private'
    build['commitShort'] = post_params['commitIdFull'][0:7]
    build['commit'] = post_params['commitIdFull']
    build['branch'] = 'private'
    build['commitDate'] = int(round(time.time() * 1000))  # set arbitrary commitDate
    build['publishDate'] = int(round(time.time() * 1000))
    build['artifactUrl'] = 'https://deployrepo.pinadmin.com/' + build_name + '-private' + '/' + file_name

    build = builds_helper.publish_build(request, build)
    buildId = build.get('id')
    redirectURL = '/env/%s/%s/build/%s' % (name, stage, buildId)
    return HttpResponse(redirectURL)
