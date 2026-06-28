export const WEB_USER_PERMISSION = 'web.user';
export const WEB_ADMIN_PERMISSION = 'web.admin';
export const CONFIG_EDIT_PERMISSION = 'config.edit';
export const CHANNELS_VIEW_ALL_PERMISSION = 'channels.view.all';
export const CHANNELS_VIEW_PREFIX = 'channels.view.';
export const LEGACY_LIVE_CHANNELS_VIEW_ALL_PERMISSION = 'live-channels.view.all';
export const LEGACY_LIVE_CHANNELS_VIEW_PREFIX = 'live-channels.view.';

export function hasPermission(permissions: string[] | undefined | null, permission: string) {
  if (!permissions) {
    return false;
  }
  return permissions.includes('*')
    || permissions.includes(permission)
    || (permission === WEB_USER_PERMISSION && permissions.includes(WEB_ADMIN_PERMISSION));
}

export function hasAnyChannelViewPermission(permissions: string[] | undefined | null) {
  if (!permissions) {
    return false;
  }
  return permissions.includes('*')
    || permissions.includes(CHANNELS_VIEW_ALL_PERMISSION)
    || permissions.includes(LEGACY_LIVE_CHANNELS_VIEW_ALL_PERMISSION)
    || permissions.some((permission) =>
      permission.startsWith(CHANNELS_VIEW_PREFIX) || permission.startsWith(LEGACY_LIVE_CHANNELS_VIEW_PREFIX));
}
