from deployd.types.ping_report import PingReport 
import unittest


class TestPingReport(unittest.TestCase):
    def test_init_enums(self):
        report = PingReport(jsonValue={
            "deployStage": 0,
            "status": 0,
            })
        self.assertEqual(report.deployStage, "UNKNOWN") 
        self.assertEqual(report.status, "SUCCEEDED") 

        report = PingReport(jsonValue={
            "deployStage": 10,
            "status": 7,
            })
        self.assertEqual(report.deployStage, "STOPPED") 
        self.assertEqual(report.status, "TOO_MANY_RETRY") 


if __name__ == "__main__":
    unittest.main()
