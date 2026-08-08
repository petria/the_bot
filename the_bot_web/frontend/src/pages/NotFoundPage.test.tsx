import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderPage } from '../test/pageTestUtils';
import { NotFoundPage } from './NotFoundPage';

describe('NotFoundPage', () => {
  it('shows a link back to the application', () => {
    renderPage(<NotFoundPage />, ['/missing']);

    expect(screen.getByText('Page not found')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /back to system/i })).toHaveAttribute('href', '/');
  });
});
