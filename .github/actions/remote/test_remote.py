import io
import fcntl
import json
import os
from pathlib import Path
import subprocess
import tarfile
import tempfile
import unittest
from unittest.mock import patch
import remote


class RemoteTests(unittest.TestCase):
    def test_forwarding_preserves_quotes_newlines_dollars_and_empty_values(self):
        values = {'VALUE': "quote'\n$(exit 99); `exit 98` $HOME", 'EMPTY': ''}
        script = remote.script_bytes("python3 -c 'import os,json; print(json.dumps([os.environ[\"VALUE\"],os.environ[\"EMPTY\"]]))'", 'VALUE,EMPTY', values)
        result = subprocess.run(['bash', '-se'], input=script, capture_output=True, check=True)
        self.assertEqual(json.loads(result.stdout), [values['VALUE'], ''])

    def test_remote_script_stops_on_failure(self):
        script = remote.script_bytes('false\necho should-not-run', '', {})
        result = subprocess.run(['bash', '-se'], input=script, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(result.stdout, b'')

    def test_deployment_file_layouts_and_permissions(self):
        old = os.getcwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                Path('scripts').mkdir()
                Path('scripts/docker-maintenance.sh').write_text('example')
                Path('scripts/docker-maintenance.sh').chmod(0o755)
                Path('docker-compose.yml').write_text('services: {}')
                for source, strip, name in [('docker-compose.yml', 0, 'docker-compose.yml'),
                    ('scripts/docker-maintenance.sh', 1, 'docker-maintenance.sh')]:
                    with tarfile.open(fileobj=io.BytesIO(remote.file_archive(source, strip))) as archive:
                        self.assertEqual(archive.getnames(), [name])
                        self.assertEqual(archive.extractfile(name).read(), Path(source).read_bytes())
                        self.assertEqual(archive.getmember(name).mode, Path(source).stat().st_mode & 0o777)
                with self.assertRaises(ValueError):
                    remote.file_archive('../outside', 0)
                with self.assertRaises(ValueError):
                    remote.file_archive('docker-compose.yml', 1)
            finally:
                os.chdir(old)

    def test_host_scan_is_reused_across_deployment_steps(self):
        with tempfile.TemporaryDirectory() as directory:
            env = {'REMOTE_HOST': 'docker.local', 'REMOTE_USER': 'hokan', 'REMOTE_KEY': 'test key',
                   'REMOTE_SCRIPT': 'true', 'RUNNER_TEMP': directory}
            with patch.dict(os.environ, env, clear=True), patch.object(remote.subprocess, 'run') as run:
                run.return_value = subprocess.CompletedProcess([], 0, stdout=b'host ssh-ed25519 test-key\n')
                for _ in range(5):
                    remote.main()
                scans = [call for call in run.call_args_list if call.args[0][0] == 'ssh-keyscan']
                self.assertEqual(len(scans), 1)
                self.assertIn('ed25519', scans[0].args[0])
                self.assertEqual(sum(call.args[0][0] == 'ssh' for call in run.call_args_list), 5)

    def test_maintenance_uses_existing_readonly_lock_without_truncation(self):
        script = Path(__file__).resolve().parents[3] / 'scripts/docker-maintenance.sh'
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            docker = root / 'docker'
            docker.write_text('#!/bin/sh\nif [ "$1" = info ]; then echo /tmp; fi\n')
            docker.chmod(0o755)
            lock = root / 'maintenance.lock'
            lock.write_text('existing lock inode')
            lock.chmod(0o444)
            inode = lock.stat().st_ino
            env = dict(os.environ, PATH=str(root) + ':' + os.environ['PATH'],
                       DOCKER_MAINTENANCE_LOCK_FILE=str(lock))
            result = subprocess.run(['bash', str(script), '--report'], env=env, capture_output=True, check=True)
            self.assertIn(b'Docker root:', result.stdout)
            self.assertEqual(lock.stat().st_ino, inode)
            self.assertEqual(lock.read_text(), 'existing lock inode')
            with lock.open('rb') as held:
                fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
                result = subprocess.run(['bash', str(script), '--report'], env=env, capture_output=True, check=True)
                self.assertIn(b'already running', result.stdout)
                self.assertNotIn(b'Docker root:', result.stdout)

    def test_timeout_and_environment_name_validation(self):
        self.assertEqual(remote.timeout_seconds('30m'), 1800)
        self.assertEqual(remote.timeout_seconds('10m'), 600)
        with self.assertRaises(ValueError):
            remote.timeout_seconds('0')
        with self.assertRaises(ValueError):
            remote.script_bytes('true', 'FOO;false', {})


if __name__ == '__main__':
    unittest.main()
