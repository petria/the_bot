import { getJson, postJson, putJson } from './client';

export type LiveChannelDirection = 'INBOUND' | 'WEB_OUTBOUND';

export type LiveChannelEvent = {
  id: number;
  requestId: number;
  createdAt: number;
  echoToAlias: string | null;
  sender: string | null;
  senderId: string | null;
  message: string | null;
  protocol: string | null;
  network: string | null;
  chatType: string | null;
  chatId: string | null;
  direction: LiveChannelDirection | null;
};

export type LiveChannelSendResponse = {
  sent: boolean;
  sentTo: string | null;
  message: string | null;
};

export type LiveChannel = {
  echoToAlias: string;
  label: string;
  connectionType: string | null;
  network: string | null;
  channelType: string | null;
  sendAllowed: boolean;
  adminAllowed: boolean;
};

export type LiveChannelSettings = {
  publicAiEnabled: boolean;
  allowAnonymousAiCommands: boolean;
  resolveUrls: boolean;
  captureResolvedUrls: boolean;
  captureImages: boolean;
};

export type LiveChannelSettingsApplyResponse = {
  status: string;
  settings: LiveChannelSettings;
};

export type LiveChannelUser = {
  account: string | null;
  awayMessage: string | null;
  host: string | null;
  nick: string | null;
  operatorInformation: string | null;
  realName: string | null;
  server: string | null;
  userString: string | null;
  displayPrefix: string | null;
  channelModes: string[] | null;
  channelRoles: string[] | null;
  away: boolean;
};

type LiveChannelEventsResponse = {
  events: LiveChannelEvent[] | null;
};

type LiveChannelUsersResponse = {
  channelUsers: LiveChannelUser[] | null;
};

type LiveChannelsResponse = {
  channels: LiveChannel[] | null;
};

export async function getLiveChannels(): Promise<LiveChannel[]> {
  const response = await getJson<LiveChannelsResponse>('/api/web/live-channels/channels');
  return response.channels ?? [];
}

export async function getLiveChannelEvents(echoToAlias: string, afterId: number): Promise<LiveChannelEvent[]> {
  const params = new URLSearchParams({
    echoToAlias,
    afterId: `${afterId}`,
  });
  const response = await getJson<LiveChannelEventsResponse>(`/api/web/live-channels/events?${params.toString()}`);
  return response.events ?? [];
}

export function getLiveChannelEventStreamUrl(echoToAlias: string, afterId: number): string {
  const params = new URLSearchParams({
    echoToAlias,
    afterId: `${afterId}`,
  });
  return `/api/web/live-channels/stream?${params.toString()}`;
}

export async function getLiveChannelUsers(echoToAlias: string): Promise<LiveChannelUser[]> {
  const params = new URLSearchParams({ echoToAlias });
  const response = await getJson<LiveChannelUsersResponse>(`/api/web/live-channels/users?${params.toString()}`);
  return response.channelUsers ?? [];
}

export async function getLiveChannelSettings(echoToAlias: string): Promise<LiveChannelSettings> {
  const params = new URLSearchParams({ echoToAlias });
  return getJson<LiveChannelSettings>(`/api/web/live-channels/settings?${params.toString()}`);
}

export async function saveAndApplyLiveChannelSettings(
  echoToAlias: string,
  settings: LiveChannelSettings,
): Promise<LiveChannelSettingsApplyResponse> {
  return putJson<LiveChannelSettingsApplyResponse>('/api/web/live-channels/settings', {
    echoToAlias,
    ...settings,
  });
}

export async function sendLiveChannelMessage(echoToAlias: string, message: string): Promise<LiveChannelSendResponse> {
  return postJson<LiveChannelSendResponse>('/api/web/live-channels/send', {
    echoToAlias,
    message,
  });
}
