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

export type FakeEventSource = {
  url: string;
  emit(type: string, data: unknown): void;
  close(): void;
};

type EventListener = (event: { data: string }) => void;

type RecordedSource = {
  url: string;
  listeners: Map<string, EventListener[]>;
  onmessage: EventListener | null;
  close(): void;
};

/** Installs a window.EventSource stub and returns a handle for every source a page opens. */
export function installEventSourceMock(): FakeEventSource[] {
  const handles: FakeEventSource[] = [];

  class TestEventSource {
    static readonly CONNECTING = 0;
    static readonly OPEN = 1;
    static readonly CLOSED = 2;

    onopen: (() => void) | null = null;
    onerror: (() => void) | null = null;
    onmessage: EventListener | null = null;
    readyState = TestEventSource.OPEN;
    readonly url: string;
    readonly listeners = new Map<string, EventListener[]>();

    constructor(url: string) {
      this.url = url;
      const source = this as unknown as RecordedSource;
      handles.push({
        url,
        emit(type, data) {
          const event = { data: typeof data === 'string' ? data : JSON.stringify(data) };
          if (type === 'message') {
            source.onmessage?.(event);
            return;
          }
          (source.listeners.get(type) ?? []).forEach((listener) => listener(event));
        },
        close() {
          source.close();
        },
      });
    }

    addEventListener(type: string, listener: EventListener) {
      this.listeners.set(type, [...(this.listeners.get(type) ?? []), listener]);
    }

    removeEventListener(type: string, listener: EventListener) {
      this.listeners.set(type, (this.listeners.get(type) ?? []).filter((candidate) => candidate !== listener));
    }

    close() {
      this.readyState = TestEventSource.CLOSED;
      return undefined;
    }
  }

  Object.defineProperty(window, 'EventSource', {
    configurable: true,
    writable: true,
    value: TestEventSource,
  });

  return handles;
}
