from deployd.types.ping_request import PingRequest
from deployd.types.ping_report import PingReport
import unittest


class TestPingRequest(unittest.TestCase):
    def test_to_json_enums(self):
        reports = [
            PingReport(
                {
                    "deployStage": 7,
                    "status": 4,
                }
            ),
            PingReport(
                {
                    "deployStage": "POST_RESTART",
                    "status": "SCRIPT_FAILED",
                }
            ),
        ]
        request = PingRequest(reports=reports)
        json_request = request.to_json()
        self.assertEqual(json_request["reports"][0]["deployStage"], "POST_RESTART")
        self.assertEqual(json_request["reports"][0]["agentStatus"], "SCRIPT_FAILED")
        self.assertEqual(json_request["reports"][1]["deployStage"], "POST_RESTART")
        self.assertEqual(json_request["reports"][1]["agentStatus"], "SCRIPT_FAILED")


if __name__ == "__main__":
    unittest.main()
