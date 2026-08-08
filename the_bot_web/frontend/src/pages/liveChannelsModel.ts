import type { LiveChannel, LiveChannelEvent, LiveChannelUser } from '../api/liveChannels';

export type OpenChannel = Pick<LiveChannel, 'echoToAlias' | 'label' | 'sendAllowed' | 'adminAllowed' | 'modeAllowed'>;

export const maxMessageLength = 900;

const openChannelsStorageKey = 'the-bot-live-channels-open';
const activeAliasStorageKey = 'the-bot-live-channels-active';

export function openChannelFromLiveChannel(channel: LiveChannel): OpenChannel {
  return {
    echoToAlias: channel.echoToAlias,
    label: channel.label,
    sendAllowed: channel.sendAllowed,
    adminAllowed: channel.adminAllowed,
    modeAllowed: channel.modeAllowed,
  };
}

export function userDisplayName(user: LiveChannelUser) {
  const baseName = user.nick || user.realName || user.account || user.userString || 'unknown';
  const prefix = user.displayPrefix?.trim() || '';
  return `${prefix}${baseName}`;
}

export function compareChannelUsers(left: LiveChannelUser, right: LiveChannelUser) {
  const leftOperator = isChannelOperator(left);
  const rightOperator = isChannelOperator(right);
  if (leftOperator !== rightOperator) {
    return leftOperator ? -1 : 1;
  }
  return userSortName(left).localeCompare(userSortName(right), undefined, { sensitivity: 'base' });
}

export function userSortName(user: LiveChannelUser) {
  return user.nick || user.realName || user.account || user.userString || 'unknown';
}

export function isChannelOperator(user: LiveChannelUser) {
  const prefix = user.displayPrefix?.trim();
  if (prefix === '@') {
    return true;
  }
  const modes = user.channelModes?.map((mode) => mode.trim().toLowerCase()) ?? [];
  if (modes.includes('@') || modes.includes('o')) {
    return true;
  }
  const roles = user.channelRoles?.map((role) => role.trim().toLowerCase()) ?? [];
  return roles.includes('operator') || roles.includes('op');
}

export function userDetail(user: LiveChannelUser) {
  const roles = user.channelRoles?.filter(Boolean) ?? [];
  const modes = user.channelModes?.filter((mode) => mode && mode !== user.displayPrefix) ?? [];
  const parts = [
    user.realName && user.realName !== user.nick ? user.realName : null,
    ...roles,
    ...modes,
    user.operatorInformation && !roles.some((role) => role.toLowerCase() === user.operatorInformation?.toLowerCase())
      ? user.operatorInformation
      : null,
    user.away ? 'away' : null,
  ].filter(Boolean);
  return parts.length === 0 ? (user.account || user.userString || '-') : parts.join(' / ');
}

export function userKey(user: LiveChannelUser, index: number) {
  return user.account || user.userString || user.nick || user.realName || `user-${index}`;
}

export function formatEvent(event: LiveChannelEvent) {
  const timestamp = formatTime(event.createdAt);
  const message = event.message || '';
  if (event.direction === 'WEB_OUTBOUND') {
    return `${timestamp} ${message}`;
  }
  const sender = event.sender || 'unknown';
  return `${timestamp} ${sender}: ${message}`;
}

export function formatTime(value: number) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '--:--:--';
  }
  return [
    date.getHours(),
    date.getMinutes(),
    date.getSeconds(),
  ].map((part) => part.toString().padStart(2, '0')).join(':');
}

export function readOpenChannels(storage: Storage = window.sessionStorage): OpenChannel[] {
  try {
    const raw = storage.getItem(openChannelsStorageKey);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw) as OpenChannel[];
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
        .filter((channel) => typeof channel.echoToAlias === 'string' && typeof channel.label === 'string')
        .map((channel) => ({
          echoToAlias: channel.echoToAlias,
          label: channel.label,
          sendAllowed: channel.sendAllowed === true,
          adminAllowed: channel.adminAllowed === true,
          modeAllowed: channel.modeAllowed === true,
        }));
  } catch {
    return [];
  }
}

export function hasSavedOpenChannels(storage: Storage = window.sessionStorage) {
  return storage.getItem(openChannelsStorageKey) !== null;
}

export function readActiveAlias(storage: Storage = window.sessionStorage) {
  return storage.getItem(activeAliasStorageKey);
}
