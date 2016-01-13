#!/bin/bash
# Set env variable TELETRAAN_SERVICE_URL to be the backend Teletraan service url
# Set env variable TELETRAAN_TEST_TOKEN if backend authentication is enabled
if [ -z ${TELETRAAN_SERVICE_URL+x} ];
then
    echo "TELETRAAN_SERVICE_URL is not set, use http://localhost:8080";
    export TELETRAAN_SERVICE_URL=http://localhost:8080
fi
export TELETRAAN_SERVICE_VERSION=v1
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TARGET=$DIR/$1
shift
PYTHONPATH=$DIR/.. python $TARGET $*