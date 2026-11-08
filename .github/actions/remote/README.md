# Native SSH deployment action

Used by the VM deployment job for remote scripts and file copies. Requires the
runner's Python 3 and OpenSSH and the VM's Bash and tar; it downloads no tools.
This replaces appleboy ssh/scp actions whose runtime GitHub Release download
repeatedly returned HTTP 504 before deployment could connect to the VM.

Remote scripts receive only the environment names listed in `envs`. Values are
shell-quoted and sent over stdin, so multiline secrets and shell characters remain
literal. Scripts run with `bash -se`; a failed command fails the step. The existing
30-minute deployment timeout remains supported. Connection timeout is 30 seconds.

Each step creates a private temporary key file and removes it on exit.
One Ed25519 host-key scan is shared through RUNNER_TEMP for the job, which the
runner cleans up afterward. Reusing the scan avoids sshd's unauthenticated-connection
penalties. This retains the previous action's first-contact trust model. The action does not modify the runner's user SSH files.

Copy mode accepts one regular checkout file, `target` and optional
`strip_components`. It sends a tar stream over SSH, preserves source permissions
and replaces the destination file. All current workflow copies use overwrite=true.

Run checks with:

```sh
python3 -m unittest discover -s .github/actions/remote -v
```
