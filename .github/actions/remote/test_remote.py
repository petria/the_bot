import io
import json
import os
from pathlib import Path
import subprocess
import tarfile
import tempfile
import unittest
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

    def test_timeout_and_environment_name_validation(self):
        self.assertEqual(remote.timeout_seconds('30m'), 1800)
        self.assertEqual(remote.timeout_seconds('10m'), 600)
        with self.assertRaises(ValueError):
            remote.timeout_seconds('0')
        with self.assertRaises(ValueError):
            remote.script_bytes('true', 'FOO;false', {})


if __name__ == '__main__':
    unittest.main()
