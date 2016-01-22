#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

echo "Download and running Teletraan services..."

RELEASE_VERSION=v1.0.1
DEPLOY_SERVICE_VERSION=fdd68c0
DEPLOY_BOARD_VERSION=fdd68c0
DEPLOY_SENTINEL_VERSION=fdd68c0
AGENT_VERSION=1.2.2

cd ~
mkdir teletraan-demo

echo "Install Teletraan server..."
cd ~/teletraan-demo
mkdir deploy-service
wget -O - https://github.com/pinterest/teletraan/releases/download/${RELEASE_VERSION}/teletraan-service-${DEPLOY_SERVICE_VERSION}.tar.gz | tar zxf - -C deploy-service
./deploy-service/bin/run.sh start
echo "Successfully installed Teletraan Server"

echo "Install Deploy Board..."
cd ~/teletraan-demo
virtualenv ./venv
source ./venv/bin/activate
mkdir deploy-board
wget -O - https://github.com/pinterest/teletraan/releases/download/${RELEASE_VERSION}/deploy-board-${DEPLOY_BOARD_VERSION}.tar.gz | tar zxf - -C deploy-board --strip-components=1
cd deploy-board
pip install -r requirements.txt
./run.sh start
echo "Successfully installed Deploy Board"

echo "Install Deploy Sentinel..."
cd ~/teletraan-demo
mkdir deploy-sentinel
wget -O - https://github.com/pinterest/teletraan/releases/download/${RELEASE_VERSION}/deploy-sentinel-${DEPLOY_SENTINEL_VERSION}.tar.gz | tar zxf - -C deploy-sentinel --strip-components=1
echo "Successfully installed deploy-sentinel"

echo "Install Deploy Agent..."
cd ~/teletraan-demo
wget https://github.com/pinterest/teletraan/releases/download/${RELEASE_VERSION}/deploy-agent-${AGENT_VERSION}.zip
pip install deploy-agent==${AGENT_VERSION} --find-links=file:///home/vagrant/teletraan-demo/deploy-agent-${AGENT_VERSION}.zip
echo "Successfully installed Deploy Agent"

echo "Successfully completed Teletraan installation. Access http://localhost:8888 to try it out!"
