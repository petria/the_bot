import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as storageApi from '../api/adminMediaStorage';
import { renderPage } from '../test/pageTestUtils';
import { AdminMediaStoragePage } from './AdminMediaStoragePage';

describe('AdminMediaStoragePage', () => {
  it('loads settings and saves an edited storage directory', async () => {
    const settings = {
      enabled: true, storageDir: '/media', publicUrlPrefix: '/m', maxFileSizeMb: 25,
      retentionDays: 30, directoryExists: true, writable: true, detail: null,
    };
    vi.spyOn(storageApi, 'getMediaStorageSettings').mockResolvedValue(settings);
    const save = vi.spyOn(storageApi, 'updateMediaStorageSettings').mockResolvedValue(settings);
    const user = userEvent.setup();
    renderPage(<AdminMediaStoragePage />);

    const input = await screen.findByDisplayValue('/media');
    await user.clear(input);
    await user.type(input, '/mnt/storagebox/media');
    await user.click(screen.getByRole('button', { name: /save/i }));

    expect(save).toHaveBeenCalledWith(expect.objectContaining({ storageDir: '/mnt/storagebox/media' }));
  });
});
