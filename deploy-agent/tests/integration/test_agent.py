import argparse
from deployd.agent import DeployAgent
from deployd.client.client import Client
from deployd.common.config import Config
import logging

logging.basicConfig(level=logging.DEBUG)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('-e', '--env_stage', dest='stage', default='prod')
    args = parser.parse_args()
    config = Config(filenames='deployd/conf/agent.conf')
    client = Client(config=config)
    agent = DeployAgent(client=client, conf=config)
    agent.serve_build()
