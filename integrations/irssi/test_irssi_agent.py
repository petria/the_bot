import importlib.util
from datetime import datetime, timezone, timedelta
import os
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('irssi_agent', Path(__file__).with_name('irssi_agent.py'))
agent = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent)

class AgentTests(unittest.TestCase):
    def setUp(self):
        self.job = dict(id='id', expiresAt=(datetime.now(timezone.utc)+timedelta(seconds=60)).isoformat(), network='IRCNet', channel='#target', expectedNick='_Pete_', message='Alice: Thanks')
        self.connections = [dict(network='IRCnet', nick='_Pete_', channels=['#target'])]
    def test_valid_send_and_literal_slash_are_text(self):
        agent.validate_job(self.job, self.connections, '_Pete_')
        self.job['message'] = 'Alice: /quit'
        agent.validate_job(self.job, self.connections, '_Pete_')
    def test_wrong_network_nick_channel_and_controls_cannot_send(self):
        for key, value in [('network','other'),('expectedNick','other'),('channel','#other'),('message','x\n/quit'),('message','x\x01'),('expiresAt','2020-01-01T00:00:00+00:00')]:
            job = dict(self.job, **{key: value})
            with self.assertRaises(ValueError):
                agent.validate_job(job, self.connections, '_Pete_')
        with self.assertRaises(ValueError):
            agent.validate_job(self.job, [], '_Pete_')
    def test_private_journal_and_credentials_are_atomic_and_restricted(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'credentials.json'
            agent.save_private(path, {'token':'synthetic-test-token'})
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
            agent.save_private(path, {'job':'UNCERTAIN'})
            self.assertIn('UNCERTAIN',path.read_text())
            self.assertFalse(path.with_suffix('.json.tmp').exists())
    def test_crash_journal_prevents_duplicate_dispatch_after_restart(self):
        import json
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'journal.json'
            journal={}
            self.assertTrue(agent.prepare_dispatch(path,journal,'job-id'))
            restored=json.loads(path.read_text())
            self.assertEqual(restored['job-id']['status'],'UNCERTAIN')
            self.assertFalse(agent.prepare_dispatch(path,restored,'job-id'))
    def test_pipe_framing_preserves_multiple_buffered_responses(self):
        read, write=os.pipe()
        try:
            reader=agent.JsonPipeReader(read)
            os.write(write,b'{"type":"snapshot"}\n{"type":"result"}\n')
            self.assertEqual(reader.receive(.1)['type'],'snapshot')
            self.assertEqual(reader.receive(.1)['type'],'result')
        finally:
            os.close(read);os.close(write)
    def test_late_result_is_recorded_while_waiting_for_snapshot(self):
        read,write=os.pipe()
        try:
            with tempfile.TemporaryDirectory() as directory:
                journal={'id':{'status':'UNCERTAIN','acked':False}}
                path=Path(directory)/'journal.json'
                os.write(write,b'{"type":"result","id":"id","status":"SUBMITTED"}\n{"type":"snapshot","connections":[]}\n')
                reply=agent.receive_expected(agent.JsonPipeReader(read),'snapshot',path,journal,.1)
                self.assertEqual(reply['type'],'snapshot')
                self.assertEqual(journal['id']['status'],'SUBMITTED')
        finally:
            os.close(read);os.close(write)
    def test_irc_case_mapping(self):
        self.assertEqual(agent.fold('IRCNet'),agent.fold('IRCnet'))
        self.assertNotEqual(agent.fold('[Nick]'),agent.fold('{nick}'))  # Do not guess a server's CASEMAPPING

if __name__ == '__main__':
    unittest.main()
