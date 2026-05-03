import { getJson } from './client';

export type KnownUserTarget = {
  logicalUserKey: string | null;
  configuredUserId: number | null;
  configuredUsername: string | null;
  configuredName: string | null;
  matchedConfiguredUser: boolean;
  matchSource: string | null;
  observedUserKey: string | null;
  observedUserId: string | null;
  observedUsername: string | null;
  observedDisplayName: string | null;
  connectionId: number;
  connectionType: string | null;
  network: string | null;
  channelId: string | null;
  channelName: string | null;
  echoToAlias: string | null;
  targetType: string | null;
  lastSeenAt: number | null;
  lastSeenSource: string | null;
};

export type KnownUserTargetsResponse = {
  targets: KnownUserTarget[] | null;
};

export async function getKnownUserTargets(query: string): Promise<KnownUserTarget[]> {
  const params = new URLSearchParams();
  if (query.trim()) {
    params.set('query', query.trim());
  }

  const path = params.size > 0
    ? `/api/web/known-users/targets?${params.toString()}`
    : '/api/web/known-users/targets';
  const response = await getJson<KnownUserTargetsResponse>(path);
  return response.targets ?? [];
}
