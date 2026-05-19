export const WEB_USER_PERMISSION = 'web.user';
export const WEB_ADMIN_PERMISSION = 'web.admin';
export const CONFIG_EDIT_PERMISSION = 'config.edit';

export function hasPermission(permissions: string[] | undefined | null, permission: string) {
  if (!permissions) {
    return false;
  }
  return permissions.includes('*')
    || permissions.includes(permission)
    || (permission === WEB_USER_PERMISSION && permissions.includes(WEB_ADMIN_PERMISSION));
}
