#!/bin/bash -ex
#
# This script takes a standard ubuntu box and installs all the software needed to run teletraan locally.

echo "Install java and python..."

# Add Java 8 repo
#add-apt-repository ppa:webupd8team/java
add-apt-repository ppa:openjdk-r/ppa

apt-get update

# Accept license agreement and install java
#echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
#apt-get install -y oracle-java8-installer
apt-get install -y openjdk-8-jre

# Install some dev tools
apt-get install -y python python-pip python-virtualenv

# Install and config mysql
export DEBIAN_FRONTEND=noninteractive
apt-get install -q -y mysql-server mysql-client
mysql -u root < /home/vagrant/teletraan/deploy-service/common/src/main/resources/sql/deploy.sql

echo "Completed install!"
