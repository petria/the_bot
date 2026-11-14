import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as liveMediaApi from '../api/liveMedia';
import type { LiveMediaItem } from '../api/liveMedia';
import { renderPage } from '../test/pageTestUtils';
import { LiveMediaPage } from './LiveMediaPage';

const imageItem: LiveMediaItem = {
  type: 'media', id: '1', shortCode: 'abc', createdAt: '2026-08-06T10:00:00Z',
  expiresAt: null, sourceProtocol: 'discord', sourceNetwork: 'Test',
  sourceChannelAlias: 'DISCORD-TEST', sourceChannelName: '#test', sourceSender: 'Petri',
  contentType: 'image/png', mediaType: 'image', originalFileName: 'chart.png',
  sizeBytes: 1024, url: '/m/abc', provider: null, title: null, author: null,
  description: null, duration: null, publishedAt: null, viewCount: null,
};

function mockMedia(items: LiveMediaItem[]) {
  vi.spyOn(liveMediaApi, 'getLiveMedia').mockResolvedValue({
    enabled: true,
    storageDir: '/media',
    publicUrlPrefix: '/media',
    detail: null,
    items,
  });
}

describe('LiveMediaPage', () => {
  it('renders captured media with its source channel', async () => {
    mockMedia([imageItem]);

    renderPage(<LiveMediaPage />);

    expect(await screen.findByText('Live Media')).toBeInTheDocument();
    expect((await screen.findAllByText('chart.png')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('DISCORD-TEST')).length).toBeGreaterThan(0);
  });

  it('renders captured documents', async () => {
    mockMedia([{ ...imageItem, id: '2', shortCode: 'pdf12', contentType: 'application/pdf', mediaType: 'document', originalFileName: 'document.pdf', sizeBytes: 2048, url: '/m/pdf12' }]);

    renderPage(<LiveMediaPage />);

    expect((await screen.findAllByText('document.pdf')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('document')).length).toBeGreaterThan(0);
  });

  it('stacks the filter fields into a mobile field row', async () => {
    mockMedia([]);
    const { container } = renderPage(<LiveMediaPage />);

    await screen.findByText('Live Media');
    const fieldRow = container.querySelector('.field-row');
    expect(fieldRow).not.toBeNull();
    expect(fieldRow?.querySelectorAll('.mantine-TextInput-root')).toHaveLength(1);
    expect(fieldRow?.querySelectorAll('.mantine-Select-root')).toHaveLength(2);
    expect(screen.getByRole('textbox', { name: 'Filter' }).closest('.field-row')).toBe(fieldRow);
  });

  it('renders a card variant that repeats every table column', async () => {
    mockMedia([imageItem]);
    const { container } = renderPage(<LiveMediaPage />);

    await screen.findAllByText('chart.png');
    const table = container.querySelector('.data-table');
    const cards = container.querySelector('.data-cards');
    expect(table).toBeInTheDocument();
    expect(cards).toBeInTheDocument();

    const card = within(cards as HTMLElement).getByText('chart.png').closest('.mantine-Card-root');
    expect(card).not.toBeNull();
    for (const label of ['Content', 'Source', 'Sender', 'Created', 'Expires']) {
      expect(within(card as HTMLElement).getByText(label)).toBeInTheDocument();
    }
    expect(within(card as HTMLElement).getByText('Petri')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('DISCORD-TEST')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('image')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('link', { name: 'Open item' })).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('button', { name: 'Copy link' }).closest('.action-row')).toBeInTheDocument();
  });

  it('keeps a readable empty state in the card variant', async () => {
    mockMedia([]);
    const { container } = renderPage(<LiveMediaPage />);

    const cards = container.querySelector('.data-cards');
    expect(cards).toBeInTheDocument();
    expect(within(cards as HTMLElement).getByText('No live media found.')).toBeInTheDocument();
  });
});
