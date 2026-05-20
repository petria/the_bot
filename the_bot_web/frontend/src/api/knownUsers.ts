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

export function connectionDisplayName(target: Pick<KnownUserTarget, 'connectionType' | 'network'>): string {
  const type = (target.connectionType || 'UNKNOWN')
      .replace(/_CONNECTION$/i, '')
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (match) => match.toUpperCase());
  return target.network ? `${type} / ${target.network}` : type;
}

export function observedPrimaryName(target: KnownUserTarget): string {
  return firstNonBlank(
      target.observedDisplayName,
      target.observedUsername,
      readableObservedUserId(target),
      target.observedUserKey,
      '-',
  );
}

export function observedSecondaryText(target: KnownUserTarget): string {
  const type = target.connectionType || '';
  if (type === 'IRC_CONNECTION') {
    return firstNonBlank(target.observedUserId, target.observedDisplayName, target.observedUserKey, '-');
  }
  if (type === 'WHATSAPP_CONNECTION') {
    return firstNonBlank(readableObservedUserId(target), target.observedUserKey, '-');
  }
  if (type === 'DISCORD_CONNECTION' || type === 'TELEGRAM_CONNECTION') {
    const id = readableObservedUserId(target);
    return id ? `id ${id}` : target.observedUserKey || '-';
  }
  return firstNonBlank(target.observedUserId, target.observedUserKey, '-');
}

export function observedOptionLabel(target: KnownUserTarget): string {
  const channel = target.channelName || target.echoToAlias || target.channelId || 'private';
  return `${observedPrimaryName(target)} / ${connectionDisplayName(target)} / ${channel}`;
}

function readableObservedUserId(target: KnownUserTarget): string | null {
  const id = target.observedUserId?.trim();
  if (!id) {
    return null;
  }
  if (target.connectionType === 'WHATSAPP_CONNECTION') {
    return id.replace(/@s\.whatsapp\.net$/i, '').replace(/@lid$/i, ' @lid');
  }
  if (target.connectionType === 'DISCORD_CONNECTION' && id.length > 12) {
    return `${id.slice(0, 6)}...${id.slice(-4)}`;
  }
  return id;
}

function firstNonBlank(...values: Array<string | null | undefined>): string {
  return values.find((value) => value !== null && value !== undefined && value.trim() !== '')?.trim() ?? '';
}
