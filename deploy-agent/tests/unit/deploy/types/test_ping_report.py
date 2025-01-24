from deployd.common.types import AgentStatus, DeployStage
from deployd.types.ping_report import PingReport
import unittest


class TestPingReport(unittest.TestCase):
    def test_init_enums(self):
        report = PingReport(
            jsonValue={
                "deployStage": DeployStage.UNKNOWN,
                "status": AgentStatus.SUCCEEDED,
            }
        )
        self.assertEqual(report.deployStage, "UNKNOWN")
        self.assertEqual(report.status, "SUCCEEDED")

        report = PingReport(
            jsonValue={
                "deployStage": DeployStage.STOPPED,
                "status": AgentStatus.TOO_MANY_RETRY,
            }
        )
        self.assertEqual(report.deployStage, "STOPPED")
        self.assertEqual(report.status, "TOO_MANY_RETRY")


if __name__ == "__main__":
    unittest.main()
