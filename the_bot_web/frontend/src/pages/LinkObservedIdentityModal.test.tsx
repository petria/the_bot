import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { AdminUser } from '../api/adminUsers';
import type { KnownUserTarget } from '../api/knownUsers';
import { renderPage } from '../test/pageTestUtils';
import { LinkObservedIdentityModal } from './LinkObservedIdentityModal';

const target: KnownUserTarget = {
  logicalUserKey: 'discord:Petri', configuredUserId: null, configuredUsername: null, configuredName: null,
  matchedConfiguredUser: false, matchSource: null, observedUserKey: 'discord-user', observedUserId: '123456789',
  observedUsername: 'petria', observedDisplayName: 'Petri Discord', connectionId: 2,
  connectionType: 'DISCORD_CONNECTION', network: 'DiscordNetwork', channelId: '2', channelName: '#test',
  echoToAlias: 'DISCORD-TEST', targetType: 'PUBLIC', lastSeenAt: null, lastSeenSource: 'DISCORD',
};

const user: AdminUser = {
  id: 1, username: 'petria', name: 'Petri', email: null, ircNick: null,
  telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
  chatIdentities: [], permissions: ['web.user'], reserved: false,
};

describe('LinkObservedIdentityModal', () => {
  it('shows the observed identity and selected registered user', () => {
    renderPage(<LinkObservedIdentityModal opened target={target} user={user} onClose={() => undefined} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/Petri Discord/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /link/i })).toBeInTheDocument();
  });
});
