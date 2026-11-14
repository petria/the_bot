import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as mediaApi from '../api/adminMediaContent';
import type { MediaContentItem } from '../api/adminMediaContent';
import { renderPage } from '../test/pageTestUtils';
import { AdminMediaContentPage } from './AdminMediaContentPage';

const imageItem: MediaContentItem = {
  id: '1', shortCode: 'abc', contentType: 'image/png', mediaType: 'image',
  originalFileName: 'photo.png', sizeBytes: 2048, createdAt: null, expiresAt: null,
  sourceProtocol: 'TELEGRAM', sourceNetwork: 'TelegramNetwork',
  sourceChannelAlias: 'TELEGRAM-TEST', sourceChannelName: 'Test group', sourceSender: 'Petri',
};

function mockMedia(items: MediaContentItem[]) {
  vi.spyOn(mediaApi, 'getMediaContent').mockResolvedValue({
    enabled: true,
    storageDir: '/media',
    publicUrlPrefix: '/media',
    detail: null,
    items,
  });
}

describe('AdminMediaContentPage', () => {
  it('renders stored media content and source details', async () => {
    mockMedia([imageItem]);

    renderPage(<AdminMediaContentPage />);

    expect(await screen.findByText('Media Content')).toBeInTheDocument();
    expect((await screen.findAllByText('photo.png')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('TELEGRAM-TEST')).length).toBeGreaterThan(0);
  });

  it('renders stored PDF documents', async () => {
    mockMedia([{
      id: '2', shortCode: 'pdf12', contentType: 'application/pdf', mediaType: 'document',
      originalFileName: 'document.pdf', sizeBytes: 2048, createdAt: null, expiresAt: null,
      sourceProtocol: 'WhatsApp', sourceNetwork: 'WhatsApp', sourceChannelAlias: 'WA-TEST',
      sourceChannelName: 'WhatsApp', sourceSender: 'Petri',
    }]);

    renderPage(<AdminMediaContentPage />);

    expect((await screen.findAllByText('document.pdf')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('document')).length).toBeGreaterThan(0);
  });

  it('stacks the filter field and type control into a mobile field row', async () => {
    mockMedia([]);
    const { container } = renderPage(<AdminMediaContentPage />);

    await screen.findByText('Media Content');
    const fieldRow = container.querySelector('.field-row');
    expect(fieldRow).not.toBeNull();
    expect(fieldRow?.querySelector('.mantine-SegmentedControl-root')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Filter' }).closest('.field-row')).toBe(fieldRow);
  });

  it('renders a card variant that repeats every table column', async () => {
    mockMedia([imageItem]);
    const { container } = renderPage(<AdminMediaContentPage />);

    await screen.findAllByText('photo.png');
    const table = container.querySelector('.data-table');
    const cards = container.querySelector('.data-cards');
    expect(table).toBeInTheDocument();
    expect(cards).toBeInTheDocument();

    const card = within(cards as HTMLElement).getByText('photo.png').closest('.mantine-Card-root');
    expect(card).not.toBeNull();
    for (const label of ['File', 'Source', 'Sender', 'Size', 'Created', 'Expires']) {
      expect(within(card as HTMLElement).getByText(label)).toBeInTheDocument();
    }
    expect(within(card as HTMLElement).getByText('2 KB')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('Petri')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('abc')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('link', { name: 'Open media' }).closest('.action-row')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('button', { name: 'Copy media link' })).toBeInTheDocument();
  });

  it('keeps a readable empty state in the card variant', async () => {
    mockMedia([]);
    const { container } = renderPage(<AdminMediaContentPage />);

    const cards = container.querySelector('.data-cards');
    expect(within(cards as HTMLElement).getByText('No media content found.')).toBeInTheDocument();
  });
});
