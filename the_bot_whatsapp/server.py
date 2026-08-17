import json
import mimetypes
import os
import re
import signal
import subprocess
import sys
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


PORT = int(os.environ.get("BOT_WHATSAPP_PORT", "8095"))
STORE_DIR = os.environ.get("WACLI_STORE_DIR", "/wacli")
SEND_TOKEN = os.environ.get("BOT_WHATSAPP_SEND_TOKEN", "")
INTERNAL_TOKEN = os.environ.get("BOT_WHATSAPP_INTERNAL_TOKEN", "")
WACLI_BIN = os.environ.get("WACLI_BIN", "wacli")
MEDIA_WAIT_SECONDS = int(os.environ.get("BOT_WHATSAPP_MEDIA_WAIT_SECONDS", "20"))
AUTH_TIMEOUT_SECONDS = int(os.environ.get("BOT_WHATSAPP_AUTH_TIMEOUT_SECONDS", "300"))
QR_TTL_SECONDS = int(os.environ.get("BOT_WHATSAPP_QR_TTL_SECONDS", "120"))
INTERNAL_TOKEN_HEADER = "X-TheBot-Internal-Token"


class RuntimeCoordinator:
    """Owns the processes that access the single wacli store."""

    def __init__(self, sync_command):
        self.sync_command = list(sync_command)
        self.lock = threading.RLock()
        self.sync_process = None
        self.auth_process = None
        self.auth_started_at = None
        self.auth_method = None
        self.state = "STARTING"
        self.authenticated = False
        self.message = None
        self.pairing_code = None
        self.qr_payload = None
        self.qr_expires_at = None
        self.last_error = None
        self.sync_restart_at = 0
        self.stopping = False
        self.monitor_thread = None

    def start(self):
        with self.lock:
            self.authenticated = self._check_authenticated_locked()
            if self.authenticated:
                self.state = "AUTHENTICATED"
                self._start_sync_locked()
            else:
                self.state = "UNAUTHENTICATED"
                self.message = "WhatsApp is not authenticated"
        self.monitor_thread = threading.Thread(target=self._monitor, name="wacli-runtime", daemon=True)
        self.monitor_thread.start()

    def stop(self):
        with self.lock:
            self.stopping = True
            self._terminate_process_locked(self.auth_process)
            self._terminate_process_locked(self.sync_process)

    def snapshot(self):
        with self.lock:
            qr_expires_at = self.qr_expires_at if self.qr_payload and self.qr_expires_at and self.qr_expires_at > time.time() else None
            return {
                "state": self.state,
                "authenticated": self.authenticated,
                "syncRunning": self.sync_process is not None and self.sync_process.poll() is None,
                "authRunning": self.auth_process is not None and self.auth_process.poll() is None,
                "method": self.auth_method,
                "qrPayload": self.qr_payload if qr_expires_at else None,
                "qrExpiresAt": int(qr_expires_at * 1000) if qr_expires_at else None,
                "pairingCode": self.pairing_code,
                "message": self.message,
                "error": self.last_error,
            }

    def authentication_in_progress(self):
        with self.lock:
            return self.auth_process is not None and self.auth_process.poll() is None

    def start_auth(self, method, phone=None):
        if method not in ("qr", "phone"):
            return False, "Authentication method must be 'qr' or 'phone'"
        if method == "phone":
            phone = normalize_phone(phone)
            if not phone:
                return False, "A valid E.164 phone number is required"

        with self.lock:
            if self.stopping:
                return False, "WhatsApp runtime is stopping"
            if self.auth_process is not None:
                return False, "Authentication is already in progress"
            self._terminate_process_locked(self.sync_process)
            self.sync_process = None
            self.sync_restart_at = 0
            self.auth_method = method
            self.auth_started_at = time.time()
            self.state = "STARTING"
            self.authenticated = False
            self.message = "Starting WhatsApp authentication"
            self.last_error = None
            self.pairing_code = None
            self.qr_payload = None
            self.qr_expires_at = None
            command = [WACLI_BIN, "--store", STORE_DIR, "auth"]
            if method == "qr":
                command.extend(["--qr-format", "text"])
            else:
                command.extend(["--phone", phone])
            try:
                self.auth_process = subprocess.Popen(
                    command,
                    text=True,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    bufsize=1,
                )
            except Exception as exc:
                self.auth_process = None
                self.state = "FAILED"
                self.last_error = str(exc)
                self.message = "Could not start WhatsApp authentication"
                return False, self.last_error
            process = self.auth_process
            threading.Thread(target=self._read_auth_output, args=(process, process.stdout, False), daemon=True).start()
            threading.Thread(target=self._read_auth_output, args=(process, process.stderr, True), daemon=True).start()
            threading.Thread(target=self._wait_for_auth, args=(process,), daemon=True).start()
            return True, None

    def cancel_auth(self):
        with self.lock:
            if self.auth_process is None or self.auth_process.poll() is not None:
                return False, "Authentication is not running"
            self._terminate_process_locked(self.auth_process)
            self.auth_process = None
            self.auth_started_at = None
            self.state = "CANCELLED"
            self.message = "WhatsApp authentication was cancelled"
            self.qr_payload = None
            self.qr_expires_at = None
            self.pairing_code = None
            if self._check_authenticated_locked():
                self.authenticated = True
                self.state = "AUTHENTICATED"
                self.message = "Authentication cancelled; existing WhatsApp session retained"
                self._start_sync_locked()
            return True, None

    def doctor_allowed(self):
        return not self.authentication_in_progress()

    def _monitor(self):
        while True:
            time.sleep(0.5)
            with self.lock:
                if self.stopping:
                    return
                auth_process = self.auth_process
                if auth_process is not None and auth_process.poll() is None:
                    if self.auth_started_at and time.time() - self.auth_started_at > AUTH_TIMEOUT_SECONDS:
                        self._terminate_process_locked(auth_process)
                        self.auth_process = None
                        self.state = "FAILED"
                        self.last_error = "Authentication timed out"
                        self.message = "WhatsApp authentication timed out"
                        self.qr_payload = None
                        self.qr_expires_at = None
                        self.pairing_code = None
                        self.auth_started_at = None
                        if self._check_authenticated_locked():
                            self.authenticated = True
                            self.state = "AUTHENTICATED"
                            self._start_sync_locked()
                        continue
                if (self.auth_process is None and self.sync_process is None
                        and self.state in ("AUTHENTICATED", "STARTING")
                        and time.time() >= self.sync_restart_at):
                    if self._check_authenticated_locked():
                        self.authenticated = True
                        self.state = "AUTHENTICATED"
                        self._start_sync_locked()

                sync_process = self.sync_process
                if sync_process is not None and sync_process.poll() is not None:
                    self.sync_process = None
                    self.sync_restart_at = time.time() + 2
                    if self._check_authenticated_locked():
                        self.authenticated = True
                        self.state = "AUTHENTICATED"
                        self.message = "wacli sync stopped; restarting"
                        self._start_sync_locked()
                    else:
                        self.authenticated = False
                        self.state = "UNAUTHENTICATED"
                        self.message = "WhatsApp is not authenticated"

    def _read_auth_output(self, process, stream, is_stderr):
        if stream is None:
            return
        try:
            for raw_line in stream:
                line = raw_line.strip()
                if not line:
                    continue
                with self.lock:
                    if process is not self.auth_process:
                        return
                    if not is_stderr and self.auth_method == "qr":
                        payload = extract_qr_payload(line)
                        if payload:
                            self.qr_payload = payload
                            self.qr_expires_at = time.time() + QR_TTL_SECONDS
                            self.state = "WAITING_FOR_SCAN"
                            self.message = "Scan the QR code with WhatsApp"
                    elif self.auth_method == "phone":
                        pairing_code = extract_pairing_code(line)
                        if pairing_code:
                            self.pairing_code = pairing_code
                            self.state = "WAITING_FOR_PHONE_PAIRING"
                        if len(line) < 240:
                            self.message = line
        except Exception as exc:
            with self.lock:
                if process is self.auth_process:
                    self.last_error = str(exc)

    def _wait_for_auth(self, process):
        return_code = process.wait()
        with self.lock:
            if process is not self.auth_process:
                return
            self.auth_process = None
            self.auth_started_at = None
            if self.stopping:
                return
            if return_code == 0 and self._check_authenticated_locked():
                self.authenticated = True
                self.state = "AUTHENTICATED"
                self.message = "WhatsApp authentication completed"
                self.last_error = None
                self.qr_payload = None
                self.qr_expires_at = None
                self.pairing_code = None
                self.sync_restart_at = 0
                self._start_sync_locked()
            else:
                self.authenticated = False
                self.state = "FAILED"
                self.last_error = self.last_error or f"wacli auth exited with status {return_code}"
                self.message = "WhatsApp authentication failed"

    def _start_sync_locked(self):
        if (self.sync_process is not None or self.stopping or not self.sync_command
                or time.time() < self.sync_restart_at):
            return
        try:
            self.sync_process = subprocess.Popen(self.sync_command)
        except Exception as exc:
            self.sync_process = None
            self.state = "FAILED"
            self.last_error = str(exc)
            self.message = "Could not start wacli sync"
            self.sync_restart_at = time.time() + 2

    def _check_authenticated_locked(self):
        result = run_wacli_capture([
            WACLI_BIN,
            "--json",
            "--store",
            STORE_DIR,
            "auth",
            "status",
        ], timeout=10)
        if result["timedOut"] or result["exitCode"] != 0:
            return False
        body = parse_json_or_text(result["stdout"])
        return is_authenticated_response(body)

    def _terminate_process_locked(self, process):
        if process is None or process.poll() is not None:
            return
        try:
            process.terminate()
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=5)
        except Exception:
            pass


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.handle_health()
            return
        if self.path == "/auth/status":
            self.handle_auth_status()
            return
        if self.path == "/status":
            self.handle_status()
            return
        if self.path == "/identity":
            self.handle_identity()
            return
        if self.path.startswith("/media"):
            self.handle_media()
            return
        self.respond(404, {"status": "NOK", "message": "Not found"})

    def do_POST(self):
        if self.path == "/auth/start":
            self.handle_auth_start()
            return
        if self.path == "/auth/cancel":
            self.handle_auth_cancel()
            return
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
        if self.authentication_in_progress():
            self.respond(409, {"status": "NOK", "message": "WhatsApp authentication is in progress"})
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
        if reply_to:
            command.extend(["--reply-to", reply_to])
        if reply_to_sender:
            command.extend(["--reply-to-sender", reply_to_sender])
        self.run_wacli(command, to)

    def handle_presence(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        if self.authentication_in_progress():
            self.respond(409, {"status": "NOK", "message": "WhatsApp authentication is in progress"})
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
        runtime = self.runtime()
        if runtime is not None:
            snapshot = runtime.snapshot()
            if not snapshot["authenticated"]:
                self.respond(503, {"status": "NOK", "message": "not authenticated", "auth": snapshot})
                return
            self.respond(200, {"status": "OK", "authenticated": True, "auth": snapshot})
            return
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
        runtime = self.runtime()
        if runtime is not None and not runtime.doctor_allowed():
            self.respond(409, {"status": "NOK", "message": "WhatsApp authentication is in progress", "auth": runtime.snapshot()})
            return
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
            "auth": runtime.snapshot() if runtime is not None else None,
            "stdout": parse_json_or_text(result["stdout"]),
            "stderr": result["stderr"],
            "exitCode": result["exitCode"],
            "timedOut": result["timedOut"],
        })

    def handle_auth_status(self):
        if not self.internal_authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        runtime = self.runtime()
        if runtime is None:
            self.respond(503, {"status": "NOK", "message": "WhatsApp runtime is unavailable"})
            return
        self.respond(200, {"status": "OK", "auth": runtime.snapshot()})

    def handle_auth_start(self):
        if not self.internal_authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        payload = self.read_json_payload()
        if payload is None:
            return
        method = str(payload.get("method", "qr")).strip().lower()
        phone = str(payload.get("phone", "")).strip()
        runtime = self.runtime()
        if runtime is None:
            self.respond(503, {"status": "NOK", "message": "WhatsApp runtime is unavailable"})
            return
        started, error = runtime.start_auth(method, phone)
        if not started:
            self.respond(409 if "already in progress" in (error or "") else 400, {
                "status": "NOK",
                "message": error,
                "auth": runtime.snapshot(),
            })
            return
        self.respond(202, {"status": "OK", "auth": runtime.snapshot()})

    def handle_auth_cancel(self):
        if not self.internal_authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        runtime = self.runtime()
        if runtime is None:
            self.respond(503, {"status": "NOK", "message": "WhatsApp runtime is unavailable"})
            return
        cancelled, error = runtime.cancel_auth()
        if not cancelled:
            self.respond(409, {"status": "NOK", "message": error, "auth": runtime.snapshot()})
            return
        self.respond(200, {"status": "OK", "auth": runtime.snapshot()})

    def handle_identity(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        result = self.run_sqlite_capture(
            "SELECT jid, lid FROM whatsmeow_device LIMIT 1",
            timeout=10,
        )
        if result["timedOut"]:
            self.respond(504, {"status": "NOK", "message": "wacli identity lookup timed out"})
            return
        if result["exitCode"] != 0:
            self.respond(503, {
                "status": "NOK",
                "message": "wacli identity lookup failed",
                "stderr": result["stderr"],
            })
            return
        rows = [line.split("\t", 1) for line in result["stdout"].splitlines() if "\t" in line]
        if not rows:
            self.respond(503, {"status": "NOK", "message": "WhatsApp identity is not available"})
            return
        jid, lid = rows[0]
        self.respond(200, {"status": "OK", "jid": jid, "lid": lid})

    def handle_media(self):
        if not self.authorized():
            self.respond(401, {"status": "NOK", "message": "Unauthorized"})
            return
        if self.authentication_in_progress():
            self.respond(409, {"status": "NOK", "message": "WhatsApp authentication is in progress"})
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

    def internal_authorized(self):
        return bool(INTERNAL_TOKEN) and self.headers.get(INTERNAL_TOKEN_HEADER) == INTERNAL_TOKEN

    def runtime(self):
        return getattr(getattr(self, "server", None), "runtime", None)

    def authentication_in_progress(self):
        runtime = self.runtime()
        return runtime is not None and runtime.authentication_in_progress()

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
        return run_wacli_capture(command, timeout)

    def run_sqlite_capture(self, query, timeout):
        return self.run_wacli_capture([
            "sqlite3",
            "-separator",
            "\t",
            os.path.join(STORE_DIR, "session.db"),
            query,
        ], timeout)

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


def is_authenticated_response(body):
    return bool(
        isinstance(body, dict)
        and body.get("success") is True
        and isinstance(body.get("data"), dict)
        and body["data"].get("authenticated") is True
    )


def run_wacli_capture(command, timeout):
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


def normalize_phone(value):
    phone = str(value or "").strip()
    if not re.fullmatch(r"\+?[1-9][0-9]{7,14}", phone):
        return None
    return phone


def extract_qr_payload(line):
    candidate = line.strip()
    if candidate.lower().startswith("qr:"):
        candidate = candidate[3:].strip()
    if re.fullmatch(r"[0-9]+@[^\s]+", candidate) and len(candidate) >= 24:
        return candidate
    return None


def extract_pairing_code(line):
    match = re.search(r"\b[A-Z0-9]{4}(?:-[A-Z0-9]{4}){1,3}\b", line.upper())
    return match.group(0) if match else None


def first_query_value(query, name):
    values = query.get(name)
    if not values:
        return ""
    return str(values[0]).strip()


if __name__ == "__main__":
    sync_command = sys.argv[1:]
    runtime = RuntimeCoordinator(sync_command)
    runtime.start()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    server.runtime = runtime

    def stop(signum=None, frame=None):
        runtime.stop()
        threading.Thread(target=server.shutdown, daemon=True).start()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    print(f"bot-whatsapp wrapper listening on :{PORT}", flush=True)
    try:
        server.serve_forever()
    finally:
        runtime.stop()
