import { fireEvent, screen, waitFor } from '@testing-library/react';
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
    const createRule = vi.spyOn(meApi, 'createNotifyRule').mockImplementation(async (input) => ({ ...input, id: 'rule-1', username: 'petria', createdAt: 1, updatedAt: 1 }));
    vi.spyOn(meApi, 'getNotifyRules').mockResolvedValue([]);
    vi.spyOn(connectionsApi, 'getConnectionsOverview').mockResolvedValue({ connections: [], activities: [] });

    renderPage(<ProfilePage />, ['/profile']);

    expect(await screen.findByText('Profile')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Petri')).toBeInTheDocument();
    const autoReply = screen.getByRole('switch', { name: /enable auto reply/i });
    expect(autoReply).not.toBeChecked();
    fireEvent.click(autoReply);
    expect(autoReply).toBeChecked();
    fireEvent.change(screen.getByLabelText('Source echo alias'), { target: { value: 'IRC-TEST' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add pattern' }));
    await waitFor(() => expect(createRule).toHaveBeenCalledWith(expect.objectContaining({ autoReplyEnabled: true })));
    await waitFor(() => expect(screen.getByRole('switch', { name: /enable auto reply/i })).not.toBeChecked());
    expect(screen.getByText('Change Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create irc claim token/i })).toBeInTheDocument();
  });
});
