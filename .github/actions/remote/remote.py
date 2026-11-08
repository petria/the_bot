"""Small adapter for this repository's deployment inputs; no downloaded binaries."""
import io
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import tarfile
import tempfile


def script_bytes(script, names, environ):
    exports = []
    for name in names.split(','):
        name = name.strip()
        if not name:
            continue
        if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', name):
            raise ValueError('Invalid forwarded environment name')
        exports.append('export ' + name + '=' + shlex.quote(environ.get(name, '')))
    return ('\n'.join(exports) + '\n' + script + '\n').encode()


def file_archive(source, strip):
    path = Path(source)
    if path.is_absolute() or '..' in path.parts or not path.is_file() or path.is_symlink():
        raise ValueError('Source must be a regular file within the checkout')
    if strip < 0 or strip >= len(path.parts):
        raise ValueError('Invalid strip_components')
    archive = io.BytesIO()
    with tarfile.open(fileobj=archive, mode='w') as tar:
        tar.add(path, arcname=str(Path(*path.parts[strip:])), recursive=False)
    return archive.getvalue()


def timeout_seconds(value):
    match = re.fullmatch(r'([1-9][0-9]*)(s|m|h)?', value)
    if not match:
        raise ValueError('Invalid command timeout')
    return int(match[1]) * {'s': 1, 'm': 60, 'h': 3600, None: 1}[match[2]]


def main():
    env = os.environ
    host, user = env['REMOTE_HOST'], env['REMOTE_USER']
    if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_.:-]*', host):
        raise ValueError('Invalid SSH host')
    if not re.fullmatch(r'[A-Za-z0-9_][A-Za-z0-9_.-]*', user):
        raise ValueError('Invalid SSH user')
    if not env['REMOTE_KEY'].strip():
        raise ValueError('SSH key is required')
    timeout = timeout_seconds(env.get('REMOTE_TIMEOUT', '10m'))
    with tempfile.TemporaryDirectory(prefix='bot-deploy-ssh-') as directory:
        key = Path(directory, 'identity')
        key.write_text(env['REMOTE_KEY'].rstrip() + '\n')
        key.chmod(0o600)
        known = Path(directory, 'known_hosts')
        # Match existing first-contact deployment behavior without changing user SSH files.
        scan = subprocess.run(['ssh-keyscan', '-T', '15', '-H', host], capture_output=True, timeout=30, check=True)
        if not scan.stdout.strip():
            raise ValueError('SSH host key scan returned no keys')
        known.write_bytes(scan.stdout)
        ssh = ['ssh', '-i', str(key), '-o', 'IdentitiesOnly=yes', '-o', 'BatchMode=yes',
               '-o', 'ConnectTimeout=30', '-o', 'ServerAliveInterval=15', '-o', 'ServerAliveCountMax=4',
               '-o', 'StrictHostKeyChecking=yes', '-o', 'UserKnownHostsFile=' + str(known), '-l', user, host]
        if env.get('REMOTE_SOURCE'):
            if env.get('REMOTE_SCRIPT') or not env.get('REMOTE_TARGET'):
                raise ValueError('Copy requires a target and no script')
            if env.get('REMOTE_OVERWRITE', 'true') != 'true':
                raise ValueError('This action only supports overwrite=true')
            payload = file_archive(env['REMOTE_SOURCE'], int(env.get('REMOTE_STRIP', '0')))
            target = shlex.quote(env['REMOTE_TARGET'])
            command = 'mkdir -p -- ' + target + ' && tar -xf - -C ' + target
        else:
            if not env.get('REMOTE_SCRIPT', '').strip():
                raise ValueError('Remote script is required')
            payload = script_bytes(env['REMOTE_SCRIPT'], env.get('REMOTE_ENVS', ''), env)
            command = 'bash -se'
        # Script and forwarded secrets travel through stdin, never command-line arguments.
        subprocess.run(ssh + [command], input=payload, timeout=timeout, check=True)


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as exc:
        print('SSH deployment failed with exit status ' + str(exc.returncode), file=sys.stderr)
        sys.exit(1)
    except subprocess.TimeoutExpired:
        print('SSH deployment timed out', file=sys.stderr)
        sys.exit(1)
    except (ValueError, KeyError) as exc:
        print('Invalid SSH deployment configuration: ' + str(exc), file=sys.stderr)
        sys.exit(1)
