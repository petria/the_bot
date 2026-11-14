import { getJson, putJson } from './client';

export type WebUiLink = { url: string; configured: boolean };

export type WebUiLinksSettings = { main: WebUiLink; develop: WebUiLink };

export type WebUiLinksUpdate = { mainUrl: string; developUrl: string };

export const DEFAULT_MAIN_URL = 'https://hokanthebot.airiot.fi';
export const DEFAULT_DEVELOP_URL = 'https://hokandevbot.airiot.fi';

const endpoint = '/api/web/admin/system/webui-links';

export const getWebUiLinks = () => getJson<WebUiLinksSettings>(endpoint);
export const saveWebUiLinks = (update: WebUiLinksUpdate) => putJson<WebUiLinksSettings>(endpoint, update);
