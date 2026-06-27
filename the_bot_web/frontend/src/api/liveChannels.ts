import { getJson, postJson } from './client';

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

export async function getLiveChannelEvents(echoToAlias: string, afterId: number): Promise<LiveChannelEvent[]> {
  const params = new URLSearchParams({
    echoToAlias,
    afterId: `${afterId}`,
  });
  const response = await getJson<LiveChannelEventsResponse>(`/api/web/admin/live-channels/events?${params.toString()}`);
  return response.events ?? [];
}

export async function getLiveChannelUsers(echoToAlias: string): Promise<LiveChannelUser[]> {
  const params = new URLSearchParams({ echoToAlias });
  const response = await getJson<LiveChannelUsersResponse>(`/api/web/admin/live-channels/users?${params.toString()}`);
  return response.channelUsers ?? [];
}

export async function sendLiveChannelMessage(echoToAlias: string, message: string): Promise<LiveChannelSendResponse> {
  return postJson<LiveChannelSendResponse>('/api/web/admin/live-channels/send', {
    echoToAlias,
    message,
  });
}
