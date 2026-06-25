import { getJson, postJson } from './client';

export interface ConsoleCommandRequest {
  sessionId: string;
  command: string;
}

export interface ConsoleCommandResponse {
  requestId: number;
  accepted: boolean;
}

export interface ConsoleEvent {
  id: number;
  requestId: number;
  createdAt: number;
  message: string;
}

interface ConsoleEventsResponse {
  events: ConsoleEvent[] | null;
}

export function executeConsoleCommand(sessionId: string, command: string): Promise<ConsoleCommandResponse> {
  return postJson<ConsoleCommandResponse>('/api/web/console/command', { sessionId, command });
}

export async function getConsoleEvents(sessionId: string, afterId: number): Promise<ConsoleEvent[]> {
  const params = new URLSearchParams({
    sessionId,
    afterId: `${afterId}`,
  });
  const response = await getJson<ConsoleEventsResponse>(`/api/web/console/events?${params.toString()}`);
  return response.events ?? [];
}
