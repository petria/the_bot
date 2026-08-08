import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import * as systemApi from '../api/adminSystem';
import { renderPage } from '../test/pageTestUtils';
import { AdminSystemPage } from './AdminSystemPage';

describe('AdminSystemPage', () => {
  it('renders configured AI backends and routes', async () => {
    vi.spyOn(systemApi, 'getHermesBackendConfig').mockResolvedValue({
      systemMode: 'enabled',
      backends: [
        {
          id: 'openai', label: 'OpenAI', provider: 'openai', baseUrl: null, model: 'gpt-5.5',
          apiMode: 'responses', timeoutSeconds: 30, contextWindow: 100000, healthy: true,
          toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
          apiKeyConfigured: true, reasoningDisabled: false,
        },
        {
          id: 'local-0', label: 'Local', provider: 'ollama', baseUrl: 'http://ollama:11434', model: 'qwen',
          apiMode: 'chat-completions', timeoutSeconds: 30, contextWindow: 8192, healthy: true,
          toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
          apiKeyConfigured: false, reasoningDisabled: true,
        },
      ],
      routes: [{
        id: 'chat', label: 'Chat', backendId: 'openai', provider: 'openai', baseUrl: null,
        model: 'gpt-5.5', apiMode: 'responses', timeoutSeconds: 30, contextWindow: 100000,
        healthy: true, toolCapable: true, detail: null,
      }],
      profiles: [], fallback: null, globalOverride: null,
    });

    renderPage(<AdminSystemPage />);

    expect(await screen.findByText('Manage AI Routes')).toBeInTheDocument();
    expect(await screen.findByText('OpenAI backend')).toBeInTheDocument();
    expect(screen.getByText('Route selection')).toBeInTheDocument();
  });
});
