#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

echo "Download and running Teletraan services..."

VERSION=v1.0.0
AGENT_VERSION=1.1.5

cd ~
mkdir teletraan-demo

cd ~/teletraan-demo
mkdir deploy-service
wget -O - https://github.com/pinterest/teletraan/releases/download/${VERSION}/teletraan-service-${VERSION}-bin.tar.gz | tar zxf - -C deploy-service
./deploy-service/bin/run_server.sh &

cd ~/teletraan-demo
virtualenv ./venv
source ./venv/bin/activate
mkdir deploy-board
wget -O - https://github.com/pinterest/teletraan/releases/download/${VERSION}/deploy-board-${VERSION}.tar.gz | tar zxf - -C deploy-board --strip-components=1
cd deploy-board
pip install -r requirements.txt
mkdir /tmp/deploy_board
./run.sh >& /tmp/deploy_board/access.log &

cd ~/teletraan-demo
wget https://github.com/pinterest/teletraan/releases/download/${VERSION}/deploy-agent-${AGENT_VERSION}.zip
pip install deploy-agent==${AGENT_VERSION} --find-links=file:///home/vagrant/teletraan-demo/deploy-agent-${AGENT_VERSION}.zip

echo "Completed Teletraan installation. Access http://localhost:8888 to try it out!"
