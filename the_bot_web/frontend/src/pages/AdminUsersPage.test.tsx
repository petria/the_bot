import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as usersApi from '../api/adminUsers';
import { renderPage } from '../test/pageTestUtils';
import { AdminUsersPage } from './AdminUsersPage';

describe('AdminUsersPage', () => {
  it('lists users and opens the create-user form', async () => {
    vi.spyOn(usersApi, 'getAdminUsers').mockResolvedValue({
      users: [{
        id: 1, username: 'petria', name: 'Petri', email: 'petri@example.test', ircNick: '_Pete_',
        telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
        chatIdentities: [], permissions: ['web.user'], reserved: false,
      }],
      availablePermissions: ['web.user', 'web.admin'],
      availableHomeChannels: [],
    });
    const user = userEvent.setup();
    renderPage(<AdminUsersPage />, ['/admin/users']);

    expect(await screen.findByText('Manage Users')).toBeInTheDocument();
    expect((await screen.findAllByText('petria')).length).toBeGreaterThan(0);
    await user.click(screen.getByRole('button', { name: /add user/i }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
  });
});
