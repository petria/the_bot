export const WEB_ADMIN_PERMISSION = 'web.admin';
export const CONFIG_EDIT_PERMISSION = 'config.edit';

export function hasPermission(permissions: string[] | undefined | null, permission: string) {
  if (!permissions) {
    return false;
  }
  return permissions.includes('*') || permissions.includes(permission);
}
