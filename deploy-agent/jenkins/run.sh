#!/bin/bash

# Clean tar.gz files.
cd $WORKSPACE/deploy-agent
set +e
find .tox/dist -name *.zip -delete
set -e

export IS_PINTEREST=true

# Run Tests
tox
