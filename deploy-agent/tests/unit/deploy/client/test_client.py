import unittest
from tests import TestCase

from deployd.client.client import Client
from deployd.client.base_client import BaseClient
from deployd.common.config import Config


class TestClient(TestCase):
    def test_extends_base_client(self):
        self.assertTrue(issubclass(Client, BaseClient))

    def test_read_host_info(self):
        client = Client(config=Config())
        client._ec2_tags = {}
        client._availability_zone = "us-east-1"
        return_value: bool = client._read_host_info()
        self.assertIsNotNone(client._hostname)
        self.assertIsNotNone(client._ip)
        self.assertTrue(return_value)

    def test_read_host_info_no_ec2_tags_provided(self):
        client = Client(config=Config())
        with self.assertRaises(AttributeError):
            client._read_host_info()

if __name__ == '__main__':
    unittest.main()
