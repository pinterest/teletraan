import unittest
from unittest import mock
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

    def test_read_host_info_normandie(self):
        client = Client(config=Config())
        client._ec2_tags = {}
        client._availability_zone = "us-east-1"
        return_value: bool = client._read_host_info()
        self.assertTrue(return_value)

        # On a host with normandie, the normandie status should be set to OK
        # On a host without, such as build agents, the normandie status should be ERROR
        self.assertIsNotNone(client._normandie_status)
        self.assertTrue(client._normandie_status == "OK" or client._normandie_status == "ERROR")

    # Normandie status should be ERROR even when the subprocess call returns a non-parseable output
    @mock.patch("subprocess.check_output")
    def test_read_host_info_normandie_error(self, mock_check_output):
        mock_check_output.return_value = b"not a parseable SAN URL"
        client = Client(config=Config())
        client._ec2_tags = {}
        client._availability_zone = "us-east-1"
        return_value: bool = client._read_host_info()
        self.assertTrue(return_value)

        self.assertIsNotNone(client._normandie_status)
        self.assertEqual(client._normandie_status, "ERROR")

    def test_read_host_info_knox(self):
        client = Client(config=Config())
        client._ec2_tags = {}
        client._availability_zone = "us-east-1"
        return_value: bool = client._read_host_info()
        self.assertTrue(return_value)

        # On a host with knox, the knox status should be set to OK
        # On a host without, such as build agents, the knox status should be ERROR
        self.assertIsNotNone(client._knox_status)
        self.assertTrue(client._knox_status == "OK" or client._knox_status == "ERROR")

    def test_read_host_info_no_ec2_tags_provided(self):
        client = Client(config=Config())
        with self.assertRaises(AttributeError):
            client._read_host_info()


if __name__ == "__main__":
    unittest.main()
