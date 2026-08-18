import { fireEvent, screen, waitFor } from '@testing-library/react';
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

  it('shows WhatsApp authentication controls and QR link', async () => {
    vi.spyOn(configApi, 'getAdminConnectionConfig').mockResolvedValue({
      profile: 'DEV', configFile: '/runtime/DEV.json', lastModifiedAt: '2026-08-06T10:00:00Z',
      config: {
        botConfig: null, ircServerConfigs: [], discordConfig: null, telegramConfig: null,
        whatsappConfig: { network: 'WhatsApp', sendBaseUrl: 'http://bot-whatsapp:8095', connectStartup: true, channelList: [] },
      },
      topicStates: [], modeStates: [],
    });
    vi.spyOn(configApi, 'getAdminWhatsAppAuthStatus').mockResolvedValue({
      state: 'WAITING_FOR_SCAN', authenticated: false, syncRunning: false, authRunning: true,
      method: 'qr', qrUrl: 'https://example.test/media/qr?token=secret',
      linkExpiresAt: '2026-08-17T20:45:00Z', pairingCode: null, message: 'Scan the QR code', error: null,
    });
    const start = vi.spyOn(configApi, 'startAdminWhatsAppAuth').mockResolvedValue({
      state: 'STARTING', authenticated: false, syncRunning: false, authRunning: true,
      method: 'qr', qrUrl: null, linkExpiresAt: null, pairingCode: null,
      message: 'Starting WhatsApp authentication', error: null,
    });

    renderPage(<AdminConnectionConfigPage />, ['/admin/config']);

    fireEvent.click(await screen.findByRole('tab', { name: 'WhatsApp' }));
    expect(await screen.findByAltText('WhatsApp authentication QR code')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Generate QR/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Generate QR/ }));
    await waitFor(() => expect(start).toHaveBeenCalledWith('qr', undefined));
  });
});
