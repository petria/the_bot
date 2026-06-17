import unittest

import server


class HealthHandlerTest(unittest.TestCase):
    def test_health_returns_ok_when_authenticated(self):
        handler = handler_with_wacli({
            "stdout": '{"success":true,"data":{"authenticated":true}}',
            "stderr": "",
            "exitCode": 0,
            "timedOut": False,
        })

        handler.handle_health()

        self.assertEqual(200, handler.response[0])
        self.assertEqual("OK", handler.response[1]["status"])
        self.assertTrue(handler.response[1]["authenticated"])

    def test_health_returns_unavailable_when_not_authenticated(self):
        handler = handler_with_wacli({
            "stdout": '{"success":true,"data":{"authenticated":false}}',
            "stderr": "",
            "exitCode": 0,
            "timedOut": False,
        })

        handler.handle_health()

        self.assertEqual(503, handler.response[0])
        self.assertEqual("NOK", handler.response[1]["status"])
        self.assertEqual("not authenticated", handler.response[1]["message"])

    def test_health_returns_unavailable_when_wacli_fails(self):
        handler = handler_with_wacli({
            "stdout": "",
            "stderr": "not authenticated; run `wacli auth`",
            "exitCode": 1,
            "timedOut": False,
        })

        handler.handle_health()

        self.assertEqual(503, handler.response[0])
        self.assertEqual("wacli auth status failed", handler.response[1]["message"])

    def test_health_returns_unavailable_when_wacli_times_out(self):
        handler = handler_with_wacli({
            "stdout": "",
            "stderr": "",
            "exitCode": -1,
            "timedOut": True,
        })

        handler.handle_health()

        self.assertEqual(503, handler.response[0])
        self.assertEqual("wacli auth status timed out", handler.response[1]["message"])


def handler_with_wacli(result):
    handler = server.Handler.__new__(server.Handler)
    handler.run_wacli_capture = lambda command, timeout: result
    handler.respond = lambda status, body: setattr(handler, "response", (status, body))
    return handler


if __name__ == "__main__":
    unittest.main()
