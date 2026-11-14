import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as urlsApi from '../api/adminCollectedUrls';
import { renderPage } from '../test/pageTestUtils';
import { AdminCollectedUrlsPage } from './AdminCollectedUrlsPage';

describe('AdminCollectedUrlsPage', () => {
  it('renders collected URL metadata', async () => {
    vi.spyOn(urlsApi, 'getCollectedUrls').mockResolvedValue({
      enabled: true, storageDir: '/media', detail: null,
      items: [{
        id: '1', shortCode: 'abc', url: 'https://example.test/video', provider: 'YouTube',
        title: 'Example video', author: 'Author', description: null, duration: null,
        publishedAt: null, viewCount: null, createdAt: null, expiresAt: null,
        sourceProtocol: 'IRC', sourceNetwork: 'IRCNet', sourceChannelAlias: 'IRC-TEST',
        sourceChannelName: '#test', sourceSender: 'Petri',
      }],
    });

    renderPage(<AdminCollectedUrlsPage />);

    expect(await screen.findByText('Collected URLs')).toBeInTheDocument();
    expect(await screen.findByText('Example video')).toBeInTheDocument();
    expect(await screen.findAllByText('YouTube')).not.toHaveLength(0);
  });

  it('stacks the filter fields into a mobile field row', async () => {
    vi.spyOn(urlsApi, 'getCollectedUrls').mockResolvedValue({
      enabled: true, storageDir: '/media', detail: null, items: [],
    });
    const { container } = renderPage(<AdminCollectedUrlsPage />);

    await screen.findByText('Collected URLs');
    const fieldRow = container.querySelector('.field-row');
    expect(fieldRow).not.toBeNull();
    expect(fieldRow?.querySelector('.mantine-TextInput-root')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Provider' }).closest('.field-row')).toBe(fieldRow);
  });

});
