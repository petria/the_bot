import json
import mimetypes
import os
import subprocess
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


PORT = int(os.environ.get("BOT_WHATSAPP_PORT", "8095"))
STORE_DIR = os.environ.get("WACLI_STORE_DIR", "/wacli")
SEND_TOKEN = os.environ.get("BOT_WHATSAPP_SEND_TOKEN", "")
WACLI_BIN = os.environ.get("WACLI_BIN", "wacli")
MEDIA_WAIT_SECONDS = int(os.environ.get("BOT_WHATSAPP_MEDIA_WAIT_SECONDS", "20"))


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.handle_health()
            return
        if self.path == "/status":
            self.handle_status()
            return
        if self.path.startswith("/media"):
            self.handle_media()
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
        reply_to = str(payload.get("replyTo", "")).strip()
        reply_to_sender = str(payload.get("replyToSender", "")).strip()
        if reply_to and not reply_to_sender:
            reply_to_sender = self.resolve_bot_reply_sender(to, reply_to)
        if reply_to:
            command.extend(["--reply-to", reply_to])
        if reply_to_sender:
            command.extend(["--reply-to-sender", reply_to_sender])
        self.run_wacli(command, to)

    def resolve_bot_reply_sender(self, chat, message_id):
        result = self.run_wacli_capture([
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "messages",
            "show",
            "--chat",
            chat,
            "--id",
            message_id,
        ], timeout=10)
        if result["timedOut"] or result["exitCode"] != 0:
            return ""
        body = parse_json_or_text(result["stdout"])
        data = body.get("data") if isinstance(body, dict) else None
        if not isinstance(data, dict) or not data.get("FromMe"):
            return ""

        auth = self.run_wacli_capture([
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "auth",
            "status",
        ], timeout=10)
        if auth["timedOut"] or auth["exitCode"] != 0:
            return ""
        auth_body = parse_json_or_text(auth["stdout"])
        auth_data = auth_body.get("data") if isinstance(auth_body, dict) else None
        return str(auth_data.get("linked_jid", "")).strip() if isinstance(auth_data, dict) else ""

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

    def handle_media(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return

        parsed = urllib.parse.urlparse(self.path)
        query = urllib.parse.parse_qs(parsed.query)
        chat = first_query_value(query, "chat")
        message_id = first_query_value(query, "id")
        if not chat or not message_id:
            self.respond(400, {"status": "NOK", "message": "Both 'chat' and 'id' are required"})
            return

        deadline = time.time() + max(0, MEDIA_WAIT_SECONDS)
        last_body = None
        while True:
            result = self.run_wacli_capture([
                WACLI_BIN,
                "--json",
                "--store",
                STORE_DIR,
                "messages",
                "show",
                "--chat",
                chat,
                "--id",
                message_id,
            ], timeout=10)
            if result["timedOut"]:
                self.respond(504, {"status": "NOK", "message": "wacli messages show timed out"})
                return
            if result["exitCode"] != 0:
                self.respond(502, {
                    "status": "NOK",
                    "message": "wacli messages show failed",
                    "stderr": result["stderr"],
                    "exitCode": result["exitCode"],
                })
                return
            body = parse_json_or_text(result["stdout"])
            last_body = body
            data = body.get("data") if isinstance(body, dict) else None
            local_path = str(data.get("LocalPath", "")).strip() if isinstance(data, dict) else ""
            if local_path:
                self.respond_file(local_path, data)
                return
            if time.time() >= deadline:
                self.respond(409, {
                    "status": "NOK",
                    "message": "media is not downloaded yet",
                    "messageId": message_id,
                    "chat": chat,
                    "wacli": last_body,
                })
                return
            time.sleep(0.5)

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

    def respond_file(self, local_path, message_data):
        path = local_path
        if not os.path.isabs(path):
            path = os.path.join(STORE_DIR, path)
        path = os.path.abspath(path)
        if not os.path.isfile(path):
            self.respond(404, {"status": "NOK", "message": "downloaded media file does not exist", "path": local_path})
            return
        content_type = ""
        if isinstance(message_data, dict):
            content_type = str(message_data.get("MimeType", "")).strip()
        if not content_type:
            content_type = mimetypes.guess_type(path)[0] or "application/octet-stream"
        filename = os.path.basename(path)
        size = os.path.getsize(path)
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(size))
        self.send_header("Content-Disposition", f'inline; filename="{filename}"')
        self.end_headers()
        with open(path, "rb") as handle:
            while True:
                chunk = handle.read(1024 * 64)
                if not chunk:
                    break
                self.wfile.write(chunk)

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


def first_query_value(query, name):
    values = query.get(name)
    if not values:
        return ""
    return str(values[0]).strip()


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"bot-whatsapp wrapper listening on :{PORT}", flush=True)
    server.serve_forever()
