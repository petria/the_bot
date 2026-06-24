import { deleteJson, getJson, postJson, putJson } from './client';

export interface MeResponse {
  id: number | null;
  username: string;
  name: string | null;
  email: string | null;
  ircNick: string | null;
  telegramId: string | null;
  discordId: string | null;
  whatsappId: string | null;
  permissions: string[];
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
  whatsappId: string;
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

export interface IrcClaimTokenResponse {
  token: string;
  expiresAt: number;
}

export function createIrcClaimToken(): Promise<IrcClaimTokenResponse> {
  return postJson<IrcClaimTokenResponse>('/api/web/me/irc-claim-token', {});
}

export type NotifyPatternType = 'PRESET_MENTION' | 'REGEX';

export interface UserNotifyRule {
  id: string | null;
  username: string | null;
  enabled: boolean;
  sourceEchoToAlias: string;
  sourceDisplayName: string | null;
  patternType: NotifyPatternType;
  pattern: string | null;
  destinationConnectionType: string;
  cooldownSeconds: number;
  createdAt: number;
  updatedAt: number;
}

export type UserNotifyRuleInput = Omit<UserNotifyRule, 'id' | 'username' | 'createdAt' | 'updatedAt'>;

type UserNotifyRuleListResponse = {
  rules: UserNotifyRule[] | null;
};

export async function getNotifyRules(): Promise<UserNotifyRule[]> {
  const response = await getJson<UserNotifyRuleListResponse>('/api/web/me/notify-rules');
  return response.rules ?? [];
}

export function createNotifyRule(rule: UserNotifyRuleInput): Promise<UserNotifyRule> {
  return postJson<UserNotifyRule>('/api/web/me/notify-rules', rule);
}

export function updateNotifyRule(id: string, rule: UserNotifyRuleInput): Promise<UserNotifyRule> {
  return putJson<UserNotifyRule>(`/api/web/me/notify-rules/${encodeURIComponent(id)}`, rule);
}

export function deleteNotifyRule(id: string): Promise<void> {
  return deleteJson<void>(`/api/web/me/notify-rules/${encodeURIComponent(id)}`);
}
