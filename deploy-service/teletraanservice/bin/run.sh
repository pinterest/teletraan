#!/bin/bash
##########################################################################################
# 1. The default run.sh and server.yaml are provided mainly for development and demo purpose.
#    It is highly recommended to create your own run.sh and server.yaml for production use,
#    so that you can fine tune JVM options and customize server configs.
#
# 2. The default run.sh provide an simple but portable way of running Teletraan in background.
#    It is highly recommended to use your favorite tools to daemonize it properly, such as
#    use start-stop-daemon, init, runsv (from runit), upstart, systemd, and etc.
#    Also, use monit, supervisor or other process monitoring tools to monitor your process.
##########################################################################################

# Specify all the defaults
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname ${DIR})"
TARGET_DIR="${ROOT_DIR}/target"
CP=${ROOT_DIR}/*:${ROOT_DIR}/lib/*:${TARGET_DIR}/classes:${TARGET_DIR}/lib/*
CONFIG_FILE=${ROOT_DIR}/bin/server.yaml
PID_FILE=${HOME}/teletraan.pid
ACTION="run"
JAVA_OPTS="-server -Xmx1024m -Xms1024m \
-verbosegc -Xloggc:/tmp/gc.log -XX:ErrorFile=/tmp/jvm_error.log \
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintClassHistogram \
-XX:+HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC -XX:+UseParNewGC \
-XX:ConcGCThreads=7 -XX:ParallelGCThreads=7"
JAVA_CMD="java"
if [ "x$JAVA_HOME" != "x" ]; then JAVA_CMD=${JAVA_HOME}/bin/java; fi

display_usage() {
	echo -e "Usage: $0 [OPTIONS] [run|start|stop|restart]"
	echo -e "\nOPTIONS:"
	echo -e "  -c/--config     Config file, default is ${CONFIG_FILE}"
	echo -e "  -i/--pid        PID file, default is ${PID_FILE}"
	echo -e "  -p/--class-path Class path, default is ${CP}"
	echo -e "  -o/--java-opts  JVM options, default is ${JAVA_OPTS}"
	echo -e "\nACTION:"
	echo -e "  run     Run service in foreground. This is the default action."
	echo -e "  start   Run service in background."
	echo -e "  stop    Stop the service running in background."
	echo -e "  restart Restart the service running in background."
}

function server_start {
    echo "Starting Teletraan server..."

    OPTS="${JAVA_OPTS} \
    -cp ${CP} \
    com.pinterest.teletraan.TeletraanService \
    server ${CONFIG_FILE}"

    echo $OPTS

    if [ "$1" == "FOREGROUND" ]
    then
        ${JAVA_CMD} ${OPTS}
    else
        ${JAVA_CMD} ${OPTS} &
        echo $! > ${PID_FILE}
        echo "Teletraan server started."
    fi
}

function server_stop {
    kill -TERM $(cat ${PID_FILE})
    rm -fr ${PID_FILE}
    echo "Teletraan server stopped."
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
    -c|--config)
    CONFIG_FILE="$2"
    shift # past argument
    ;;
    -p|--class-path)
    CP="$2"
    shift # past argument
    ;;
    -o|--java-opts)
    JAVA_OPTS="$2"
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
