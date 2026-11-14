import { act, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderPage, installEventSourceMock, type FakeEventSource } from '../test/pageTestUtils';
import { SystemPage } from './SystemPage';
import type { SystemStatusResponse } from '../api/system';

const status: SystemStatusResponse = {
  checkedAt: '2026-11-14T08:00:00Z',
  components: [{
    name: 'bot-io',
    status: 'UP',
    componentType: 'SPRING_BOOT',
    runtimeMode: 'docker',
    healthUrl: 'http://bot-io:8091/actuator/health',
    healthStatus: 'UP',
    baseUrl: 'http://bot-io:8091',
    profiles: 'prod',
    version: '0.9.4',
    artifact: 'bot-io.jar',
    uptimeSeconds: 7200,
    startedAt: '2026-11-14T06:00:00Z',
    receivedCalls: 128,
    requestedCalls: 64,
    responseTimeMs: 12,
    checkedAt: '2026-11-14T08:00:00Z',
    containerName: 'the_bot_bot_io',
    containerState: 'running',
    containerStatusText: 'Up 2 hours',
    image: 'the_bot/bot-io:latest',
    containerStartedAt: '2026-11-14T06:00:00Z',
    restartCount: 0,
    containerError: null,
    error: null,
  }],
};

describe('SystemPage', () => {
  it('opens the live system status view and exposes reconnect state', () => {
    installEventSourceMock();
    renderPage(<SystemPage />);

    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });

  it('marks every component detail row for mobile stacking', async () => {
    const sources = installEventSourceMock();
    const { container } = renderPage(<SystemPage />);
    act(() => sources[0].emit('status', status));

    expect(await screen.findByText('bot-io')).toBeInTheDocument();

    const infoRows = container.querySelectorAll('.info-row');
    expect(infoRows.length).toBeGreaterThanOrEqual(10);
    expect(screen.getByText('Spring Boot').closest('.info-row')).toBeInTheDocument();
    expect(screen.getByText('the_bot_bot_io').closest('.info-row')).toBeInTheDocument();
    expect(container.querySelector('.system-info-line')).toBeInTheDocument();
    expect(sources[0].url).toContain('/api/web/system/status/stream');
  });
});
