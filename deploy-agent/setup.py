# Copyright 2016 Pinterest, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from setuptools import setup
import os

<<<<<<< HEAD
__version__ = '1.2.9'
=======
__version__ = '1.2.8'
>>>>>>> change the name

markdown_contents = open(os.path.join(os.path.dirname(__file__),
                                      'README.md')).read()

console_scripts = ['deploy-agent = deployd.agent:main',
                   'deploy-downloader = deployd.download.downloader:main',
                   'deploy-stager = deployd.staging.stager:main']

install_requires = [
    "requests==2.9.1",
    "gevent==1.0.2",
    "lockfile==0.10.2",
    "boto>=2.39.0",
    "python-daemon==2.0.6"
]

# Pinterest specific settings
if os.getenv("IS_PINTEREST", "false") == "true":
    extra_requires = [
        "pinlogger>=0.11.0",
        "pinstatsd==1.0.55",
    ]
    install_requires += extra_requires

setup(
    name='deploy-agent',
    version=__version__,
    scripts=['bin/{}'.format(p) for p in os.listdir('bin')],
    long_description=markdown_contents,
    entry_points={
        'console_scripts': console_scripts,
    },
    install_requires=install_requires,
    author="Pinterest",
    packages=[
        'deployd',
        'deployd.download',
        'deployd.common',
        'deployd.staging',
        'deployd.client',
        'deployd.types',
    ],
    include_package_data=True,
    package_data={'deployd/conf': ['*.conf']}
)
