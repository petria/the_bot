import { getJson } from './client';

export interface GeneratedPageResponse {
  id: string;
  componentType: string;
  title: string;
  createdAt: string;
  expiresAt: string;
  props: Record<string, unknown>;
}

export function getGeneratedPage(id: string, token: string) {
  return getJson<GeneratedPageResponse>(
    `/api/web/generated-pages/${encodeURIComponent(id)}?token=${encodeURIComponent(token)}`,
  );
}
