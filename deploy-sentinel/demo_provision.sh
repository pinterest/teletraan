#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

echo "Install Teletraan runtime dependencies..."

# Add Java 8 repo
#add-apt-repository ppa:webupd8team/java
add-apt-repository ppa:openjdk-r/ppa

apt-get update

# Accept license agreement and install java
#echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
#apt-get install -y oracle-java8-installer
echo "Install openjdk 8..."
apt-get install -y openjdk-8-jre
echo "Successfully installed openjdk 8"

echo "Install python and related tools..."
apt-get install -y python python-pip python-virtualenv python-dev
echo "Successfully installed python"

echo "Successfully completed Teletraan dependencies install!"

su -c "source /home/vagrant/teletraan/deploy-sentinel/demo_run.sh" vagrant
