#!/bin/bash

if [ $# -lt 1 ]
then
    echo "Usage : $0 [start|stop|restart]"
    exit
fi

LOG_DIR=/tmp/teletraan
mkdir -p $LOG_DIR
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname ${DIR})"
TARGET_DIR="${ROOT_DIR}/target"
CP=${ROOT_DIR}/*:${ROOT_DIR}/lib/*:${TARGET_DIR}/classes:${TARGET_DIR}/lib/*

function server_start {
    echo "Starting Teletraan server..."

    java -server -Xmx1024m -Xms1024m -verbosegc -Xloggc:${LOG_DIR}/gc.log \
    -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintClassHistogram \
    -XX:+HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ErrorFile=${LOG_DIR}/jvm_error.log \
    -XX:ConcGCThreads=7 -XX:ParallelGCThreads=7 -cp ${CP} \
    com.pinterest.teletraan.TeletraanService \
    server ${ROOT_DIR}/bin/server.yaml &

    echo "Teletraan server started. Check log at /tmp/teletraan/service.log"
}

function server_stop {
    echo "Stopping Teletraan server..."
    pkill -f "com.pinterest.teletraan.TeletraanService"
    pkill -f "/tmp/teletraan/db"
    echo "Teletraan server stopped."
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
