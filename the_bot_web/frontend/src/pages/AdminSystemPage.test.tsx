import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
          id: 'openai', label: 'OpenAI', provider: 'openai', baseUrl: null, model: 'gpt-5.6-luna',
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
        model: 'gpt-5.6-luna', apiMode: 'responses', timeoutSeconds: 30, contextWindow: 100000,
        healthy: true, toolCapable: true, detail: null,
      }],
      profiles: [], fallback: null, globalOverride: null,
    });

    renderPage(<AdminSystemPage />);

    expect(await screen.findByText('Manage AI Routes')).toBeInTheDocument();
    expect(await screen.findByText('OpenAI backend')).toBeInTheDocument();
    expect(screen.getByText('Route selection')).toBeInTheDocument();

    const user = userEvent.setup();
    await user.click(screen.getAllByRole('textbox', { name: 'Model' })[0]);
    expect(screen.getByText('gpt-5.6-luna')).toBeInTheDocument();
    expect(screen.getByText('gpt-5.6-terra')).toBeInTheDocument();
    expect(screen.getByText('gpt-5.6-sol')).toBeInTheDocument();
    expect(screen.queryByText('gpt-5.5')).not.toBeInTheDocument();
  });

  it('shows an unsupported saved OpenAI model as invalid', async () => {
    vi.spyOn(systemApi, 'getHermesBackendConfig').mockResolvedValue({
      systemMode: 'enabled',
      backends: [{
        id: 'openai', label: 'OpenAI', provider: 'openai', baseUrl: null, model: 'gpt-5.5',
        apiMode: 'responses', timeoutSeconds: 30, contextWindow: null, healthy: true,
        toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
        apiKeyConfigured: true, reasoningDisabled: false,
      }, {
        id: 'local-0', label: 'Local', provider: 'ollama', baseUrl: 'http://ollama:11434', model: 'qwen',
        apiMode: 'chat-completions', timeoutSeconds: 30, contextWindow: 8192, healthy: true,
        toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
        apiKeyConfigured: false, reasoningDisabled: true,
      }],
      routes: [], profiles: [], fallback: null, globalOverride: null,
    });

    renderPage(<AdminSystemPage />);

    expect(await screen.findByText('Saved OpenAI model "gpt-5.5" is not supported. Choose one of the available gpt-5.6 models.')).toBeInTheDocument();
    expect(screen.getByText('The saved OpenAI model is no longer supported. Select gpt-5.6-luna, gpt-5.6-terra, or gpt-5.6-sol before saving route changes.')).toBeInTheDocument();
  });
});
