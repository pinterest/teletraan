FROM python:2.7
ENV PROJECT_DIR=/opt/deploy-agent
RUN mkdir $PROJECT_DIR
WORKDIR $PROJECT_DIR
ADD . $PROJECT_DIR
RUN python setup.py build
# For whatever stupid reasons, rerun install will fix the following
# error: The 'python-daemon==2.0.5' distribution was not found
RUN python setup.py install; exit 0
RUN python setup.py install

ENTRYPOINT ["deploy-agent"]
