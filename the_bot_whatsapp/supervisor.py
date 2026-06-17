import os
import signal
import subprocess
import sys
import time


SERVER_COMMAND = [sys.executable, "/opt/bot-whatsapp/server.py"]


def main():
    sync_command = sys.argv[1:]
    if not sync_command:
        print("sync command is required", file=sys.stderr, flush=True)
        return 2

    children = []
    stopping = False

    def terminate(signum=None, frame=None):
        nonlocal stopping
        stopping = True
        for child in children:
            if child.poll() is None:
                child.terminate()

    signal.signal(signal.SIGTERM, terminate)
    signal.signal(signal.SIGINT, terminate)

    sync_process = subprocess.Popen(sync_command)
    children.append(sync_process)
    server_process = subprocess.Popen(SERVER_COMMAND)
    children.append(server_process)

    exit_code = 0
    exited_name = None
    while True:
        for name, child in (("wacli sync", sync_process), ("bot-whatsapp wrapper", server_process)):
            code = child.poll()
            if code is not None:
                exit_code = code
                exited_name = name
                break
        if exited_name is not None or stopping:
            break
        time.sleep(1)

    if exited_name is not None:
        print(f"{exited_name} exited with status {exit_code}; stopping sidecar", flush=True)
    terminate()
    deadline = time.time() + 10
    for child in children:
        while child.poll() is None and time.time() < deadline:
            time.sleep(0.2)
        if child.poll() is None:
            child.kill()
    for child in children:
        try:
            child.wait(timeout=5)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait()
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
