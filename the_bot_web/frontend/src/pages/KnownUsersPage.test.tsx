import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as knownUsersApi from '../api/knownUsers';
import * as meApi from '../api/me';
import { renderPage } from '../test/pageTestUtils';
import { KnownUsersPage } from './KnownUsersPage';

describe('KnownUsersPage', () => {
  it('renders observed users and admin actions only for web admins', async () => {
    vi.spyOn(meApi, 'getMe').mockResolvedValue({
      id: 1, username: 'petria', name: 'Petri', email: null, ircNick: null,
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      permissions: ['web.user', 'web.admin'], roles: [],
    });
    vi.spyOn(knownUsersApi, 'getKnownUserTargets').mockResolvedValue([{
      logicalUserKey: 'irc:IRCNet:petria', configuredUserId: null,
      configuredUsername: null, configuredName: null, matchedConfiguredUser: false,
      matchSource: null, observedUserKey: 'petria', observedUserId: 'petria',
      observedUsername: 'petria', observedDisplayName: 'Petri', connectionId: 1,
      connectionType: 'IRC_CONNECTION', network: 'IRCNet', channelId: '1',
      channelName: '#test', echoToAlias: 'IRC-TEST', targetType: 'PUBLIC',
      lastSeenAt: Date.now(), lastSeenSource: 'IRC',
    }]);

    renderPage(<KnownUsersPage />);

    expect(await screen.findByText('Known Users')).toBeInTheDocument();
    expect((await screen.findAllByText('Petri')).length).toBeGreaterThan(0);
    expect((screen.getAllByRole('button', { name: /link/i })).length).toBeGreaterThan(0);
  });
});
