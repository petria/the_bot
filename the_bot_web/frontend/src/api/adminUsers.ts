import { deleteJson, getJson, postJson, putJson } from './client';

export interface AdminChatIdentity {
  identityKey: string | null;
  connectionType: string | null;
  network: string | null;
  userId: string | null;
  username: string | null;
  displayName: string | null;
  source: string | null;
  linkedAt: number | null;
  linkedBy: string | null;
}

export interface AdminUser {
  id: number | null;
  username: string | null;
  name: string | null;
  email: string | null;
  ircNick: string | null;
  telegramId: string | null;
  discordId: string | null;
  whatsappId: string | null;
  chatIdentities: AdminChatIdentity[] | null;
  permissions: string[];
  reserved: boolean;
}

export interface AdminUsersResponse {
  users: AdminUser[];
  availablePermissions: string[];
}

export interface AdminUserCreateRequest {
  username: string;
  password: string;
  name: string;
  email: string;
  ircNick: string;
  telegramId: string;
  discordId: string;
  whatsappId: string;
  permissions: string[];
}

export type AdminUserUpdateRequest = Omit<AdminUserCreateRequest, 'username' | 'password'>;

export interface AdminPasswordResetRequest {
  password: string;
}

export interface AdminChatIdentityLinkRequest {
  connectionType: string | null;
  network: string | null;
  observedUserId: string | null;
  observedUsername: string | null;
  observedDisplayName: string | null;
  source: string | null;
  moveIfOwned: boolean;
}

export interface AdminObservedUserCreateRequest {
  username: string;
  password: string;
  name: string;
  email: string;
  connectionType: string | null;
  network: string | null;
  echoToAlias: string | null;
  observedUserId: string | null;
  observedUsername: string | null;
  observedDisplayName: string | null;
  source: string | null;
}

export function getAdminUsers(): Promise<AdminUsersResponse> {
  return getJson<AdminUsersResponse>('/api/web/admin/users');
}

export function createAdminUser(request: AdminUserCreateRequest): Promise<AdminUser> {
  return postJson<AdminUser>('/api/web/admin/users', request);
}

export function createAdminUserFromObservedIdentity(request: AdminObservedUserCreateRequest): Promise<AdminUser> {
  return postJson<AdminUser>('/api/web/admin/users/observed', request);
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

export function linkAdminUserChatIdentity(id: number, request: AdminChatIdentityLinkRequest): Promise<AdminUser> {
  return postJson<AdminUser>(`/api/web/admin/users/${id}/chat-identities`, request);
}

export function unlinkAdminUserChatIdentity(id: number, identityKey: string): Promise<AdminUser> {
  return deleteJson<AdminUser>(`/api/web/admin/users/${id}/chat-identities/${encodeURIComponent(identityKey)}`);
}
