#!/bin/bash

# Exit upon any errors
set -e

cd /var/teletraan/tools

./deploydb.sh -f ../sql/deploy.sql
./upgrade.sh
