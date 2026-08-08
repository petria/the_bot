import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as meApi from '../api/me';
import { renderPage } from '../test/pageTestUtils';
import { DashboardPage } from './DashboardPage';

describe('DashboardPage', () => {
  it('renders the logged-in user overview', async () => {
    vi.spyOn(meApi, 'getMe').mockResolvedValue({
      id: 1,
      username: 'petria',
      name: 'Petri',
      email: 'petri@example.test',
      ircNick: '_Pete_',
      telegramId: null,
      discordId: null,
      whatsappId: null,
      homeChannel: null,
      permissions: ['web.user'],
      roles: [],
    });

    renderPage(<DashboardPage />);

    expect(await screen.findByText(/Signed in as petria/)).toBeInTheDocument();
    expect(screen.getByText('Petri')).toBeInTheDocument();
  });
});
