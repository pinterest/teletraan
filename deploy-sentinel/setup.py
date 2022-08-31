import datetime
from setuptools import setup
import os

date = datetime.datetime.now().strftime("%Y%m%d.%H%M%S")
__version__ = '0.1.0'
markdown_contents = open(os.path.join(os.path.dirname(__file__),
                                      'README.md')).read()


setup(
    name='deploy-sentinel',
    version=__version__,
    long_description=markdown_contents,
    author="devtools",
    author_email="devtools@pinterest.com",
    packages=[
        'service',
    ],
    package_data={'deploy-sentinel/teletraan': ['*']},
    include_package_data=True
)
