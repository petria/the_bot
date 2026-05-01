import { getJson } from './client';

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
