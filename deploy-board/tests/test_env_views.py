# Copyright 2026 Pinterest, Inc.
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

"""Unit tests for env_views. Run from deploy-board/:

python -m unittest discover -s tests
"""

import collections
import collections.abc
import os
import tempfile
import unittest
from unittest import mock

# templatetags/utils.py imports Mapping from collections, which was removed in
# Python 3.10.
if not hasattr(collections, "Mapping"):
    collections.Mapping = collections.abc.Mapping

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "deploy_board.settings")
os.environ.setdefault("SECRET_KEY", "test")
os.environ.setdefault("IS_PINTEREST", "true")
os.environ.setdefault("LOG_DIR", tempfile.mkdtemp())
os.environ.setdefault("LOG_LEVEL", "INFO")

import django  # noqa: E402

django.setup()

from django.test import RequestFactory  # noqa: E402

from deploy_board.webapp import env_views  # noqa: E402
from deploy_board.webapp.error_views import ExceptionHandlerMiddleware  # noqa: E402
from deploy_board.webapp.helpers.exceptions import NotFoundException  # noqa: E402

ENV = {"id": "env-id", "envName": "foo", "stageName": "prod", "buildName": "foo"}


def ajax_get(path):
    return RequestFactory().get(path, HTTP_X_REQUESTED_WITH="XMLHttpRequest")


class AjaxNotFoundMixin:
    def assertAjaxNotFound(self, view, request):
        with self.assertRaises(NotFoundException) as ctx:
            view(request, "foo", "prod")
        with self.assertLogs("deploy_board.webapp.error_views", "ERROR"):
            response = ExceptionHandlerMiddleware(None).process_exception(
                request, ctx.exception
            )
        self.assertEqual(response.status_code, 404)


class GetBuildsTest(AjaxNotFoundMixin, unittest.TestCase):
    def setUp(self):
        self.request = ajax_get("/env/foo/prod/builds/?page_index=1&page_size=5")
        patcher = mock.patch.multiple(
            env_views,
            environs_helper=mock.DEFAULT,
            deploys_helper=mock.DEFAULT,
            builds_helper=mock.DEFAULT,
            render_to_string=mock.DEFAULT,
        )
        self.mocks = patcher.start()
        self.addCleanup(patcher.stop)
        self.environs_helper = self.mocks["environs_helper"]
        self.environs_helper.BUILD_STAGE = "BUILD"
        self.mocks["deploys_helper"].get_all.return_value = {"deploys": []}
        self.mocks["builds_helper"].get_builds_and_tags.return_value = []
        self.mocks["render_to_string"].return_value = "html"

    def rendered_context(self):
        return self.mocks["render_to_string"].call_args[0][1]

    def test_deleted_stage_returns_404(self):
        self.environs_helper.get_env_by_stage.return_value = None

        self.assertAjaxNotFound(env_views.get_builds, self.request)
        self.environs_helper.get_env_promotes_config.assert_not_called()

    def test_missing_promote_config_does_not_show_lock(self):
        self.environs_helper.get_env_by_stage.return_value = ENV
        self.environs_helper.get_env_promotes_config.return_value = None

        response = env_views.get_builds(self.request, "foo", "prod")

        self.assertEqual(response.status_code, 200)
        self.assertFalse(self.rendered_context()["show_lock"])

    def test_auto_promote_from_build_stage_shows_lock(self):
        self.environs_helper.get_env_by_stage.return_value = ENV
        self.environs_helper.get_env_promotes_config.return_value = {
            "type": "AUTO",
            "predStage": "BUILD",
        }

        env_views.get_builds(self.request, "foo", "prod")

        self.assertTrue(self.rendered_context()["show_lock"])

    def test_manual_promote_does_not_show_lock(self):
        self.environs_helper.get_env_by_stage.return_value = ENV
        self.environs_helper.get_env_promotes_config.return_value = {
            "type": "MANUAL",
            "predStage": "BUILD",
        }

        env_views.get_builds(self.request, "foo", "prod")

        self.assertFalse(self.rendered_context()["show_lock"])


class UpdateDeployProgressTest(AjaxNotFoundMixin, unittest.TestCase):
    def setUp(self):
        self.request = ajax_get("/env/foo/prod/update_deploy_progress/")
        patcher = mock.patch.multiple(
            env_views,
            environs_helper=mock.DEFAULT,
            deploys_helper=mock.DEFAULT,
        )
        self.mocks = patcher.start()
        self.addCleanup(patcher.stop)

    def test_deleted_stage_returns_404(self):
        self.mocks["environs_helper"].get_env_by_stage.return_value = None

        self.assertAjaxNotFound(env_views.update_deploy_progress, self.request)
        self.mocks["deploys_helper"].update_progress.assert_not_called()


if __name__ == "__main__":
    unittest.main()
