#!/bin/bash

if [ $# -lt 1 ]
then
    echo "Usage : $0 [start|stop|restart]"
    exit
fi

function server_start {
    echo "Starting Deployboard..."

    mkdir -p /tmp/deploy_board
    export ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    export PYTHONPATH=$ROOT_DIR
    python $ROOT_DIR/manage.py runserver 0.0.0.0:8888 >& /tmp/deploy_board/access.log &

    echo "Deployboard started. Check log at /tmp/deploy_board/service.log"
}

function server_stop {
    echo "Stopping Deployboard..."
    pkill -f "teletraan/deploy-board/manage.py runserver"
    echo "Deployboard stopped."
}

case "$1" in

    start)
    server_start
    ;;

    stop)
    server_stop
    ;;

    restart)
    server_stop
    server_start
    ;;

esac

exit 0
