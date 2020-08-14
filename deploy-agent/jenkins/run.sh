#!/bin/bash

# Clean tar.gz files.
set +e
find .tox/dist -name *.zip -delete
set -e

export IS_PINTEREST=true

# Run Tests
tox

