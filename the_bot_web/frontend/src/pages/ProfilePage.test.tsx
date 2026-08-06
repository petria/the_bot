import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as connectionsApi from '../api/connections';
import * as meApi from '../api/me';
import { renderPage } from '../test/pageTestUtils';
import { ProfilePage } from './ProfilePage';

describe('ProfilePage', () => {
  it('loads editable profile and password controls', async () => {
    vi.spyOn(meApi, 'getMe').mockResolvedValue({
      id: 1, username: 'petria', name: 'Petri', email: 'petri@example.test', ircNick: '_Pete_',
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      permissions: ['web.user'], roles: [],
    });
    vi.spyOn(meApi, 'getNotifyRules').mockResolvedValue([]);
    vi.spyOn(connectionsApi, 'getConnectionsOverview').mockResolvedValue({ connections: [], activities: [] });

    renderPage(<ProfilePage />, ['/profile']);

    expect(await screen.findByText('Profile')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Petri')).toBeInTheDocument();
    expect(screen.getByText('Change Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create irc claim token/i })).toBeInTheDocument();
  });
});
