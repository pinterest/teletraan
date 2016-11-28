#!/bin/bash

# Exit upon any errors
set -e

# Set defaults
HOST=
PORT=
USER=root
PASSWORD=
DATABASE=deploy
CURRENT_VERSION=
TARGET_VERSION=
DRYRUN=false

usage() { 
  echo "Usage: upgrade [-H|--host HOST] [-P|--port PORT]"
  echo "               [-u|--user USERNAME] [-p|--password PASSWORD]"
  echo "               [-d|--database DATABASE] [-v|--version VERSION]"
  echo "               [--dry-run] [-h|--help]" 
  echo "optional arguments:"
  echo "-h, --help              show this help message and exit"
  echo "-H, --host HOST         DB server host name, default is localhost"
  echo "-P, --port HOST         DB server port number, default is 3306"
  echo "-u, --user USERNAME     DB server user name, default is root"
  echo "-p, --password PASSWORD DB server password, default is empty"
  echo "-d, --database DATABASE database to use, default is deploy"
  echo "-v, --version VERSION   the schema version to upgrade to, default is the highest version possible"
  echo "--dry-run               print upgrade route without action"
}

resolve_target_version() {
  # list all files like schema-update-N.sql, and use the largest N as the target version
  TARGET_VERSION=0
  for f in schema-update-*.sql
  do
    tmp=${f#schema-update*-}
    num=${tmp%.sql}
    if (( num > TARGET_VERSION )); then
        TARGET_VERSION=$num
    fi
  done
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
    -v|--version)
    TARGET_VERSION="$2"
    shift # past argument
    ;;
    -h|--help)
    usage #unknown option
    exit 0
    ;;
    --dry-run)
    DRYRUN=true
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

CURRENT_VERSION=$(mysql $HOST $PORT -u $USER $PASSWORD $DATABASE -s -N < /var/teletraan/tools/check_version.sql)
[ -z "$TARGET_VERSION" ] && resolve_target_version
echo "Will upgrade schema from version ${CURRENT_VERSION} to version ${TARGET_VERSION}"

for ((i=CURRENT_VERSION+1;i<=TARGET_VERSION;i++)); do
    echo "upgrading to version ${i} with schema-update-$i.sql..."
    if [ "$DRYRUN" == "true" ]; then
        continue
    fi
    mysql $HOST $PORT -u $USER $PASSWORD $DATABASE < /var/teletraan/tools/schema-update-$i.sql
done

echo "Successfully upgraded schema from version ${CURRENT_VERSION} to version ${TARGET_VERSION}"
