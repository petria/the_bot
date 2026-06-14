import { getJson, postJson, putJson } from './client';

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

export type HermesFallbackProfileStatus = {
  profileId: string;
  expectedRoute: string;
  healthy: boolean;
  openAiAvailable: boolean;
  cooldownUntil: string | null;
  detail: string;
};

export type HermesFallbackSettingsResponse = {
  enabled: boolean;
  baseUrl: string;
  model: string;
  profiles: HermesFallbackProfileStatus[];
};

export type HermesProfile = {
  id: string;
  label: string;
  provider: string;
  baseUrl: string | null;
  model: string;
  apiMode: string;
  timeoutSeconds: number | null;
  healthy: boolean | null;
  toolCapable: boolean | null;
  detail: string | null;
  contextWindow: number | null;
};

export type HermesBackendConfigResponse = {
  profiles: HermesProfile[];
};

export function getHermesSettings(): Promise<HermesSettingsResponse> {
  return getJson<HermesSettingsResponse>('/api/web/admin/system/hermes');
}

export function updateHermesSettings(selectedProfileId: string): Promise<HermesSettingsResponse> {
  return postJson<HermesSettingsResponse>('/api/web/admin/system/hermes', { selectedProfileId });
}

export function getHermesFallback(): Promise<HermesFallbackSettingsResponse> {
  return getJson<HermesFallbackSettingsResponse>('/api/web/admin/system/hermes/fallback');
}

export function getHermesFallbackModels(baseUrl: string): Promise<string[]> {
  return getJson<{ models: string[] }>(
    `/api/web/admin/system/hermes/fallback/models?baseUrl=${encodeURIComponent(baseUrl)}`
  ).then((response) => response.models);
}

export function updateHermesFallback(baseUrl: string, model: string, enabled: boolean): Promise<HermesFallbackSettingsResponse> {
  return putJson<HermesFallbackSettingsResponse>('/api/web/admin/system/hermes/fallback', { baseUrl, model, enabled });
}

export function getHermesBackendConfig(): Promise<HermesBackendConfigResponse> {
  return getJson<HermesBackendConfigResponse>('/api/web/admin/system/hermes/backends');
}

export function updateHermesBackendConfig(config: HermesBackendConfigResponse): Promise<HermesBackendConfigResponse> {
  return putJson<HermesBackendConfigResponse>('/api/web/admin/system/hermes/backends', config);
}
