import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LiveChannelsPage } from './LiveChannelsPage';
import type {
  LiveChannel,
  LiveChannelEvent,
  LiveChannelSettings,
  LiveChannelUser,
} from '../api/liveChannels';
import type { UserHomeChannel } from '../api/me';

const liveApi = vi.hoisted(() => ({
  getLiveChannels: vi.fn(),
  getLiveChannelEventStreamUrl: vi.fn(),
  getLiveChannelSettings: vi.fn(),
  getLiveChannelUsers: vi.fn(),
  saveAndApplyLiveChannelSettings: vi.fn(),
  sendLiveChannelMessage: vi.fn(),
  setLiveChannelIrcOperatorMode: vi.fn(),
}));

const meApi = vi.hoisted(() => ({ getMe: vi.fn() }));

vi.mock('../api/liveChannels', async () => {
  const actual = await vi.importActual<typeof import('../api/liveChannels')>('../api/liveChannels');
  return { ...actual, ...liveApi };
});

vi.mock('../api/me', async () => {
  const actual = await vi.importActual<typeof import('../api/me')>('../api/me');
  return { ...actual, ...meApi };
});

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  readonly url: string;
  closed = false;
  onopen: (() => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  onerror: (() => void) | null = null;

  constructor(url: string) {
    this.url = url;
    FakeEventSource.instances.push(this);
  }

  close() {
    this.closed = true;
  }

  open() {
    this.onopen?.();
  }

  message(event: LiveChannelEvent) {
    this.onmessage?.({ data: JSON.stringify(event) } as MessageEvent<string>);
  }

  error() {
    this.onerror?.();
  }
}

const channel: LiveChannel = {
  echoToAlias: 'IRC-TEST',
  label: '#test',
  connectionType: 'IRC',
  network: 'IRCNet',
  channelType: 'IrcPublic',
  sendAllowed: true,
  adminAllowed: true,
  modeAllowed: true,
};

const viewOnlyChannel: LiveChannel = {
  ...channel,
  echoToAlias: 'IRC-VIEW',
  label: '#view-only',
  sendAllowed: false,
  adminAllowed: false,
  modeAllowed: false,
};

const settings: LiveChannelSettings = {
  publicAiEnabled: true,
  allowAnonymousAiCommands: false,
  resolveUrls: true,
  captureResolvedUrls: false,
  captureImages: false,
};

function user(overrides: Partial<LiveChannelUser>): LiveChannelUser {
  return {
    account: null,
    awayMessage: null,
    host: null,
    nick: null,
    operatorInformation: null,
    realName: null,
    server: null,
    userString: null,
    displayPrefix: null,
    channelModes: null,
    channelRoles: null,
    away: false,
    ...overrides,
  };
}

function renderPage(homeChannel: UserHomeChannel | null = {
  connectionType: 'IRC',
  network: 'IRCNet',
  echoToAlias: channel.echoToAlias,
  label: channel.label,
}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MantineProvider>
        <LiveChannelsPage homeChannel={homeChannel} />
      </MantineProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  FakeEventSource.instances = [];
  Object.defineProperty(window, 'EventSource', {
    configurable: true,
    writable: true,
    value: FakeEventSource,
  });
  liveApi.getLiveChannels.mockResolvedValue([channel]);
  liveApi.getLiveChannelEventStreamUrl.mockReturnValue('/api/web/live-channels/stream?echoToAlias=IRC-TEST&afterId=0');
  liveApi.getLiveChannelUsers.mockResolvedValue([
    user({ nick: 'zeta' }),
    user({ nick: 'Pete', displayPrefix: '@', realName: 'Petri' }),
    user({ nick: 'Alice' }),
  ]);
  liveApi.getLiveChannelSettings.mockResolvedValue(settings);
  liveApi.saveAndApplyLiveChannelSettings.mockResolvedValue({ status: 'OK', settings });
  liveApi.sendLiveChannelMessage.mockResolvedValue({ sent: true, sentTo: '#test', message: 'hello' });
  liveApi.setLiveChannelIrcOperatorMode.mockResolvedValue({
    echoToAlias: channel.echoToAlias,
    botHasOperator: true,
    operator: true,
    changed: ['Alice'],
    unchanged: [],
    error: null,
  });
  meApi.getMe.mockResolvedValue({});
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('LiveChannelsPage', () => {
  it('opens the home channel and renders users in operator-first order', async () => {
    renderPage();

    expect(await screen.findByText('@Pete')).toBeInTheDocument();
    const rows = [...document.querySelectorAll('.live-channel-user-row')];
    expect(rows.map((row) => row.querySelector('.live-channel-user-details .mantine-Text-root')?.textContent)).toEqual([
      '@Pete',
      'Alice',
      'zeta',
    ]);
    expect(screen.getByText('Live stream reconnecting...')).toBeInTheDocument();
  });

  it('opens and closes a selected channel tab', async () => {
    const user = userEvent.setup();
    liveApi.getLiveChannels.mockResolvedValue([channel, viewOnlyChannel]);
    renderPage(null);

    const channelSelect = await screen.findByPlaceholderText('Select public channel');
    await user.click(channelSelect);
    await user.click(screen.getByText('#view-only'));
    await user.click(screen.getByRole('button', { name: 'Open tab' }));
    expect(await screen.findByText('You have view-only access to this channel.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Close IRC-VIEW' }));
    await waitFor(() => expect(screen.queryByText('You have view-only access to this channel.')).not.toBeInTheDocument());
  });

  it('keeps checkbox selection stable and sends the selected users to the operator API', async () => {
    const user = userEvent.setup();
    renderPage();

    const checkbox = await screen.findByRole('checkbox', { name: 'Select Alice' });
    await user.click(checkbox);
    expect(checkbox).toBeChecked();
    expect(screen.getByRole('button', { name: 'Op selected' })).toBeEnabled();

    await user.click(screen.getByRole('button', { name: 'Op selected' }));
    await waitFor(() => expect(liveApi.setLiveChannelIrcOperatorMode).toHaveBeenCalledWith(
      'IRC-TEST',
      ['Alice'],
      true,
    ));
    await waitFor(() => {
      const output = screen.getByLabelText('IRC-TEST live output') as HTMLTextAreaElement;
      expect(output.value).toContain('system> opped: Alice');
    });
  });

  it('supports deselecting a user without losing the rendered page', async () => {
    const user = userEvent.setup();
    renderPage();

    const checkbox = await screen.findByRole('checkbox', { name: 'Select Alice' });
    await user.click(checkbox);
    await user.click(checkbox);

    expect(checkbox).not.toBeChecked();
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(liveApi.setLiveChannelIrcOperatorMode).not.toHaveBeenCalled();
  });

  it('sends a message and restores the input for another message', async () => {
    const user = userEvent.setup();
    renderPage();

    const input = await screen.findByPlaceholderText('Message to channel');
    await user.type(input, 'hello channel');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Send' })).toBeEnabled());
    await user.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => expect(liveApi.sendLiveChannelMessage).toHaveBeenCalledWith('IRC-TEST', 'hello channel'));
    expect(input).toHaveValue('');
    expect(input).toHaveFocus();
  });

  it('does not expose send, settings, or operator controls for a view-only channel', async () => {
    liveApi.getLiveChannels.mockResolvedValue([viewOnlyChannel]);
    liveApi.getLiveChannelUsers.mockResolvedValue([]);
    renderPage({
      connectionType: 'IRC',
      network: 'IRCNet',
      echoToAlias: viewOnlyChannel.echoToAlias,
      label: viewOnlyChannel.label,
    });

    expect(await screen.findByText('You have view-only access to this channel.')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Message to channel')).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Channel settings for IRC-VIEW' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Op selected' })).not.toBeInTheDocument();
  });

  it('loads, edits, and saves channel settings for an admin channel', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Channel settings for IRC-TEST' }));
    await waitFor(() => expect(liveApi.getLiveChannelSettings).toHaveBeenCalledWith('IRC-TEST'));
    const publicAi = await screen.findByLabelText('Public AI');
    await user.click(publicAi);
    await user.click(screen.getByRole('button', { name: 'Save and apply' }));

    await waitFor(() => expect(liveApi.saveAndApplyLiveChannelSettings).toHaveBeenCalledWith(
      'IRC-TEST',
      { ...settings, publicAiEnabled: false },
    ));
    expect(await screen.findByText('OK')).toBeInTheDocument();
  });

  it('connects to SSE, appends events once, and reports stream failures', async () => {
    renderPage();
    await screen.findByText('@Pete');
    const source = FakeEventSource.instances[0];
    source.open();
    expect(await screen.findByText('Live stream connected')).toBeInTheDocument();

    const event: LiveChannelEvent = {
      id: 7,
      requestId: 7,
      createdAt: new Date(2026, 7, 6, 12, 34, 56).getTime(),
      echoToAlias: 'IRC-TEST',
      sender: 'Alice',
      senderId: 'alice',
      message: 'hello from IRC',
      protocol: 'irc',
      network: 'IRCNet',
      chatType: 'channel',
      chatId: '#test',
      direction: 'INBOUND',
    };
    source.message(event);
    source.message(event);
    await waitFor(() => {
      const output = screen.getByLabelText('IRC-TEST live output') as HTMLTextAreaElement;
      expect(output.value.match(/Alice: hello from IRC/g)).toHaveLength(1);
    });

    source.error();
    expect(await screen.findByText('Live stream disconnected; reconnecting...')).toBeInTheDocument();
  });
});
