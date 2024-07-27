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
"""Backend server all exception
"""
class NotFoundException(Exception):
    pass


class TeletraanException(Exception):
    def __init__(self, message: str, status=500) -> None:
        super().__init__(message)
        self.status = status


class IllegalArgumentException(TeletraanException):
    def __init__(self, message: str) -> None:
        super().__init__(message, status=400)

class FailedAuthenticationException(TeletraanException):
    def __init__(self, message: str) -> None:
        super().__init__(message, status=401)


class NotAuthorizedException(TeletraanException):
    def __init__(self, message: str) -> None:
        super().__init__(message, status=403)
