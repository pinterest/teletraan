#FROM python:2.7
FROM ubuntu:18.04

RUN apt-get update
RUN apt-get upgrade


RUN apt-get install -y git vim
RUN apt-get install -y build-essential
RUN apt-get install -y libssl-dev libreadline-dev libsqlite3-dev
RUN apt-get install -y liblzma-dev libbz2-dev zlib1g-dev libffi-dev
RUN apt-get install -y python-dev python-setuptools python3-dev python3-setuptools python-pip python3-pip
RUN apt-get install -y tox

# python3 tests appear to need curl
RUN apt-get install -y curl

ENV PROJECT_DIR=/opt/deploy-agent
RUN mkdir $PROJECT_DIR
WORKDIR $PROJECT_DIR
ADD . $PROJECT_DIR


# tox recreate=True does not seem to be respected, test later
RUN rm -rf .tox
RUN python setup.py build
# For whatever stupid reasons, rerun install will fix the following
# error: The 'python-daemon==2.0.5' distribution was not found
#RUN python setup.py install; exit 0
#RUN python setup.py install

RUN ["bash"]
#ENTRYPOINT ["deploy-agent"]
