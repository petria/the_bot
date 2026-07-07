import { getJson } from './client';

export type CollectedUrlItem = {
  id: string;
  shortCode: string;
  url: string;
  provider: string | null;
  title: string;
  author: string | null;
  description: string | null;
  duration: string | null;
  publishedAt: string | null;
  viewCount: number | null;
  createdAt: string | null;
  expiresAt: string | null;
  sourceProtocol: string | null;
  sourceNetwork: string | null;
  sourceChannelAlias: string | null;
  sourceChannelName: string | null;
  sourceSender: string | null;
};

export type CollectedUrlsResponse = {
  enabled: boolean;
  storageDir: string | null;
  detail: string | null;
  items: CollectedUrlItem[];
};

export function getCollectedUrls(): Promise<CollectedUrlsResponse> {
  return getJson<CollectedUrlsResponse>('/api/web/admin/collected-urls');
}
