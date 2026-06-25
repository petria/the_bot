import { postJson } from './client';

export interface ConsoleCommandRequest {
  command: string;
}

export interface ConsoleCommandResponse {
  username: string;
  reply: string | null;
}

export function executeConsoleCommand(command: string): Promise<ConsoleCommandResponse> {
  return postJson<ConsoleCommandResponse>('/api/web/cli/command', { command });
}
