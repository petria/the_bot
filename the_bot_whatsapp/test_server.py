import unittest
from unittest.mock import patch

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


class MediaHandlerTest(unittest.TestCase):
    def test_media_serves_local_path_from_message(self):
        captured = {}
        handler = handler_with_wacli({
            "stdout": '{"success":true,"data":{"LocalPath":"/wacli/media/image.jpg","MimeType":"image/jpeg"}}',
            "stderr": "",
            "exitCode": 0,
            "timedOut": False,
        })
        handler.path = "/media?chat=120363408176012025%40g.us&id=3EB"
        handler.respond_file = lambda path, data: captured.update({"path": path, "data": data})

        handler.handle_media()

        self.assertEqual("/wacli/media/image.jpg", captured["path"])
        self.assertEqual("image/jpeg", captured["data"]["MimeType"])
        self.assertIn("messages", handler.last_command)
        self.assertIn("show", handler.last_command)
        self.assertIn("120363408176012025@g.us", handler.last_command)
        self.assertIn("3EB", handler.last_command)

    def test_media_returns_conflict_when_not_downloaded(self):
        handler = handler_with_wacli({
            "stdout": '{"success":true,"data":{"LocalPath":"","MimeType":"image/jpeg"}}',
            "stderr": "",
            "exitCode": 0,
            "timedOut": False,
        })
        handler.path = "/media?chat=120363408176012025%40g.us&id=3EB"

        with patch.object(server, "MEDIA_WAIT_SECONDS", 0):
            handler.handle_media()

        self.assertEqual(409, handler.response[0])
        self.assertEqual("media is not downloaded yet", handler.response[1]["message"])


def handler_with_wacli(result):
    handler = server.Handler.__new__(server.Handler)
    def run_wacli_capture(command, timeout):
        handler.last_command = command
        return result
    handler.run_wacli_capture = run_wacli_capture
    handler.respond = lambda status, body: setattr(handler, "response", (status, body))
    handler.headers = {}
    return handler


if __name__ == "__main__":
    unittest.main()
