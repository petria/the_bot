import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as urlsApi from '../api/adminCollectedUrls';
import type { CollectedUrlItem } from '../api/adminCollectedUrls';
import { renderPage } from '../test/pageTestUtils';
import { AdminCollectedUrlsPage } from './AdminCollectedUrlsPage';

const videoItem: CollectedUrlItem = {
  id: '1', shortCode: 'abc', url: 'https://example.test/video', provider: 'YouTube',
  title: 'Example video', author: 'Author', description: null, duration: null,
  publishedAt: null, viewCount: null, createdAt: null, expiresAt: null,
  sourceProtocol: 'IRC', sourceNetwork: 'IRCNet', sourceChannelAlias: 'IRC-TEST',
  sourceChannelName: '#test', sourceSender: 'Petri',
};

function mockUrls(items: CollectedUrlItem[]) {
  vi.spyOn(urlsApi, 'getCollectedUrls').mockResolvedValue({
    enabled: true,
    storageDir: '/media',
    detail: null,
    items,
  });
}

describe('AdminCollectedUrlsPage', () => {
  it('renders collected URL metadata', async () => {
    mockUrls([videoItem]);

    renderPage(<AdminCollectedUrlsPage />);

    expect(await screen.findByText('Collected URLs')).toBeInTheDocument();
    expect((await screen.findAllByText('Example video')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText('YouTube')).length).toBeGreaterThan(0);
  });

  it('stacks the filter fields into a mobile field row', async () => {
    mockUrls([]);
    const { container } = renderPage(<AdminCollectedUrlsPage />);

    await screen.findByText('Collected URLs');
    const fieldRow = container.querySelector('.field-row');
    expect(fieldRow).not.toBeNull();
    expect(fieldRow?.querySelector('.mantine-TextInput-root')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Provider' }).closest('.field-row')).toBe(fieldRow);
  });

  it('renders a card variant that repeats every table column', async () => {
    mockUrls([videoItem]);
    const { container } = renderPage(<AdminCollectedUrlsPage />);

    await screen.findAllByText('Example video');
    const table = container.querySelector('.data-table');
    const cards = container.querySelector('.data-cards');
    expect(table).toBeInTheDocument();
    expect(cards).toBeInTheDocument();

    const card = within(cards as HTMLElement).getByText('Example video').closest('.mantine-Card-root');
    expect(card).not.toBeNull();
    for (const label of ['Title', 'Source', 'Sender', 'Created', 'Expires', 'URL']) {
      expect(within(card as HTMLElement).getByText(label)).toBeInTheDocument();
    }
    expect(within(card as HTMLElement).getByText('YouTube')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('Petri')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('IRC-TEST')).toBeInTheDocument();
    const urlRow = within(card as HTMLElement).getByText('URL').closest('.info-row');
    expect(within(urlRow as HTMLElement).getByText('https://example.test/video')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('link', { name: 'Open original URL' }).closest('.action-row')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('button', { name: 'Copy original URL' })).toBeInTheDocument();
  });

  it('keeps a readable empty state in the card variant', async () => {
    mockUrls([]);
    const { container } = renderPage(<AdminCollectedUrlsPage />);

    const cards = container.querySelector('.data-cards');
    expect(within(cards as HTMLElement).getByText('No collected URLs found.')).toBeInTheDocument();
  });
});
