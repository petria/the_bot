import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as connectionsApi from '../api/connections';
import * as meApi from '../api/me';
import { renderPage } from '../test/pageTestUtils';
import { ConnectionsPage } from './ConnectionsPage';

describe('ConnectionsPage', () => {
  it('renders configured connections and channels', async () => {
    vi.spyOn(meApi, 'getMe').mockResolvedValue({
      id: 1, username: 'petria', name: 'Petri', email: null, ircNick: null,
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      permissions: ['web.user'], roles: [],
    });
    vi.spyOn(connectionsApi, 'getConnectionsOverview').mockResolvedValue({
      connections: [{
        id: 1,
        type: 'IRC_CONNECTION',
        network: 'IRCNet',
        channels: [{
          id: '1', type: 'IrcPublic', network: 'IRCNet', name: '#test',
          echoToAlias: 'IRC-TEST', configured: true, observedOnly: false,
        }],
      }],
      activities: [],
    });

    renderPage(<ConnectionsPage />);

    expect(await screen.findByText('IRC_CONNECTION')).toBeInTheDocument();
    expect(await screen.findAllByText('#test')).not.toHaveLength(0);
    expect(await screen.findAllByText('IRC-TEST')).not.toHaveLength(0);
  });
});
