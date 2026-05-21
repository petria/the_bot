import { getJson } from './client';

export type CommandAliasInfo = {
  alias: string | null;
  target: string | null;
  withArgs: boolean;
};

export type CommandInfo = {
  commandName: string | null;
  displayName: string | null;
  trigger: string | null;
  className: string | null;
  requiredPermission: string | null;
  help: string | null;
  aliases: CommandAliasInfo[] | null;
};

export type CommandProviderInfo = {
  namespace: string | null;
  displayName: string | null;
  description: string | null;
  commandCount: number;
  commands: CommandInfo[] | null;
};

export type CommandsResponse = {
  providers: CommandProviderInfo[] | null;
};

export function getCommands(): Promise<CommandsResponse> {
  return getJson<CommandsResponse>('/api/web/commands');
}
