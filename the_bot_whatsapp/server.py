import json
import os
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


PORT = int(os.environ.get("BOT_WHATSAPP_PORT", "8095"))
STORE_DIR = os.environ.get("WACLI_STORE_DIR", "/wacli")
SEND_TOKEN = os.environ.get("BOT_WHATSAPP_SEND_TOKEN", "")
WACLI_BIN = os.environ.get("WACLI_BIN", "wacli")


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.handle_health()
            return
        if self.path == "/status":
            self.handle_status()
            return
        self.respond(404, {"status": "NOK", "message": "Not found"})

    def do_POST(self):
        if self.path == "/send":
            self.handle_send()
            return
        if self.path == "/presence":
            self.handle_presence()
            return
        self.respond(404, {"status": "NOK", "message": "Not found"})

    def handle_send(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return

        payload = self.read_json_payload()
        if payload is None:
            return

        to = str(payload.get("to", "")).strip()
        message = str(payload.get("message", "")).strip()
        if not to or not message:
            self.respond(400, {"status": "NOK", "message": "Both 'to' and 'message' are required"})
            return

        command = [
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "send",
            "text",
            "--to",
            to,
            "--message",
            message,
        ]
        self.run_wacli(command, to)

    def handle_presence(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return

        payload = self.read_json_payload()
        if payload is None:
            return

        to = str(payload.get("to", "")).strip()
        presence = str(payload.get("presence", "typing")).strip()
        if not to:
            self.respond(400, {"status": "NOK", "message": "'to' is required"})
            return
        if presence not in ("typing", "paused"):
            self.respond(400, {"status": "NOK", "message": "Unsupported presence"})
            return

        command = [
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "presence",
            presence,
            "--to",
            to,
        ]
        media = str(payload.get("media", "")).strip()
        if media:
            command.extend(["--media", media])
        self.run_wacli(command, to)

    def handle_health(self):
        result = self.run_wacli_capture([
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "auth",
            "status",
        ], timeout=10)
        if result["timedOut"]:
            self.respond(503, {"status": "NOK", "message": "wacli auth status timed out"})
            return
        if result["exitCode"] != 0:
            self.respond(503, {
                "status": "NOK",
                "message": "wacli auth status failed",
                "stderr": result["stderr"],
                "exitCode": result["exitCode"],
            })
            return
        body = parse_json_or_text(result["stdout"])
        authenticated = bool(
            isinstance(body, dict)
            and body.get("success") is True
            and isinstance(body.get("data"), dict)
            and body["data"].get("authenticated") is True
        )
        if not authenticated:
            self.respond(503, {"status": "NOK", "message": "not authenticated", "wacli": body})
            return
        self.respond(200, {"status": "OK", "authenticated": True, "wacli": body})

    def handle_status(self):
        result = self.run_wacli_capture([
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "doctor",
        ], timeout=15)
        status = 200 if result["exitCode"] == 0 and not result["timedOut"] else 503
        self.respond(status, {
            "status": "OK" if status == 200 else "NOK",
            "stdout": parse_json_or_text(result["stdout"]),
            "stderr": result["stderr"],
            "exitCode": result["exitCode"],
            "timedOut": result["timedOut"],
        })

    def authorized(self):
        return not SEND_TOKEN or self.headers.get("X-Bot-Whatsapp-Token") == SEND_TOKEN

    def read_json_payload(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            return json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception as exc:
            self.respond(400, {"status": "NOK", "message": f"Invalid JSON: {exc}"})
            return None

    def run_wacli(self, command, to):
        result = self.run_wacli_capture(command, timeout=90)
        if result["timedOut"]:
            self.respond(504, {"status": "NOK", "to": to, "message": "wacli command timed out"})
            return
        body = {
            "status": "OK" if result["exitCode"] == 0 else "NOK",
            "to": to,
            "stdout": parse_json_or_text(result["stdout"]),
            "stderr": result["stderr"],
            "exitCode": result["exitCode"],
        }
        self.respond(200 if result["exitCode"] == 0 else 502, body)

    def run_wacli_capture(self, command, timeout):
        try:
            result = subprocess.run(command, text=True, capture_output=True, timeout=timeout)
        except subprocess.TimeoutExpired:
            return {
                "stdout": "",
                "stderr": "",
                "exitCode": -1,
                "timedOut": True,
            }
        return {
            "stdout": result.stdout,
            "stderr": result.stderr.strip(),
            "exitCode": result.returncode,
            "timedOut": False,
        }

    def respond(self, status, body):
        raw = json.dumps(body).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


def parse_json_or_text(value):
    text = value.strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except Exception:
        return text


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"bot-whatsapp wrapper listening on :{PORT}", flush=True)
    server.serve_forever()
