#!/bin/bash
# This is for test and dev purpose only
echo "Starting deploy board..."
mkdir -p /tmp/deploy_board
export ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export PYTHONPATH=$ROOT_DIR
python $ROOT_DIR/manage.py runserver 0.0.0.0:8888 >& /tmp/deploy_board/access.log
