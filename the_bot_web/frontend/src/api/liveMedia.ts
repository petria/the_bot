import { getJson } from './client';

export type LiveMediaItem = {
  type: 'media' | 'url';
  id: string;
  shortCode: string | null;
  createdAt: string | null;
  expiresAt: string | null;
  sourceProtocol: string | null;
  sourceNetwork: string | null;
  sourceChannelAlias: string | null;
  sourceChannelName: string | null;
  sourceSender: string | null;
  contentType: string | null;
  mediaType: 'image' | 'video' | 'audio' | 'media' | null;
  originalFileName: string | null;
  sizeBytes: number | null;
  url: string | null;
  provider: string | null;
  title: string | null;
  author: string | null;
  description: string | null;
  duration: string | null;
  publishedAt: string | null;
  viewCount: number | null;
};

export type LiveMediaResponse = {
  enabled: boolean;
  storageDir: string | null;
  publicUrlPrefix: string | null;
  detail: string | null;
  items: LiveMediaItem[];
};

export function getLiveMedia(): Promise<LiveMediaResponse> {
  return getJson<LiveMediaResponse>('/api/web/live-media');
}
