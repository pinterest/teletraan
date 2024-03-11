#!/usr/bin/env python3

from distutils.core import setup

setup(name='teletraan-deploy_board',
      version='1.0.0.dev0',
      description='The user interface component of Teletraan.',
      author='Pinterest',
      author_email='help@pinterest.com',
      url='https://github.com/pinterest/teletraan',
      packages=['deploy_board',
                'deploy_board.webapp',
                'deploy_board.webapp.helpers',
                'deploy_board.tools'],
      package_dir={'deploy_board.tools': 'tools'},
      )
