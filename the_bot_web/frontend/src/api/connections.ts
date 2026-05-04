import { getJson } from './client';

export type BotConnectionChannel = {
  id: string | null;
  type: string | null;
  network: string | null;
  name: string | null;
  echoToAlias: string | null;
};

export type BotConnection = {
  id: number;
  type: string | null;
  network: string | null;
  channels: BotConnectionChannel[] | null;
};

export type ChannelActivity = {
  echoToAlias: string | null;
  type: string | null;
  network: string | null;
  name: string | null;
  lastReceivedMessageAt: number | null;
  lastReceivedMessageBy: string | null;
  lastReceivedMessageSource: string | null;
};

type ConnectionMapResponse = {
  connectionMap: Record<string, BotConnection> | null;
};

type ChannelActivityResponse = {
  channels: ChannelActivity[] | null;
};

export type ConnectionsOverview = {
  connections: BotConnection[];
  activities: ChannelActivity[];
};

export async function getConnectionsOverview(): Promise<ConnectionsOverview> {
  const [connectionMapResponse, activityResponse] = await Promise.all([
    getJson<ConnectionMapResponse>('/api/web/connections/map'),
    getJson<ChannelActivityResponse>('/api/web/connections/activity'),
  ]);

  const activities = activityResponse.channels ?? [];
  const connections = mergeActivityChannels(Object.values(connectionMapResponse.connectionMap ?? {}), activities)
      .sort(compareConnections);

  return { connections, activities };
}

function mergeActivityChannels(connections: BotConnection[], activities: ChannelActivity[]) {
  return connections.map((connection) => {
    const channelsByAlias = new Map<string, BotConnectionChannel>();
    for (const channel of connection.channels ?? []) {
      if (channel.echoToAlias) {
        channelsByAlias.set(channel.echoToAlias, channel);
      }
    }

    for (const activity of activities) {
      if (!activity.echoToAlias || !activityMatchesConnection(activity, connection)) {
        continue;
      }
      if (!channelsByAlias.has(activity.echoToAlias)) {
        channelsByAlias.set(activity.echoToAlias, {
          id: activity.echoToAlias,
          type: activity.type,
          network: activity.network,
          name: activity.name,
          echoToAlias: activity.echoToAlias,
        });
      }
    }

    return {
      ...connection,
      channels: Array.from(channelsByAlias.values()),
    };
  });
}

function activityMatchesConnection(activity: ChannelActivity, connection: BotConnection) {
  return activity.type === connection.type && activity.network === connection.network;
}

function compareConnections(left: BotConnection, right: BotConnection) {
  return sortText(left.type || '', right.type || '')
      || sortText(left.network || '', right.network || '')
      || left.id - right.id;
}

function sortText(left: string, right: string) {
  return left.localeCompare(right, undefined, { sensitivity: 'base' });
}
