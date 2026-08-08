import { fireEvent, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as configApi from '../api/adminConnectionConfig';
import { renderPage } from '../test/pageTestUtils';
import { AdminConnectionConfigPage } from './AdminConnectionConfigPage';

describe('AdminConnectionConfigPage', () => {
  it('loads the IRC connection configuration and channel switches', async () => {
    vi.spyOn(configApi, 'getAdminConnectionConfig').mockResolvedValue({
      profile: 'DEV', configFile: '/runtime/DEV.json', lastModifiedAt: '2026-08-06T10:00:00Z',
      config: {
        botConfig: { botName: 'Hokan', ircRealName: 'the_bot' },
        ircServerConfigs: [{
          name: 'Test IRC', connectStartup: true, networkName: 'IRCNet', host: 'irc.test', port: 6667,
          channelList: [{
            id: 'test', description: 'Test', name: '#test', type: 'IrcPublic', echoToAlias: 'IRC-TEST',
            echoToAliases: [], echoIrcActivity: false, joinOnStart: true, publicAiEnabled: true,
            allowAnonymousAiCommands: false, resolveUrls: false, alertMessages: false,
            captureResolvedUrls: false, captureImages: false, captureImageToAliases: [], manageOperators: false,
            manageTopic: true, topic: 'Guarded topic', manageMode: true, modes: '+st',
          }],
        }],
        discordConfig: null, telegramConfig: null, whatsappConfig: null,
      },
      topicStates: [{ echoToAlias: 'IRC-TEST', channelName: '#test', manageTopic: true,
        configuredTopic: 'Guarded topic', currentTopic: 'Guarded topic', connected: true, joined: true, mismatch: false }],
      modeStates: [{ echoToAlias: 'IRC-TEST', channelName: '#test', manageMode: true,
        configuredModes: '+st', currentModes: '+st', connected: true, joined: true, mismatch: false }],
    });

    renderPage(<AdminConnectionConfigPage />, ['/admin/config']);

    expect(await screen.findByText('Manage Connections')).toBeInTheDocument();
    expect(screen.getByDisplayValue('IRC-TEST')).toBeInTheDocument();
    expect(screen.getByLabelText('Public AI')).toBeChecked();
    expect(screen.getByRole('switch', { name: /Manage topic/ })).toBeChecked();
    expect(screen.getByDisplayValue('Guarded topic')).toBeInTheDocument();
    expect(screen.getByText(/Current IRC topic: Guarded topic/)).toBeInTheDocument();
    expect(screen.getByRole('switch', { name: /Manage channel modes/ })).toBeChecked();
    expect(screen.getByDisplayValue('+st')).toBeInTheDocument();
    expect(screen.getByText(/Current IRC modes: \+st/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'Discord' }));
    expect(screen.queryByText('Manage topic')).not.toBeInTheDocument();
  });
});
