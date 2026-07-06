import { getJson } from './client';

export type MediaContentItem = {
  id: string;
  shortCode: string;
  contentType: string;
  mediaType: 'image' | 'video' | 'audio' | 'media';
  originalFileName: string;
  sizeBytes: number;
  createdAt: string | null;
  expiresAt: string | null;
  sourceProtocol: string | null;
  sourceNetwork: string | null;
  sourceChannelAlias: string | null;
  sourceChannelName: string | null;
  sourceSender: string | null;
};

export type MediaContentResponse = {
  enabled: boolean;
  storageDir: string | null;
  publicUrlPrefix: string | null;
  detail: string | null;
  items: MediaContentItem[];
};

export function getMediaContent(): Promise<MediaContentResponse> {
  return getJson<MediaContentResponse>('/api/web/admin/media-content');
}
