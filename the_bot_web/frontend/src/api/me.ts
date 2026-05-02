import { getJson, putJson } from './client';

export interface MeResponse {
  id: number | null;
  username: string;
  name: string | null;
  email: string | null;
  ircNick: string | null;
  telegramId: string | null;
  discordId: string | null;
  admin: boolean;
  canDoIrcOp: boolean;
  roles: string[];
}

export function getMe(): Promise<MeResponse> {
  return getJson<MeResponse>('/api/web/me');
}

export interface ProfileUpdateRequest {
  name: string;
  email: string;
  ircNick: string;
  telegramId: string;
  discordId: string;
}

export function updateProfile(request: ProfileUpdateRequest): Promise<MeResponse> {
  return putJson<MeResponse>('/api/web/me/profile', request);
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface PasswordChangeResponse {
  changed: boolean;
}

export function changePassword(request: PasswordChangeRequest): Promise<PasswordChangeResponse> {
  return putJson<PasswordChangeResponse>('/api/web/me/password', request);
}
