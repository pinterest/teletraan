#!/bin/bash

set -ex

docker build -t deploy_agent_unittests . -f Dockerfile.unittest
docker run -it deploy_agent_unittests
