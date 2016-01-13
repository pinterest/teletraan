#!/bin/bash

LOG_DIR=/tmp/teletraan
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$(dirname ${DIR})"
TARGET_DIR="${ROOT_DIR}/target"
CP=${ROOT_DIR}/*:${ROOT_DIR}/lib/*:${TARGET_DIR}/classes:${TARGET_DIR}/lib/*

java -server -Xmx1024m -Xms1024m -verbosegc -Xloggc:${LOG_DIR}/gc.log \
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+PrintClassHistogram \
-XX:+HeapDumpOnOutOfMemoryError -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:ErrorFile=${LOG_DIR}/jvm_error.log \
-XX:ConcGCThreads=7 -XX:ParallelGCThreads=7 \
-cp ${CP} -Dlog4j.configuration=${ROOT_DIR}/bin/log4j.properties \
com.pinterest.teletraan.TeletraanService \
server ${ROOT_DIR}/bin/server.yaml
