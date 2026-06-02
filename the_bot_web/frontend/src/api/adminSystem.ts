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

export type HermesProfileOption = {
  id: string;
  label: string;
  baseUrl: string;
  model: string;
  apiMode: string;
  timeoutSeconds: number | null;
  healthUrl: string;
  selected: boolean;
};

export type HermesSettingsResponse = {
  currentProfileId: string | null;
  baseUrl: string | null;
  model: string | null;
  apiMode: string | null;
  timeoutSeconds: number | null;
  configured: boolean;
  healthUrl: string | null;
  options: HermesProfileOption[];
};

export function getOpenClawSettings(): Promise<OpenClawSettingsResponse> {
  return getJson<OpenClawSettingsResponse>('/api/web/admin/system/openclaw');
}

export function updateOpenClawSettings(selectedInstanceId: string): Promise<OpenClawSettingsResponse> {
  return postJson<OpenClawSettingsResponse>('/api/web/admin/system/openclaw', { selectedInstanceId });
}

export function getHermesSettings(): Promise<HermesSettingsResponse> {
  return getJson<HermesSettingsResponse>('/api/web/admin/system/hermes');
}

export function updateHermesSettings(selectedProfileId: string): Promise<HermesSettingsResponse> {
  return postJson<HermesSettingsResponse>('/api/web/admin/system/hermes', { selectedProfileId });
}
