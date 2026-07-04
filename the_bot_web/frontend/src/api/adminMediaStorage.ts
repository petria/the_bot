import { getJson, putJson } from './client';

export type MediaStorageSettings = {
  enabled: boolean;
  storageDir: string;
  publicUrlPrefix: string;
  maxFileSizeMb: number;
  retentionDays: number;
  directoryExists: boolean;
  writable: boolean;
  detail: string | null;
};

export type MediaStorageUpdate = {
  enabled: boolean;
  storageDir: string;
  maxFileSizeMb: number;
  retentionDays: number;
};

export function getMediaStorageSettings(): Promise<MediaStorageSettings> {
  return getJson<MediaStorageSettings>('/api/web/admin/system/media-storage');
}

export function updateMediaStorageSettings(settings: MediaStorageUpdate): Promise<MediaStorageSettings> {
  return putJson<MediaStorageSettings>('/api/web/admin/system/media-storage', settings);
}
