import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as liveMediaApi from '../api/liveMedia';
import { renderPage } from '../test/pageTestUtils';
import { LiveMediaPage } from './LiveMediaPage';

describe('LiveMediaPage', () => {
  it('renders captured media with its source channel', async () => {
    vi.spyOn(liveMediaApi, 'getLiveMedia').mockResolvedValue({
      enabled: true,
      storageDir: '/media',
      publicUrlPrefix: '/media',
      detail: null,
      items: [{
        type: 'media', id: '1', shortCode: 'abc', createdAt: '2026-08-06T10:00:00Z',
        expiresAt: null, sourceProtocol: 'discord', sourceNetwork: 'Test',
        sourceChannelAlias: 'DISCORD-TEST', sourceChannelName: '#test', sourceSender: 'Petri',
        contentType: 'image/png', mediaType: 'image', originalFileName: 'chart.png',
        sizeBytes: 1024, url: '/m/abc', provider: null, title: null, author: null,
        description: null, duration: null, publishedAt: null, viewCount: null,
      }],
    });

    renderPage(<LiveMediaPage />);

    expect(await screen.findByText('Live Media')).toBeInTheDocument();
    expect(await screen.findByText('chart.png')).toBeInTheDocument();
    expect((await screen.findAllByText('DISCORD-TEST')).length).toBeGreaterThan(0);
  });

  it('renders captured documents', async () => {
    vi.spyOn(liveMediaApi, 'getLiveMedia').mockResolvedValue({
      enabled: true,
      storageDir: '/media',
      publicUrlPrefix: '/media',
      detail: null,
      items: [{
        type: 'media', id: '2', shortCode: 'pdf12', createdAt: '2026-08-06T10:00:00Z',
        expiresAt: null, sourceProtocol: 'WhatsApp', sourceNetwork: 'WhatsApp',
        sourceChannelAlias: 'WA-TEST', sourceChannelName: 'WhatsApp', sourceSender: 'Petri',
        contentType: 'application/pdf', mediaType: 'document', originalFileName: 'document.pdf',
        sizeBytes: 2048, url: '/m/pdf12', provider: null, title: null, author: null,
        description: null, duration: null, publishedAt: null, viewCount: null,
      }],
    });

    renderPage(<LiveMediaPage />);

    expect(await screen.findByText('document.pdf')).toBeInTheDocument();
    expect(await screen.findByText('document')).toBeInTheDocument();
  });
});
