import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import * as generatedApi from '../api/generatedPages';
import { renderPage } from '../test/pageTestUtils';
import { GeneratedPage } from './GeneratedPage';

describe('GeneratedPage', () => {
  it('renders a generated GLUGGA counts page from its tokenized route', async () => {
    vi.spyOn(generatedApi, 'getGeneratedPage').mockResolvedValue({
      id: 'page-1', componentType: 'GluggaCountsPage', title: 'GLUGGA counts',
      createdAt: '2026-08-06T10:00:00Z', expiresAt: '2026-08-07T10:00:00Z',
      props: {
        channel: '#test', network: 'IRCNet', counterKey: 'GLUGGA_COUNT', counterName: 'GLUGGA',
        generatedAt: '2026-08-06T10:00:00Z', rowCount: 1,
        rows: [{ rank: 1, nick: 'petria', value: 3 }],
      },
    });

    renderPage(
      <Routes><Route path="/generated/:id" element={<GeneratedPage />} /></Routes>,
      ['/generated/page-1?token=test-token'],
    );

    expect(await screen.findByText('GLUGGA counts')).toBeInTheDocument();
    expect((await screen.findAllByText('petria')).length).toBeGreaterThan(0);
    expect(generatedApi.getGeneratedPage).toHaveBeenCalledWith('page-1', 'test-token');
  });
});
