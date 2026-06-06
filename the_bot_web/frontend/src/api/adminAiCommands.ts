import { getJson, putJson } from './client';

export type AiCommandDefinition = {
  name: string;
  enabled: boolean;
  description: string | null;
  usage: string | null;
  aliases: string[];
  requiredPermission: string | null;
  instructions: string | null;
  allowedTools: string[];
  maxToolIterations: number;
};

export type AiCommandConfig = {
  commands: AiCommandDefinition[];
};

export type AiCommandConfigResponse = {
  path: string;
  config: AiCommandConfig;
  availableTools: string[];
};

export function getAiCommands(): Promise<AiCommandConfigResponse> {
  return getJson<AiCommandConfigResponse>('/api/web/admin/ai-commands');
}

export function saveAiCommands(config: AiCommandConfig): Promise<AiCommandConfigResponse> {
  return putJson<AiCommandConfigResponse>('/api/web/admin/ai-commands', config);
}
