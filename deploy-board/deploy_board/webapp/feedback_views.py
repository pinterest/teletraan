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
"""Collection of all feedback related views"""

from django.shortcuts import render
from django.views.generic import View
from django.http import HttpResponse
import json
from .helpers import ratings_helper

DEFAULT_PAGE_SIZE = 30


def submit_feedback(request):
    data = request.POST
    rating = data["stars"]
    feedback = data["comment"]
    id = ratings_helper.create_ratings(request, rating, feedback)
    return HttpResponse(json.dumps({"result": id}), content_type="application/json")


class RatingsView(View):
    def get(self, request):
        index = int(request.GET.get("page_index", "1"))
        size = int(request.GET.get("page_size", DEFAULT_PAGE_SIZE))
        ratings = ratings_helper.get_ratings(request, index, size)
        starSum = 0
        numStarFeedbacks = 0
        overallRating = 0

        # Calculate average rating
        for rating in ratings:
            if rating["rating"] != "-1":
                starSum += int(rating["rating"])
                numStarFeedbacks += 1

        if numStarFeedbacks != 0:
            overallRating = round((starSum / float(numStarFeedbacks)), 2)

        return render(
            request,
            "feedbacks/feedback_landing.html",
            {
                "ratings": ratings,
                "overallRating": overallRating,
                "pageIndex": index,
                "pageSize": DEFAULT_PAGE_SIZE,
                "disablePrevious": index <= 1,
                "disableNext": len(ratings) < DEFAULT_PAGE_SIZE,
            },
        )
