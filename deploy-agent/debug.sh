#!/bin/bash

set -ex

BUILD_NAME=deployd_debug
#docker rmi "$BUILD_NAME"  # don't always run; fails anyways if containers are running, force it -f
docker build -t "$BUILD_NAME" .
docker run -it "$BUILD_NAME" bash
