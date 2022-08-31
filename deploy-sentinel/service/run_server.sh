#!/bin/bash

RUNDIR=/var/run/deploy-sentinel
PIDFILE=${RUNDIR}/deploy-sentinel.pid

NAME="deploy-sentinel"

DAEMON_OPTS="/mnt/deploy-sentinel/service/deploy-sentinel"

function server_start {
    echo -n "Starting ${NAME}: "
    mkdir -p ${RUNDIR}
    touch ${PIDFILE}
    chown -R prod:prod ${RUNDIR}
    chmod 755 ${RUNDIR}
    mkdir -p /var/log/deploy-sentinel

    pwd
    /sbin/start-stop-daemon --start --quiet --umask 007 --pidfile ${PIDFILE} --make-pidfile \
        --exec ${DAEMON_OPTS}>/var/log/deploy-sentinel/deploy-sentinel.log 2>&1 &
    echo "${NAME} started."
}

function server_stop {
    echo -n "Stopping ${DESC}: "
    /sbin/start-stop-daemon --stop --quiet --pidfile ${PIDFILE} --retry=TERM/30/KILL/5
    if [ $? -gt 1 ]; then
        echo "failed to stop ${NAME}."
    else
        echo "${NAME} stopped."
        rm -f ${RUNDIR}/*
    fi
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
