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
});
