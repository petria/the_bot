import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderPage, installEventSourceMock } from '../test/pageTestUtils';
import { SystemPage } from './SystemPage';

describe('SystemPage', () => {
  it('opens the live system status view and exposes reconnect state', () => {
    installEventSourceMock();
    renderPage(<SystemPage />);

    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });
});
