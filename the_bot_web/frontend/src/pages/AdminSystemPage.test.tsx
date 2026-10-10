import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as systemApi from '../api/adminSystem';
import { renderPage } from '../test/pageTestUtils';
import { AdminSystemPage } from './AdminSystemPage';

describe('AdminSystemPage', () => {
  it('selects llama.cpp, discovers models and saves without Ollama reasoning controls', async () => {
    const config: systemApi.HermesBackendConfigResponse = {
      systemMode: 'enabled',
      backends: [{
        id: 'local-0', label: 'Local', provider: 'ollama', baseUrl: 'http://192.168.0.60:8081/v1', model: 'swift-model',
        apiMode: 'chat-completions', timeoutSeconds: 30, contextWindow: 65536, healthy: true,
        toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
        apiKeyConfigured: false, reasoningDisabled: true,
      }],
      routes: [], profiles: [], fallback: null, globalOverride: null,
    };
    vi.spyOn(systemApi, 'getHermesBackendConfig').mockResolvedValue(config);
    const discover = vi.spyOn(systemApi, 'getHermesFallbackModels').mockResolvedValue({ models: ['swift-model'], items: [] });
    const save = vi.spyOn(systemApi, 'updateHermesBackendConfig').mockImplementation(async (value) => value);
    renderPage(<AdminSystemPage />);
    const user = userEvent.setup();
    await user.click(await screen.findByRole('textbox', { name: 'Provider' }));
    await user.keyboard('{ArrowDown}');
    await user.click(await screen.findByText('llama.cpp', { selector: '[role="option"] span' }));
    expect(screen.queryByRole('switch', { name: /Disable reasoning/ })).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'OpenAI-compatible base URL' })).toHaveValue('http://192.168.0.60:8081/v1');
    await user.click(screen.getByRole('button', { name: 'Load local models' }));
    await waitFor(() => expect(discover).toHaveBeenCalledWith('llamacpp', 'http://192.168.0.60:8081/v1', undefined, 'local-0'));
    await user.click(screen.getByRole('button', { name: 'Save and apply' }));
    await waitFor(() => expect(save).toHaveBeenCalledWith(expect.objectContaining({
      backends: [expect.objectContaining({ provider: 'llamacpp', model: 'swift-model', reasoningDisabled: false })],
    })));
  });
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
