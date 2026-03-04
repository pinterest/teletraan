import os
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
        self.assertTrue(
            client._normandie_status == "OK" or client._normandie_status == "ERROR"
        )

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

    @mock.patch("deployd.client.client.IS_PINTEREST", True)
    def test_client_retries_id_lookup_without_cache_when_missing(self):
        """
        Validates:
        - When _use_facter=True and initial facter lookup returns no id,
          client retries with no_cache=True.
        - If retry returns id, method succeeds (does not return False).
        """

        config_file = os.path.join(
            os.path.dirname(os.path.abspath(__file__)), "deployagent.conf"
        )
        client = Client(config=Config(config_file))
        client._use_facter = True
        client._id = None

        # First call (cached) returns missing id; second call (no_cache) returns id.
        side_effects = [
            {"ec2_instance_id": ""},  # initial cached facter result
            {"ec2_instance_id": "i-abc"},  # retry without cache
            {"ec2_placement_availability_zone": "us-east-1a"},  # subsequent request
        ]

        with mock.patch(
            "deployd.common.utils.get_info_from_facter", side_effect=side_effects
        ) as m:
            client._read_host_info()

        assert client._id == "i-abc"

        # Ensure second call used no_cache=True
        assert m.call_args_list == [
            mock.call(
                {
                    "ec2_local_ipv4",
                    "hostname",
                    "ec2_instance_id",
                    "deploy_service_combined",
                }
            ),  # original call (no no_cache arg)
            mock.call({"ec2_instance_id"}, True),  # retry with no_cache=True
            mock.call(
                {
                    "ec2_metadata.placement.availability-zone",
                    "ec2_tags",
                    "deploy_service_stage_type",
                    "ec2_placement_availability_zone",
                    "ec2_metadata.identity-credentials.ec2.info",
                }
            ),
        ]

    @mock.patch("deployd.client.client.IS_PINTEREST", True)
    def test_client_retries_availability_zone_without_cache_when_missing(self):
        """
        Validates:
        - When AZ is missing from cached facter data, client retries with no_cache=True.
        - If retry returns AZ, client sets _availability_zone.
        """

        config_file = os.path.join(
            os.path.dirname(os.path.abspath(__file__)), "deployagent.conf"
        )
        client = Client(config=Config(config_file))
        client._use_facter = True
        client._availability_zone = None

        # First call (cached) returns no AZ; second call returns AZ.
        side_effects = [
            {"ec2_instance_id": "i-abc"},  # first request
            {"ec2_placement_availability_zone": ""},  # cached facter result missing AZ
            {
                "ec2_placement_availability_zone": "us-east-1a"
            },  # no-cache retry returns AZ
        ]

        with mock.patch(
            "deployd.common.utils.get_info_from_facter", side_effect=side_effects
        ) as m:
            client._read_host_info()

        assert client._availability_zone == "us-east-1a"

        assert m.call_args_list == [
            mock.call(
                {
                    "deploy_service_combined",
                    "ec2_instance_id",
                    "ec2_local_ipv4",
                    "hostname",
                }
            ),
            mock.call(
                {
                    "ec2_metadata.placement.availability-zone",
                    "ec2_tags",
                    "deploy_service_stage_type",
                    "ec2_placement_availability_zone",
                    "ec2_metadata.identity-credentials.ec2.info",
                }
            ),  # original call (no no_cache arg)
            mock.call(
                {
                    "ec2_metadata.placement.availability-zone",
                    "ec2_placement_availability_zone",
                },
                True,
            ),  # retry with no_cache=True
        ]


if __name__ == "__main__":
    unittest.main()
