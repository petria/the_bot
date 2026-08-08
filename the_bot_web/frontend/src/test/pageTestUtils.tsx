import { MantineProvider } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderResult } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactElement } from 'react';

export function renderPage(element: ReactElement, initialEntries: string[] = ['/']): RenderResult {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <QueryClientProvider client={queryClient}>
        <MantineProvider>{element}</MantineProvider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

export function installEventSourceMock() {
  class TestEventSource {
    onopen: (() => void) | null = null;
    onerror: (() => void) | null = null;
    addEventListener() {
      return undefined;
    }
    close() {
      return undefined;
    }
  }

  Object.defineProperty(window, 'EventSource', {
    configurable: true,
    writable: true,
    value: TestEventSource,
  });
}
