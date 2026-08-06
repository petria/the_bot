import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as connectionsApi from '../api/connections';
import * as knownUsersApi from '../api/knownUsers';
import { renderPage } from '../test/pageTestUtils';
import { SendPage } from './SendPage';

describe('SendPage', () => {
  it('renders the user, channel, and IRC private send modes', async () => {
    vi.spyOn(knownUsersApi, 'getKnownUserTargets').mockResolvedValue([]);
    vi.spyOn(connectionsApi, 'getConnectionsOverview').mockResolvedValue({
      connections: [{
        id: 1, type: 'IRC_CONNECTION', network: 'IRCNet', channels: [{
          id: '1', type: 'IrcPublic', network: 'IRCNet', name: '#test', echoToAlias: 'IRC-TEST',
        }],
      }], activities: [],
    });

    renderPage(<SendPage />, ['/send']);

    expect(screen.getByText('Send Message')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'To User' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'To Channel' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'IRC Private' })).toBeInTheDocument();
  });
});
