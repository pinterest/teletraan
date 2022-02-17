#!/bin/bash
export TELETRAAN_SERVICE_URL=http://localhost:8080
export TELETRAAN_SERVICE_VERSION=v1
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TARGET=$DIR/$1
shift
PYTHONPATH=$DIR/.. python $TARGET $*
