import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as commandsApi from '../api/commands';
import { renderPage } from '../test/pageTestUtils';
import { CommandsPage } from './CommandsPage';

describe('CommandsPage', () => {
  it('groups registered commands by provider', async () => {
    vi.spyOn(commandsApi, 'getCommands').mockResolvedValue({
      providers: [{
        namespace: 'main',
        displayName: 'Main commands',
        description: 'Core commands',
        commandCount: 1,
        invocationCount: 2,
        commands: [{
          commandName: 'ping',
          displayName: 'Ping',
          trigger: '!ping',
          className: 'PingCmd',
          requiredPermission: null,
          help: 'Check that the bot is alive.',
          invocationCount: 2,
          aliases: [],
        }],
      }],
    });

    renderPage(<CommandsPage />);

    expect(await screen.findByText('Main commands')).toBeInTheDocument();
    expect((await screen.findAllByText('!ping')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('Check that the bot is alive.')).length).toBeGreaterThan(0);
  });
});
