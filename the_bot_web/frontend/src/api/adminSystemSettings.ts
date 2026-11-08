import { getJson, putJson } from './client';

export type SystemSettings = { waterTemperatureToWatch: boolean };
const endpoint = '/api/web/admin/system/settings';
export const getSystemSettings = () => getJson<SystemSettings>(endpoint);
export const saveSystemSettings = (settings: SystemSettings) => putJson<SystemSettings>(endpoint, settings);
