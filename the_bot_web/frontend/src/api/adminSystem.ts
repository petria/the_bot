import { getJson, postJson } from './client';

export type OpenClawInstanceOption = {
  id: string;
  label: string;
  host: string;
  wsUrl: string;
  originUrl: string;
  healthUrl: string;
  selected: boolean;
};

export type OpenClawSettingsResponse = {
  currentInstanceId: string | null;
  currentWsUrl: string | null;
  currentOriginUrl: string | null;
  currentHealthUrl: string | null;
  options: OpenClawInstanceOption[];
};

export function getOpenClawSettings(): Promise<OpenClawSettingsResponse> {
  return getJson<OpenClawSettingsResponse>('/api/web/admin/system/openclaw');
}

export function updateOpenClawSettings(selectedInstanceId: string): Promise<OpenClawSettingsResponse> {
  return postJson<OpenClawSettingsResponse>('/api/web/admin/system/openclaw', { selectedInstanceId });
}
