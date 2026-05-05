import { type KnownUserTarget } from './knownUsers';
import { postJson } from './client';

export type SendKnownUserRequest = {
  query: string;
  message: string;
  preferPrivate: boolean;
  connectionType?: string | null;
  echoToAlias?: string | null;
};

export type SendKnownUserResponse = {
  status: string | null;
  sentTo: string | null;
  message: string | null;
  selectedTarget: KnownUserTarget | null;
  candidateTargets: KnownUserTarget[] | null;
};

export type SendEchoToAliasRequest = {
  echoToAlias: string;
  message: string;
};

export type SendEchoToAliasResponse = {
  sentTo: string | null;
};

export type SendIrcPrivateRequest = {
  connectionId: number;
  nick: string;
  message: string;
};

export type SendIrcPrivateResponse = {
  status: string | null;
  message: string | null;
  sentTo: string | null;
};

export async function sendMessageToKnownUser(request: SendKnownUserRequest): Promise<SendKnownUserResponse> {
  return postJson<SendKnownUserResponse>('/api/web/messages/known-user', request);
}

export async function sendMessageByEchoToAlias(request: SendEchoToAliasRequest): Promise<SendEchoToAliasResponse> {
  return postJson<SendEchoToAliasResponse>('/api/web/messages/echo-to-alias', request);
}

export async function sendIrcPrivateMessage(request: SendIrcPrivateRequest): Promise<SendIrcPrivateResponse> {
  return postJson<SendIrcPrivateResponse>('/api/web/messages/irc-private', request);
}
