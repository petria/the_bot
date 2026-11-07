import { fireEvent, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import * as meApi from '../api/me';
import * as api from '../api/adminSystemSettings';
import { renderPage } from '../test/pageTestUtils';
import { AdminManageSystemPage } from './AdminManageSystemPage';

describe('Manage System', () => {
  it.each([true, false])('protects navigation and direct route for admin=%s', async (admin) => {
    vi.spyOn(meApi, 'getMe').mockResolvedValue({ id: 1, username: 'tester', name: null,
      email: null, ircNick: null, telegramId: null, discordId: null, whatsappId: null,
      homeChannel: null, roles: [], permissions: [admin ? 'web.admin' : 'web.user'] });
    const get = vi.spyOn(api, 'getSystemSettings').mockResolvedValue({ waterTemperatureToWatch: false });
    renderPage(<App />, ['/admin/manage-system']);
    if (admin) {
      expect(await screen.findByRole('heading', { name: 'Manage System' })).toBeInTheDocument();
      expect(screen.getByText('Manage AI Routes')).toBeInTheDocument();
      expect(screen.getAllByText('Manage System')).toHaveLength(2);
    } else {
      expect(await screen.findByText('Admin access required')).toBeInTheDocument();
      expect(screen.queryByText('Manage System')).not.toBeInTheDocument();
      expect(get).not.toHaveBeenCalled();
    }
  });
  it.each([false, true])('loads saved state %s and saves its opposite', async (enabled) => {
    vi.spyOn(api, 'getSystemSettings').mockResolvedValue({ waterTemperatureToWatch: enabled });
    const save = vi.spyOn(api, 'saveSystemSettings').mockResolvedValue({ waterTemperatureToWatch: !enabled });
    renderPage(<AdminManageSystemPage />);
    expect(screen.getByRole('heading', { name: 'Manage System' })).toBeInTheDocument();
    const toggle = await screen.findByRole('switch', { name: 'Water temperature to watch' });
    expect(toggle).toHaveProperty('checked', enabled);
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    fireEvent.click(toggle);
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(save).toHaveBeenCalledWith({ waterTemperatureToWatch: !enabled }));
    expect(await screen.findByText('System settings saved.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
  });

  it('does not permit saving when settings cannot be loaded', async () => {
    vi.spyOn(api, 'getSystemSettings').mockRejectedValue(new Error('Engine unavailable'));
    renderPage(<AdminManageSystemPage />);
    expect(await screen.findByText('Engine unavailable')).toBeInTheDocument();
    expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
  });

  it('retains edits and allows retry after a failed save', async () => {
    vi.spyOn(api, 'getSystemSettings').mockResolvedValue({ waterTemperatureToWatch: false });
    vi.spyOn(api, 'saveSystemSettings').mockRejectedValue(new Error('Save failed'));
    renderPage(<AdminManageSystemPage />);
    fireEvent.click(await screen.findByRole('switch'));
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Save failed')).toBeInTheDocument();
    expect(screen.getByRole('switch')).toBeChecked();
    expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();
    expect(screen.queryByText('System settings saved.')).not.toBeInTheDocument();
  });
});
