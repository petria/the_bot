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

type LiveChannelEventsResponse = {
  events: LiveChannelEvent[] | null;
};

export async function getLiveChannelEvents(echoToAlias: string, afterId: number): Promise<LiveChannelEvent[]> {
  const params = new URLSearchParams({
    echoToAlias,
    afterId: `${afterId}`,
  });
  const response = await getJson<LiveChannelEventsResponse>(`/api/web/admin/live-channels/events?${params.toString()}`);
  return response.events ?? [];
}

export async function sendLiveChannelMessage(echoToAlias: string, message: string): Promise<LiveChannelSendResponse> {
  return postJson<LiveChannelSendResponse>('/api/web/admin/live-channels/send', {
    echoToAlias,
    message,
  });
}
