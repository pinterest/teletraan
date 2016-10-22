#!/bin/bash

# Exit upon any errors
set -e

# Set defaults
HOST=
PORT=
USER=root
PASSWORD=
SQLFILE=/var/teletraan/sql/deploy.sql
DRYRUN=false

usage() { 
  echo "Usage: deploydb [-H|--host HOST] [-P|--port PORT]"
  echo "               [-u|--user USERNAME] [-p|--password PASSWORD]"
  echo "               [-f|--file FILE]"
  echo "               [-h|--help]" 
  echo "optional arguments:"
  echo "-h, --help              show this help message and exit"
  echo "-H, --host HOST         DB server host name, default is localhost"
  echo "-P, --port HOST         DB server port number, default is 3306"
  echo "-u, --user USERNAME     DB server user name, default is root"
  echo "-p, --password PASSWORD DB server password, default is empty"
  echo "-f, --file SQL file to execute, default is /var/teletraan/sql/deploy.sql"
}

while [[ $# -ge 1 ]]
do
key="$1"

case $key in
    -H|--host)
    HOST="$2"
    shift # past argument
    ;;
    -P|--port)
    PORT="$2"
    shift # past argument
    ;;
    -u|--user)
    USER="$2"
    shift # past argument
    ;;
    -p|--password)
    PASSWORD="$2"
    shift # past argument
    ;;
    -d|--database)
    DATABASE="$2"
    shift # past argument
    ;;
    -f|--file)
    SQLFILE="$2"
    shift # past argument
    ;;
    -h|--help)
    usage #unknown option
    exit 0
    ;;
    *)
    echo "Unknown option $1"
    usage #unknown option
    exit 1
    ;;
esac
shift # past argument or value
done

[ ! -z "$HOST" ] && HOST="--host=$HOST" 
[ ! -z "$PORT" ] && PORT="--port=$PORT" 
[ ! -z "$PASSWORD" ] && PASSWORD="--password=$PASSWORD" 


mysql $HOST $PORT -u $USER $PASSWORD < $SQLFILE
 