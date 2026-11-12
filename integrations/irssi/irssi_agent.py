#!/usr/bin/env python3
"""Outbound-only HTTPS queue helper. Irssi communicates over private stdin/stdout pipes."""
import argparse
from datetime import datetime
import fcntl
import getpass
import json
import os
from pathlib import Path
import select
import socket
import sys
import time
import urllib.request

DEFAULT_CONFIG = Path.home() / '.config/the-bot/irssi-agent.json'

def save_private(path, data):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    temporary = path.with_suffix(path.suffix + '.tmp')
    fd = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, 'w') as stream:
        os.fchmod(stream.fileno(), 0o600)
        json.dump(data, stream)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, path)

def api(config, path, body):
    headers = {'Content-Type': 'application/json'}
    if config.get('token'):
        headers['Authorization'] = 'Bearer ' + config['token']
    request = urllib.request.Request(config['url'].rstrip('/') + path,
                                     data=json.dumps(body).encode(), headers=headers, method='POST')
    # Do not follow redirects carrying a credential to a different server.
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    with urllib.request.build_opener(NoRedirect).open(request, timeout=10) as response:
        return json.load(response)

def fold(value):
    return value.lower()

def validate_job(job, connections, expected_nick):
    deadline = datetime.fromisoformat(job['expiresAt']).timestamp()
    if deadline <= time.time():
        raise ValueError('Send request expired')
    message = job.get('message', '')
    channel = job.get('channel', '')
    if (not isinstance(message, str) or not message or len(message) > 570
            or any(ord(c) < 32 or 127 <= ord(c) <= 159 for c in message)):
        raise ValueError('Invalid message text')
    if (not channel or channel[0] not in '#&' or any(c.isspace() or c in ',:' or ord(c) < 32 for c in channel)):
        raise ValueError('Invalid channel')
    if not expected_nick or fold(job.get('expectedNick', '')) != fold(expected_nick):
        raise ValueError('Expected nickname mismatch')
    if not any(fold(c.get('network', '')) == fold(job.get('network', ''))
               and fold(c.get('nick', '')) == fold(expected_nick)
               and any(fold(ch) == fold(channel) for ch in c.get('channels', [])) for c in connections):
        raise ValueError('IRC connection, nickname or channel is unavailable')
    return deadline

def prepare_dispatch(journal_path, journal, job_id):
    if job_id in journal:
        return False
    journal[job_id] = {'status': 'UNCERTAIN', 'detail': 'Dispatch interrupted', 'acked': False}
    save_private(journal_path, journal)
    return True

def emit(data):
    print(json.dumps(data, ensure_ascii=True), flush=True)

class JsonPipeReader:
    def __init__(self, fd):
        self.fd = fd
        self.buffer = b''

    def receive(self, timeout):
        deadline = time.monotonic() + timeout
        while b'\n' not in self.buffer:
            remaining = deadline - time.monotonic()
            if remaining <= 0 or not select.select([self.fd], [], [], remaining)[0]:
                raise TimeoutError('Irssi did not respond')
            chunk = os.read(self.fd, 65536)
            if not chunk:
                raise EOFError('Irssi pipe closed')
            self.buffer += chunk
            if len(self.buffer) > 1024 * 1024:
                self.buffer = b''
                raise ValueError('Irssi protocol size limit')
        line, self.buffer = self.buffer.split(b'\n', 1)
        return json.loads(line)


def receive_expected(reader, kind, journal_path, journal, timeout, job_id=None):
    deadline = time.monotonic() + timeout
    while True:
        message = reader.receive(max(0, deadline-time.monotonic()))
        if message.get('type') == 'result' and message.get('id') in journal:
            status = message.get('status')
            if status in ('SUBMITTED', 'FAILED', 'UNCERTAIN'):
                journal[message['id']] = {'status': status, 'detail': message.get('detail'), 'acked': False}
                save_private(journal_path, journal)
        if message.get('type') == kind and (job_id is None or message.get('id') == job_id):
            return message
        if time.monotonic() >= deadline:
            raise TimeoutError('Expected Irssi response missing')


def run(config_file):
    if config_file.stat().st_mode & 0o077:
        raise ValueError('Credential file must have mode 600')
    config = json.loads(config_file.read_text())
    if not config['url'].startswith('https://'):
        raise ValueError('HTTPS is required')
    lock = open(str(config_file) + '.lock', 'a')
    os.chmod(lock.name, 0o600)
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    journal_path = config_file.with_name('irssi-send-journal.json')
    journal = json.loads(journal_path.read_text()) if journal_path.exists() else {}
    reader = JsonPipeReader(sys.stdin.fileno())
    last_error = None
    while True:
        try:
            emit({'type': 'snapshot'})
            snapshot = receive_expected(reader, 'snapshot', journal_path, journal, 10)
            if snapshot.get('type') != 'snapshot':
                continue
            connections = snapshot.get('connections', [])
            # Retry result acknowledgments only; never retry an IRC send.
            for job_id, record in list(journal.items()):
                if not record.get('acked'):
                    api(config, '/api/irssi/jobs/' + job_id + '/result',
                        {'status': record['status'], 'detail': record.get('detail')})
                    record['acked'] = True
                    save_private(journal_path, journal)
            response = api(config, '/api/irssi/poll', {'connections': connections})
            if last_error is not None:
                emit({'type': 'status', 'message': 'Connected to bot instance'})
                last_error = None
            job = response.get('job')
            if job:
                job_id = job['id']
                if job_id in journal:
                    old = journal[job_id]
                    api(config, '/api/irssi/jobs/' + job_id + '/result', {'status': old['status'], 'detail': old.get('detail')})
                    continue
                try:
                    deadline = validate_job(job, connections, config['expectedNick'])
                except ValueError as error:
                    journal[job_id] = {'status': 'FAILED', 'detail': str(error), 'acked': False}
                    save_private(journal_path, journal)
                    continue
                # Persist before handing text to Irssi. Crash recovery reports uncertainty.
                if not prepare_dispatch(journal_path, journal, job_id):
                    continue
                emit({'type': 'send', 'job': dict(job, deadlineEpoch=deadline)})
                result = receive_expected(reader, 'result', journal_path, journal, 15, job_id)
                if result.get('type') == 'result' and result.get('id') == job_id:
                    status = result.get('status')
                    if status in ('SUBMITTED', 'FAILED', 'UNCERTAIN'):
                        journal[job_id] = {'status': status, 'detail': result.get('detail'), 'acked': False}
                        save_private(journal_path, journal)
            time.sleep(2)
        except EOFError:
            return
        except Exception as error:
            # Never print credentials, server response bodies, or message contents.
            kind = type(error).__name__
            if kind != last_error:
                emit({'type': 'status', 'message': 'Agent waiting: ' + kind})
                last_error = kind
            time.sleep(3)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['pair', 'run'])
    parser.add_argument('--config', type=Path, default=DEFAULT_CONFIG)
    parser.add_argument('--url', default='https://hokanthebot.airiot.fi')
    parser.add_argument('--name', default=socket.gethostname())
    args = parser.parse_args()
    os.umask(0o077)
    if args.action == 'pair':
        if not args.url.startswith('https://'):
            parser.error('HTTPS is required')
        code = getpass.getpass('Pairing code from Android: ').strip()
        credentials = api({'url': args.url}, '/api/irssi/pair', {'code': code, 'name': args.name})
        save_private(args.config, dict(credentials, url=args.url))
        print('Paired. Load bot_notify.pl in Irssi. Expected nick: ' + credentials['expectedNick'])
    else:
        run(args.config)

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print('Irssi agent failed: ' + type(error).__name__, file=sys.stderr)
        sys.exit(1)
