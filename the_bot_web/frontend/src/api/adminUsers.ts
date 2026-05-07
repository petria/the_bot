import { deleteJson, getJson, postJson, putJson } from './client';

export interface AdminUser {
  id: number | null;
  username: string | null;
  name: string | null;
  email: string | null;
  ircNick: string | null;
  telegramId: string | null;
  discordId: string | null;
  admin: boolean;
  canDoIrcOp: boolean;
  reserved: boolean;
}

export interface AdminUsersResponse {
  users: AdminUser[];
}

export interface AdminUserCreateRequest {
  username: string;
  password: string;
  name: string;
  email: string;
  ircNick: string;
  telegramId: string;
  discordId: string;
  admin: boolean;
  canDoIrcOp: boolean;
}

export type AdminUserUpdateRequest = Omit<AdminUserCreateRequest, 'username' | 'password'>;

export interface AdminPasswordResetRequest {
  password: string;
}

export function getAdminUsers(): Promise<AdminUsersResponse> {
  return getJson<AdminUsersResponse>('/api/web/admin/users');
}

export function createAdminUser(request: AdminUserCreateRequest): Promise<AdminUser> {
  return postJson<AdminUser>('/api/web/admin/users', request);
}

export function updateAdminUser(id: number, request: AdminUserUpdateRequest): Promise<AdminUser> {
  return putJson<AdminUser>(`/api/web/admin/users/${id}`, request);
}

export function resetAdminUserPassword(id: number, request: AdminPasswordResetRequest): Promise<AdminUser> {
  return putJson<AdminUser>(`/api/web/admin/users/${id}/password`, request);
}

export function deleteAdminUser(id: number): Promise<AdminUser> {
  return deleteJson<AdminUser>(`/api/web/admin/users/${id}`);
}
