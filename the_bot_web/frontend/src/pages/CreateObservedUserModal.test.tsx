import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { KnownUserTarget } from '../api/knownUsers';
import { renderPage } from '../test/pageTestUtils';
import { CreateObservedUserModal } from './CreateObservedUserModal';

const target: KnownUserTarget = {
  logicalUserKey: 'irc:Petri', configuredUserId: null, configuredUsername: null, configuredName: null,
  matchedConfiguredUser: false, matchSource: null, observedUserKey: 'petria', observedUserId: 'petria',
  observedUsername: 'petria', observedDisplayName: 'Petri', connectionId: 1,
  connectionType: 'IRC_CONNECTION', network: 'IRCNet', channelId: '1', channelName: '#test',
  echoToAlias: 'IRC-TEST', targetType: 'PUBLIC', lastSeenAt: null, lastSeenSource: 'IRC',
};

describe('CreateObservedUserModal', () => {
  it('prefills a new user form from an observed identity', () => {
    renderPage(<CreateObservedUserModal opened target={target} onClose={() => undefined} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByDisplayValue('petria')).toBeInTheDocument();
    expect(screen.getByText('Petri')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /generate/i })).toBeInTheDocument();
  });

  it('stacks the password field and modal actions for phone use', () => {
    renderPage(<CreateObservedUserModal opened target={target} onClose={() => undefined} />);

    const generate = screen.getByRole('button', { name: /generate/i });
    const fieldRow = generate.closest('.field-row');
    expect(fieldRow).not.toBeNull();
    expect(generate.closest('.stack-fixed')).toBeInTheDocument();
    expect(fieldRow?.querySelector('.mantine-PasswordInput-root')).toBeInTheDocument();

    const dialog = screen.getByRole('dialog');
    for (const label of [/^cancel$/i, /create user/i]) {
      expect(within(dialog).getByRole('button', { name: label }).closest('.action-row')).toBeInTheDocument();
    }
    expect(document.querySelector('.action-row .mantine-Button-root')).toBeInTheDocument();
  });

});
