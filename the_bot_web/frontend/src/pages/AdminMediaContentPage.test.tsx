import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as mediaApi from '../api/adminMediaContent';
import { renderPage } from '../test/pageTestUtils';
import { AdminMediaContentPage } from './AdminMediaContentPage';

describe('AdminMediaContentPage', () => {
  it('renders stored media content and source details', async () => {
    vi.spyOn(mediaApi, 'getMediaContent').mockResolvedValue({
      enabled: true, storageDir: '/media', publicUrlPrefix: '/media', detail: null,
      items: [{
        id: '1', shortCode: 'abc', contentType: 'image/png', mediaType: 'image',
        originalFileName: 'photo.png', sizeBytes: 2048, createdAt: null, expiresAt: null,
        sourceProtocol: 'TELEGRAM', sourceNetwork: 'TelegramNetwork',
        sourceChannelAlias: 'TELEGRAM-TEST', sourceChannelName: 'Test group', sourceSender: 'Petri',
      }],
    });

    renderPage(<AdminMediaContentPage />);

    expect(await screen.findByText('Media Content')).toBeInTheDocument();
    expect(await screen.findByText('photo.png')).toBeInTheDocument();
    expect(await screen.findByText('TELEGRAM-TEST')).toBeInTheDocument();
  });

  it('renders stored PDF documents', async () => {
    vi.spyOn(mediaApi, 'getMediaContent').mockResolvedValue({
      enabled: true,
      storageDir: '/media',
      publicUrlPrefix: '/media',
      detail: null,
      items: [{
        id: '2', shortCode: 'pdf12', contentType: 'application/pdf', mediaType: 'document',
        originalFileName: 'document.pdf', sizeBytes: 2048, createdAt: null, expiresAt: null,
        sourceProtocol: 'WhatsApp', sourceNetwork: 'WhatsApp', sourceChannelAlias: 'WA-TEST',
        sourceChannelName: 'WhatsApp', sourceSender: 'Petri',
      }],
    });

    renderPage(<AdminMediaContentPage />);

    expect(await screen.findByText('document.pdf')).toBeInTheDocument();
    expect(await screen.findByText('document')).toBeInTheDocument();
  });
});
