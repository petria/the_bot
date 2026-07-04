import { getJson, postJson, putJson } from './client';

export type AdminConfigChannel = {
  id: string | null;
  description: string | null;
  name: string | null;
  type: string | null;
  echoToAlias: string | null;
  echoToAliases: string[] | null;
  joinOnStart: boolean;
  publicAiEnabled: boolean;
  allowAnonymousAiCommands: boolean;
  resolveUrls: boolean;
  alertMessages: boolean;
  captureImages: boolean;
  captureImageToAliases: string[] | null;
};

export type AdminIrcServerConfig = {
  name: string | null;
  connectStartup: boolean;
  networkName: string | null;
  host: string | null;
  port: number;
  channelList: AdminConfigChannel[] | null;
};

export type AdminBotConfig = {
  botName: string | null;
  ircRealName: string | null;
};

export type AdminDiscordConfig = {
  connectStartup: boolean;
  theBotUserId: string | null;
  channelList: AdminConfigChannel[] | null;
};

export type AdminTelegramConfig = {
  telegramName: string | null;
  connectStartup: boolean;
  channelList: AdminConfigChannel[] | null;
};

export type AdminWhatsAppConfig = {
  network: string | null;
  sendBaseUrl: string | null;
  connectStartup: boolean;
  channelList: AdminConfigChannel[] | null;
};

export type AdminConnectionConfigPayload = {
  botConfig: AdminBotConfig | null;
  ircServerConfigs: AdminIrcServerConfig[] | null;
  discordConfig: AdminDiscordConfig | null;
  telegramConfig: AdminTelegramConfig | null;
  whatsappConfig: AdminWhatsAppConfig | null;
};

export type AdminConnectionConfigResponse = {
  profile: string | null;
  configFile: string;
  lastModifiedAt: string;
  config: AdminConnectionConfigPayload;
};

export type AdminConnectionConfigApplyTarget = {
  target: string;
  status: string;
  message: string | null;
};

export type AdminConnectionConfigApplyResponse = {
  status: string;
  savedConfig: AdminConnectionConfigResponse;
  targets: AdminConnectionConfigApplyTarget[];
};

export type PromoteChannelState = {
  channel: AdminConfigChannel;
  connectionType: string | null;
  network: string | null;
};

export function getAdminConnectionConfig(): Promise<AdminConnectionConfigResponse> {
  return getJson<AdminConnectionConfigResponse>('/api/web/admin/config/connections');
}

export function saveAdminConnectionConfig(
  config: AdminConnectionConfigPayload,
): Promise<AdminConnectionConfigResponse> {
  return putJson<AdminConnectionConfigResponse>('/api/web/admin/config/connections', config);
}

export function saveAndApplyAdminConnectionConfig(
  config: AdminConnectionConfigPayload,
): Promise<AdminConnectionConfigApplyResponse> {
  return postJson<AdminConnectionConfigApplyResponse>('/api/web/admin/config/connections/apply', config);
}
