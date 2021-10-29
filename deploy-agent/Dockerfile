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


# tox.ini setting recreate=True does not seem to be respected in docker
RUN rm -rf .tox

RUN python setup.py build

ENTRYPOINT ["bash", "-c", "tox"]

#ENTRYPOINT ["bash"]
#RUN ["bash"]
#ENTRYPOINT ["deploy-agent"]
