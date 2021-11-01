#!/bin/bash

set -ex

BUILD_NAME=deployd_debug
docker build -t "$BUILD_NAME" . -f Dockerfile.unittest
docker run -it "$BUILD_NAME" bash
