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

from unittest.mock import Mock, call, patch

from django.conf.urls import url
from django.contrib import messages
from django.contrib.messages.storage.fallback import FallbackStorage
from django.shortcuts import render
from django.test import RequestFactory, SimpleTestCase

from deploy_board.webapp import common, env_views
from deploy_board.webapp.helpers.exceptions import (
    NotAuthorizedException,
    TeletraanException,
)


def deploy_page(request):
    # Exercise the real redirect destination's template with a minimal environment.
    return render(
        request,
        "environs/env_landing.html",
        {
            "env": {"envName": "example", "stageName": "prod", "state": "NORMAL"},
            "env_promote": {"predStage": None},
        },
    )


urlpatterns = [
    url(
        r"^env/(?P<name>[\w-]+)/(?P<stage>[\w-]+)/rollback_to/(?P<deploy_id>[\w-]+)$",
        env_views.rollback_to,
    ),
    url(r"^env/example/prod/deploy/$", deploy_page),
]


class RollbackTest(SimpleTestCase):
    def setUp(self):
        self.form = {
            "description": "rollback reason",
            "mark_build_as_bad": "on",
            "toBeMarkedBuildId": "bad-build",
        }
        self.request = RequestFactory().post("/rollback/", self.form)
        self.request.session = {}
        self.request._messages = FallbackStorage(self.request)
        self.rollback_result = {"id": "rollback-deploy"}
        self.rollback = self.start_patch(
            "deploy_board.webapp.common.deploys_helper.rollback",
            return_value=self.rollback_result,
        )
        self.set_build_tag = self.start_patch(
            "deploy_board.webapp.common.builds_helper.set_build_tag"
        )

    def start_patch(self, name, **kwargs):
        patcher = patch(name, **kwargs)
        self.addCleanup(patcher.stop)
        return patcher.start()

    def rollback_to(self):
        return common.rollback_to(self.request, "example", "prod", "old-deploy")

    def test_rollback_precedes_tag_and_preserves_result(self):
        actions = Mock()
        actions.attach_mock(self.rollback, "rollback")
        actions.attach_mock(self.set_build_tag, "tag")

        result = self.rollback_to()

        self.assertIs(result, self.rollback_result)
        self.assertEqual(
            actions.mock_calls,
            [
                call.rollback(
                    self.request,
                    "example",
                    "prod",
                    "old-deploy",
                    description="rollback reason",
                ),
                call.tag(
                    self.request,
                    {
                        "targetId": "bad-build",
                        "targetType": "Build",
                        "value": "BAD_BUILD",
                        "comments": "deploy rollback, mark build as bad.rollback reason",
                    },
                ),
            ],
        )
        self.assertEqual(list(messages.get_messages(self.request)), [])

    def test_denied_tag_preserves_rollback_result_and_warns(self):
        self.set_build_tag.side_effect = NotAuthorizedException("build access denied")

        result = self.rollback_to()

        self.assertIs(result, self.rollback_result)
        self.rollback.assert_called_once()
        warning = list(messages.get_messages(self.request))
        self.assertEqual(len(warning), 1)
        self.assertEqual(warning[0].level, messages.WARNING)
        self.assertIn("Rollback started", str(warning[0]))
        self.assertIn("not marked as bad", str(warning[0]))

    def test_rollback_failure_does_not_tag(self):
        self.rollback.side_effect = TeletraanException("rollback failed")

        with self.assertRaises(TeletraanException):
            self.rollback_to()

        self.set_build_tag.assert_not_called()
        self.assertEqual(list(messages.get_messages(self.request)), [])

    def test_other_tag_failures_are_not_hidden(self):
        self.set_build_tag.side_effect = TeletraanException("backend unavailable")

        with self.assertRaises(TeletraanException):
            self.rollback_to()

        self.rollback.assert_called_once()
        self.assertEqual(list(messages.get_messages(self.request)), [])

    def test_opted_out_tag_is_not_created(self):
        self.request.POST = self.request.POST.copy()
        self.request.POST["mark_build_as_bad"] = "off"

        self.assertIs(self.rollback_to(), self.rollback_result)

        self.set_build_tag.assert_not_called()

    def test_missing_build_id_does_not_create_tag(self):
        self.request.POST = self.request.POST.copy()
        del self.request.POST["toBeMarkedBuildId"]

        self.assertIs(self.rollback_to(), self.rollback_result)

        self.set_build_tag.assert_not_called()

    def test_denied_tag_warning_survives_redirect_and_renders_on_deploy_page(self):
        self.set_build_tag.side_effect = NotAuthorizedException("build access denied")

        response = self.client.post(
            "/env/example/prod/rollback_to/old-deploy", self.form, follow=True
        )

        self.assertEqual(response.redirect_chain, [("/env/example/prod/deploy/", 302)])
        self.assertContains(response, '<div class="alert alert-warning" role="alert">')
        self.assertContains(response, "<strong>Warning!</strong> Rollback started")
        self.assertContains(response, "previous build was not marked as bad")
        self.assertNotContains(response, "<strong>Success!</strong> Rollback started")
        self.rollback.assert_called_once()
