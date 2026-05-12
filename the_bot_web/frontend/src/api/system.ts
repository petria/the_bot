import { getJson } from './client';

export type SystemComponentStatus = {
  name: string;
  status: string;
  componentType: string | null;
  baseUrl: string | null;
  profiles: string | null;
  version: string | null;
  artifact: string | null;
  uptimeSeconds: number | null;
  startedAt: string | null;
  receivedCalls: number | null;
  requestedCalls: number | null;
  responseTimeMs: number | null;
  checkedAt: string | null;
  containerName: string | null;
  containerState: string | null;
  containerStatusText: string | null;
  image: string | null;
  containerStartedAt: string | null;
  restartCount: number | null;
  containerError: string | null;
  error: string | null;
};

export type SystemStatusResponse = {
  checkedAt: string;
  components: SystemComponentStatus[];
};

export async function getSystemStatus(): Promise<SystemStatusResponse> {
  return getJson<SystemStatusResponse>('/api/web/system/status');
}
