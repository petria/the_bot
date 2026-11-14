import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as usersApi from '../api/adminUsers';
import { renderPage } from '../test/pageTestUtils';
import { AdminUsersPage } from './AdminUsersPage';

describe('AdminUsersPage', () => {
function mockUsers() {
  vi.spyOn(usersApi, 'getAdminUsers').mockResolvedValue({
    users: [{
      id: 1, username: 'petria', name: 'Petri', email: 'petri@example.test', ircNick: '_Pete_',
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      chatIdentities: [], permissions: ['web.user'], reserved: false,
    }],
    availablePermissions: ['web.user', 'web.admin'],
    availableHomeChannels: [],
  });
}

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

  it('gives the user editor a full-width mobile action row', async () => {
    mockUsers();
    const user = userEvent.setup();
    renderPage(<AdminUsersPage />, ['/admin/users']);

    await user.click(await screen.findByRole('button', { name: /add user/i }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('button', { name: /^cancel$/i }).closest('.action-row')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /create user/i }).closest('.action-row')).toBeInTheDocument();
    expect(document.querySelector('.action-row .mantine-Button-root')).toBeInTheDocument();
  });

  it('gives the password reset dialog a full-width mobile action row', async () => {
    mockUsers();
    const user = userEvent.setup();
    renderPage(<AdminUsersPage />, ['/admin/users']);

    const triggers = await screen.findAllByRole('button', { name: 'Reset password' });
    await user.click(triggers[0]);
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('button', { name: /^cancel$/i }).closest('.action-row')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /reset password/i }).closest('.action-row')).toBeInTheDocument();
  });

  it('gives the delete confirmation a full-width mobile action row', async () => {
    mockUsers();
    const user = userEvent.setup();
    renderPage(<AdminUsersPage />, ['/admin/users']);

    const triggers = await screen.findAllByRole('button', { name: 'Delete user' });
    await user.click(triggers[0]);
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('button', { name: /^cancel$/i }).closest('.action-row')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: /^delete$/i }).closest('.action-row')).toBeInTheDocument();
  });
});
