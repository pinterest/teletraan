#!/bin/bash
##########################################################################################
# 1. The default run.sh and manage.py are provided mainly for development and demo purpose.
#    It is highly recommended to create your own run.sh and manage.yaml for production use,
#    so that you can customize deployboard configs.
#
# 2. The default run.sh provide an unreliable but portable way of running deployboard in
#    background. It is highly recommended to use your favorite tools to daemonize it properly,
#    such as use start-stop-daemon, init, runsv (from runit), upstart, systemd, and etc.
#    Also, use monit, supervisor or other process monitoring tools to monitor your process.
##########################################################################################

# Specify all the defaults
ROOT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
MANAGE_FILE=${ROOT_DIR}/manage.py
ADDR_PORT=0.0.0.0:8888
LOG_DIR=/tmp/deploy_board
PID_FILE=${HOME}/deploy_board.pid
ACTION="run"

display_usage() {
	echo -e "Usage: $0 [OPTIONS] [run|start|stop|restart]"
	echo -e "\nOPTIONS:"
	echo -e "  -a/--address     Address and port, default is ${ADDR_PORT}"
	echo -e "  -m/--manage-file Manage file, default is ${MANAGE_FILE}"
	echo -e "  -p/--pid         PID file, default is ${PID_FILE}"
	echo -e "  -d/--log-dir     Log directory, default is ${LOG_DIR}"
	echo -e "\nACTION:"
	echo -e "  run     Run service in foreground. This is the default action."
	echo -e "  start   Run service in background."
	echo -e "  stop    Stop the service running in background."
	echo -e "  restart Restart the service running in background."
}

function server_start {
    echo "Starting Deployboard..."

    mkdir -p ${LOG_DIR}
    export IS_PINTEREST=true
    export PYTHONPATH=${ROOT_DIR}
    if [ "$1" == "FOREGROUND" ]
    then
        python ${MANAGE_FILE} runserver ${ADDR_PORT}
    else
        python ${MANAGE_FILE} runserver ${ADDR_PORT} >& ${LOG_DIR}/access.log &
        echo $! > ${PID_FILE}
        echo "Deployboard started."
    fi
}

function server_stop {
    PID=$(cat ${PID_FILE})
    kill -- -$(ps -o pgid= ${PID} | grep -o [0-9]*)
    rm -fr ${PID_FILE}
    echo "Deployboard stopped."
}

function action {
    case "$1" in

    run)
    server_start "FOREGROUND"
    ;;

    start)
    server_start "BACKGROUND"
    ;;

    stop)
    server_stop
    ;;

    restart)
    server_stop
    server_start
    ;;

    esac
}

while [[ $# > 0 ]]
do
key="$1"

case $key in
    -a|--address)
    ADDR_PORT="$2"
    shift # past argument
    ;;
    -m|--manage-file)
    MANAGE_FILE="$2"
    shift # past argument
    ;;
    -d|--log-dir)
    LOG_DIR="$2"
    shift # past argument
    ;;
    -p|--pid)
    PID_FILE="$2"
    shift # past argument
    ;;
    -h|--help)
    display_usage
	exit 0
	;;
    run|start|stop|restart)
    ACTION="$1"
    ;;
    *)
    display_usage
	exit 1
    ;;
esac
shift # past argument or value
done

action ${ACTION}

exit 0
