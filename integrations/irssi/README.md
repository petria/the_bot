# Approved Android replies through Irssi

The Android app reviews the draft and submits only the edited text. Main keeps
its original network, channel, sender and expected nick. A user-paired helper
on the Irssi host polls main over HTTPS. The Irssi script submits the message
through the already-connected session, as its current nick. It never runs the
message as an Irssi command.

## Install on the Irssi host

For this installation the host is `petria@new.airiot.fi`, the expected IRC nick
is `_Pete_`, and the bot account is `petria`. Python 3, Perl, Irssi Perl support,
and core `JSON::PP` are required. There is no inbound listener or open port.

Install `irssi_agent.py` to `~/.local/lib/the-bot/irssi_agent.py` and
`bot_notify.pl` to `~/.irssi/scripts/bot_notify.pl`. For automatic loading:

```sh
mkdir -p ~/.irssi/scripts/autorun
ln -s ../bot_notify.pl ~/.irssi/scripts/autorun/bot_notify.pl
```

After the feature has been accepted and deployed to **main**, install the
matching Android update. In the phone app open **Irssi → Create pairing code**.
Then on the Irssi host run:

```sh
python3 -I ~/.local/lib/the-bot/irssi_agent.py pair --url https://hokanthebot.airiot.fi
```

Paste the pairing code at the prompt. The code is single-use and expires after
ten minutes. The helper saves its agent-scoped credential under
`~/.config/the-bot/irssi-agent.json` with mode 600. It never needs your bot
password or an SSH credential. Pairing is bound to your profile IRC nick.

In the existing Irssi session:

```text
/script load bot_notify
```

After replacing a helper or pairing again, use `/bot_notify_restart`. The
optional Irssi settings `bot_notify_helper` and `bot_notify_config` select
alternate absolute paths. Check that the Irssi server's chatnet name matches
the bot's network name (case-insensitively), and that `_Pete_` is joined to the
source channel. No default/current window is used for routing.

## Use and status

New IRC notification suggestions contain structured draft data. Open a
notification, tap **Review reply**, edit it, and choose **Send via Irssi**. The
channel and original sender are shown before approval. Messages are addressed
as `sender: reply`; an existing matching sender prefix is not duplicated.
Only one line of at most 500 characters is accepted. Long UTF-8 messages are
split into safe IRC chunks while preserving their text.

The phone requires a matching agent heartbeat within 45 seconds. An approved
request expires if not claimed within 60 seconds. The script rechecks the
actual connection, nick and joined channel before sending. Repeated taps use
the same request ID. A durable local journal is written before submission;
crashes and ambiguous delivery are reported as **UNCERTAIN**, never silently
resent. Check the channel before approving **Retry (may duplicate)**.
**SUBMITTED** means Irssi submitted the text to its IRC socket, not that the
IRC server or another user acknowledged receipt.

The phone's Irssi screen lists linked agents and can revoke them. Revocation
prevents further polling. Notifications without structured draft data remain
readable and do not offer sending. Existing plain-text suggestions are not
parsed into executable jobs.

## API and storage

Mobile account authorization protects `/api/mobile/irssi/**`: pairing codes,
agent status/revocation, per-draft availability, send approval and job status.
Agent credentials authenticate only `/api/irssi/poll` and job-result POSTs;
`/api/irssi/pair` requires the single-use code. Main stores hashed agent tokens
and durable job state in `irssi-replies.json` beside its mobile notifications
file. Pairing codes and live connection presence are ephemeral. A service
restart marks claimed sends uncertain and requires new heartbeats.

The helper acknowledges results after recording them locally. Network recovery
retries result acknowledgments, not messages. Both the helper and script check
routing; the script uses `send_message`, not `command` or a shell.

## Checks

```sh
python3 -I integrations/irssi/test_irssi_agent.py
perl integrations/irssi/test_bot_notify.pl
```

These tests use fake IRC objects and never send chat messages. The backend
suite checks pairing, ownership, immutable destinations, idempotency, stale
presence, expiry, restart and revocation. Android unit tests check approval,
retry states and text validation. Final live verification requires a phone
approval of one controlled suggestion.
