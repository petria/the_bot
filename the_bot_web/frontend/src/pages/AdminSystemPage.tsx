import { Alert, Badge, Button, Card, Group, Loader, NumberInput, Select, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Plus, RefreshCw, Save, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getHermesBackendConfig,
  getHermesFallbackModels,
  updateHermesBackendConfig,
  type HermesBackend,
  type HermesBackendConfigResponse,
  type HermesFallbackModel,
} from '../api/adminSystem';

const LOCAL_RUNNER_OPTIONS = [
  { value: 'ollama', label: 'Ollama' },
  { value: 'lmstudio', label: 'LM Studio' },
  { value: 'vllm', label: 'vLLM' },
];

const OPENAI_MODEL_ITEMS: HermesFallbackModel[] = [
  { id: 'gpt-5.5', suitability: 'tool-capable', label: 'OpenAI/Codex model', toolCapable: true, detail: 'Supported Codex model' },
  { id: 'gpt-5.4', suitability: 'tool-capable', label: 'OpenAI/Codex model', toolCapable: true, detail: 'Supported Codex model' },
  { id: 'gpt-5.4-mini', suitability: 'tool-capable', label: 'OpenAI/Codex model', toolCapable: true, detail: 'Supported Codex model' },
];

const OPENAI_MODEL_OPTIONS = OPENAI_MODEL_ITEMS.map((model) => model.id);

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const backendQuery = useQuery({
    queryKey: ['admin-system-hermes-backends'],
    queryFn: getHermesBackendConfig,
  });
  const [config, setConfig] = useState<HermesBackendConfigResponse | null>(null);
  const [modelBackendId, setModelBackendId] = useState<string>('local-0');
  const [loadedModelBackendId, setLoadedModelBackendId] = useState<string>('');
  const [models, setModels] = useState<string[]>([]);
  const [modelItems, setModelItems] = useState<HermesFallbackModel[]>([]);
  const [applyFailed, setApplyFailed] = useState(false);

  useEffect(() => {
    if (backendQuery.data) {
      setConfig(backendQuery.data);
      setModelBackendId(firstLocalBackend(backendQuery.data)?.id || 'local-0');
    }
  }, [backendQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (next: HermesBackendConfigResponse) => updateHermesBackendConfig(next),
    onSuccess: (response) => {
      setApplyFailed(false);
      setConfig(response);
      queryClient.setQueryData(['admin-system-hermes-backends'], response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
    onError: () => {
      setApplyFailed(true);
      queryClient.invalidateQueries({ queryKey: ['admin-system-hermes-backends'] });
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });

  const modelMutation = useMutation({
    mutationFn: (backend: HermesBackend) =>
      getHermesFallbackModels(backend.provider, backend.baseUrl || '', backend.apiKey, backend.id),
    onSuccess: (response, backend) => {
      setModels(response.models || []);
      setModelItems(response.items?.length ? response.items : (response.models || []).map((model) => ({
        id: model,
        suitability: 'unknown',
        label: 'tool support unknown',
        toolCapable: null,
        detail: null,
      })));
      setLoadedModelBackendId(backend.id);
    },
  });

  const openAi = backendById(config, 'openai');
  const localBackends = config?.backends.filter((backend) => isLocalBackend(backend)) || [];
  const selectedModelBackend = backendById(config, modelBackendId);
  const activeModelItems = selectedModelBackend?.id === 'openai' ? OPENAI_MODEL_ITEMS : modelItems;
  const activeModels = activeModelItems.map((model) => model.id);
  const selectedModel = activeModelItems.find((item) => item.id === selectedModelBackend?.model) || null;
  const hasChanges = Boolean(config && JSON.stringify(config) !== JSON.stringify(backendQuery.data));
  const discoveredModelOptions = useMemo(
    () => activeModelItems.map((model) => ({ value: model.id, label: `${model.id} - ${model.label}` })),
    [activeModelItems]
  );
  const routeOptions = useMemo(
    () => config?.backends.map((backend) => ({ value: backend.id, label: backend.label })) || [],
    [config]
  );

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage AI Routes</Title>
          <Text c="dimmed">Global AI mode, fixed Hermes backends, and route selection.</Text>
        </div>
        <Button
          leftSection={<Save size={18} />}
          loading={saveMutation.isPending}
          disabled={!config || !hasChanges}
          onClick={() => config && saveMutation.mutate(config)}
        >
          Save and apply
        </Button>
      </Group>

      {backendQuery.isLoading ? <Loader /> : null}
      {backendQuery.isError ? <SettingsError error={backendQuery.error} /> : null}
      {saveMutation.isError ? <SettingsError error={saveMutation.error} /> : null}
      {modelMutation.isError ? <SettingsError error={modelMutation.error} /> : null}
      {applyFailed ? (
        <Alert color="yellow" variant="light" icon={<AlertCircle size={18} />}>
          Route changes were not applied. The values shown here are refreshed from saved Hermes manager state.
        </Alert>
      ) : null}

      {config ? (
        <Stack gap="md">
          <Card withBorder radius="sm">
            <Group justify="space-between" align="center">
              <Stack gap={2}>
                <Text fw={700}>AI system mode</Text>
                <Text size="sm" c="dimmed">When off, AI commands return "AI not available".</Text>
              </Stack>
              <Switch
                label={config.systemMode === 'off' ? 'Off' : 'Enabled'}
                checked={config.systemMode !== 'off'}
                onChange={(event) => updateConfig({ systemMode: event.currentTarget.checked ? 'enabled' : 'off' })}
              />
            </Group>
          </Card>

          {openAi ? (
            <BackendCard
              title="OpenAI backend"
              backend={openAi}
              modelOptions={OPENAI_MODEL_OPTIONS}
              disabledProvider
              onChange={(patch) => updateBackend('openai', patch)}
            />
          ) : null}

          {localBackends.map((backend) => (
            <BackendCard
              key={backend.id}
              title={backend.label}
              backend={backend}
              modelOptions={loadedModelBackendId === backend.id ? models : []}
              canDelete={localBackends.length > 1 && !routeUsesBackend(backend.id)}
              onDelete={() => deleteLocalBackend(backend.id)}
              onChange={(patch) => updateBackend(backend.id, patch)}
            />
          ))}

          <Button
            variant="light"
            leftSection={<Plus size={18} />}
            onClick={addLocalBackend}
          >
            Add local backend
          </Button>

          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-end">
                <Select
                  label="Model list source"
                  data={config.backends.map((backend) => ({ value: backend.id, label: backend.label }))}
                  value={modelBackendId}
                  onChange={(value) => setModelBackendId(value || firstLocalBackend(config)?.id || 'local-0')}
                />
                <Button
                  variant="light"
                  leftSection={<RefreshCw size={18} />}
                  loading={modelMutation.isPending}
                  disabled={!selectedModelBackend || selectedModelBackend.provider === 'openai' || !selectedModelBackend.baseUrl}
                  onClick={() => selectedModelBackend && modelMutation.mutate(selectedModelBackend)}
                >
                  Load local models
                </Button>
              </Group>
              <Select
                label="Apply discovered model"
                data={discoveredModelOptions}
                value={selectedModelBackend && activeModels.includes(selectedModelBackend.model) ? selectedModelBackend.model : null}
                placeholder={selectedModelBackend?.id === 'openai'
                  ? 'Select Codex model'
                  : loadedModelBackendId === selectedModelBackend?.id ? `${models.length} models loaded` : 'Load models first'}
                disabled={!selectedModelBackend || (selectedModelBackend.id !== 'openai' && (loadedModelBackendId !== selectedModelBackend.id || models.length === 0))}
                searchable
                onChange={(value) => selectedModelBackend && value && updateBackend(selectedModelBackend.id, { model: value })}
              />
              {selectedModel ? (
                <InfoLine label="Selected model" value={`${selectedModel.label}; tools=${statusText(selectedModel.toolCapable)}`} />
              ) : null}
            </Stack>
          </Card>

          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <div>
                  <Text fw={700}>Route selection</Text>
                  <Text size="sm" c="dimmed">Select which fixed backend each AI route uses.</Text>
                </div>
                <Badge color={config.systemMode === 'off' ? 'red' : 'green'}>
                  {config.systemMode === 'off' ? 'AI off' : 'AI enabled'}
                </Badge>
              </Group>
              <Group grow align="flex-start">
                {config.routes.map((route) => (
                  <Select
                    key={route.id}
                    label={route.label}
                    data={routeOptions}
                    value={route.backendId}
                    onChange={(value) => updateRoute(route.id, value || 'openai')}
                  />
                ))}
              </Group>
              <Stack gap={4}>
                {config.routes.map((route) => (
                  <InfoLine
                    key={route.id}
                    label={route.id}
                    value={`${route.backendId} / ${route.provider} / ${route.model} / ${statusText(route.healthy)}`}
                  />
                ))}
              </Stack>
            </Stack>
          </Card>
        </Stack>
      ) : null}
    </Stack>
  );

  function updateConfig(patch: Partial<HermesBackendConfigResponse>) {
    setApplyFailed(false);
    setConfig((current) => current ? { ...current, ...patch } : current);
  }

  function updateBackend(backendId: string, patch: Partial<HermesBackend>) {
    setApplyFailed(false);
    setConfig((current) => current ? {
      ...current,
      backends: current.backends.map((backend) => backend.id === backendId ? { ...backend, ...patch } : backend),
    } : current);
  }

  function updateRoute(routeId: string, backendId: string) {
    setApplyFailed(false);
    setConfig((current) => current ? {
      ...current,
      routes: current.routes.map((route) => route.id === routeId ? { ...route, backendId } : route),
    } : current);
  }

  function addLocalBackend() {
    setApplyFailed(false);
    setConfig((current) => {
      if (!current) {
        return current;
      }
      const locals = current.backends.filter((backend) => isLocalBackend(backend));
      const source = locals[0];
      const nextId = nextLocalBackendId(locals);
      const nextBackend: HermesBackend = {
        id: nextId,
        label: localLabel(nextId),
        provider: source?.provider || 'ollama',
        baseUrl: source?.baseUrl || defaultRunnerUrl(source?.provider || 'ollama'),
        model: source?.model || '',
        apiMode: source?.apiMode || 'responses',
        timeoutSeconds: source?.timeoutSeconds || 120,
        contextWindow: source?.contextWindow || 65536,
        healthy: null,
        toolCapable: null,
        detail: 'Local backend has not been validated',
        lastValidatedAt: null,
        validationStatus: 'NOT_VALIDATED',
        apiKeyConfigured: false,
        reasoningDisabled: nextBackendProviderDisablesReasoning(source?.provider || 'ollama'),
        apiKey: '',
        clearApiKey: false,
      };
      return {
        ...current,
        backends: [...current.backends, nextBackend],
      };
    });
  }

  function deleteLocalBackend(backendId: string) {
    setApplyFailed(false);
    setConfig((current) => {
      if (!current) {
        return current;
      }
      const locals = current.backends.filter((backend) => isLocalBackend(backend));
      if (locals.length <= 1 || current.routes.some((route) => route.backendId === backendId)) {
        return current;
      }
      if (modelBackendId === backendId) {
        setModelBackendId(locals.find((backend) => backend.id !== backendId)?.id || 'local-0');
      }
      return {
        ...current,
        backends: current.backends.filter((backend) => backend.id !== backendId),
      };
    });
  }

  function routeUsesBackend(backendId: string) {
    return Boolean(config?.routes.some((route) => route.backendId === backendId));
  }
}

function BackendCard({
  title,
  backend,
  modelOptions,
  disabledProvider,
  canDelete,
  onDelete,
  onChange,
}: {
  title: string;
  backend: HermesBackend;
  modelOptions: string[];
  disabledProvider?: boolean;
  canDelete?: boolean;
  onDelete?: () => void;
  onChange: (patch: Partial<HermesBackend>) => void;
}) {
  const local = isLocalBackend(backend);
  return (
    <Card withBorder radius="sm">
      <Stack gap="md">
        <Group justify="space-between" align="flex-start">
          <div>
            <Text fw={700}>{title}</Text>
            <Text size="sm" c="dimmed">{backend.detail || 'Not checked'}</Text>
          </div>
          <Badge color={backend.healthy === false ? 'red' : backend.healthy ? 'green' : 'gray'}>
            {statusText(backend.healthy)}
          </Badge>
        </Group>
        {onDelete ? (
          <Group justify="flex-end">
            <Button
              color="red"
              variant="subtle"
              leftSection={<Trash2 size={16} />}
              disabled={!canDelete}
              onClick={onDelete}
            >
              Delete
            </Button>
          </Group>
        ) : null}
        <Group grow align="flex-start">
          <Select
            label="Provider"
            data={local ? LOCAL_RUNNER_OPTIONS : [{ value: 'openai', label: 'OpenAI' }]}
            value={backend.provider}
            disabled={disabledProvider}
            onChange={(value) => onChange({
              provider: value || backend.provider,
              baseUrl: local ? defaultRunnerUrl(value || backend.provider) : null,
              reasoningDisabled: nextBackendProviderDisablesReasoning(value || backend.provider),
            })}
          />
          <TextInput
            label="OpenAI-compatible base URL"
            value={backend.baseUrl || ''}
            disabled={!local}
            onChange={(event) => onChange({ baseUrl: event.currentTarget.value })}
          />
          {modelOptions.length > 0 ? (
            <Select
              label="Model"
              data={modelOptions}
              value={modelOptions.includes(backend.model) ? backend.model : null}
              placeholder={backend.model || 'Select model'}
              searchable
              onChange={(value) => value && onChange({ model: value })}
            />
          ) : (
            <TextInput
              label="Model"
              value={backend.model || ''}
              onChange={(event) => onChange({ model: event.currentTarget.value })}
            />
          )}
          <NumberInput
            label="Timeout seconds"
            min={1}
            value={backend.timeoutSeconds || 120}
            onChange={(value) => onChange({ timeoutSeconds: typeof value === 'number' ? value : 120 })}
          />
        </Group>
        {local ? (
          <Group grow align="flex-start">
            <NumberInput
              label="Context window"
              min={65536}
              step={1024}
              value={backend.contextWindow || 65536}
              onChange={(value) => onChange({ contextWindow: typeof value === 'number' ? value : 65536 })}
            />
            <CredentialFields
              configured={Boolean(backend.apiKeyConfigured)}
              value={backend.apiKey || ''}
              clear={Boolean(backend.clearApiKey)}
              onChange={(apiKey) => onChange({ apiKey, clearApiKey: false })}
              onClear={(clearApiKey) => onChange({ clearApiKey, apiKey: '' })}
            />
            <Switch
              label="Disable reasoning"
              description="For Ollama thinking models, sends reasoning_effort=none through Hermes."
              checked={Boolean(backend.reasoningDisabled)}
              disabled={backend.provider !== 'ollama'}
              onChange={(event) => onChange({ reasoningDisabled: event.currentTarget.checked })}
            />
          </Group>
        ) : null}
        <Group grow>
          <InfoLine label="API mode" value={backend.apiMode || '-'} />
          <InfoLine label="Tools" value={statusText(backend.toolCapable)} />
          <InfoLine label="Validation" value={backend.validationStatus || '-'} />
          <InfoLine label="Last validated" value={formatDate(backend.lastValidatedAt)} />
        </Group>
      </Stack>
    </Card>
  );
}

function CredentialFields({
  configured,
  value,
  clear,
  onChange,
  onClear,
}: {
  configured: boolean;
  value: string;
  clear: boolean;
  onChange: (apiKey: string) => void;
  onClear: (clear: boolean) => void;
}) {
  return (
    <Group grow align="flex-end">
      <TextInput
        label={configured ? 'API key (configured)' : 'API key'}
        type="password"
        value={value}
        placeholder={configured ? 'Leave blank to keep current key' : ''}
        onChange={(event) => onChange(event.currentTarget.value)}
      />
      <Switch
        label="Clear saved API key"
        checked={clear}
        onChange={(event) => onClear(event.currentTarget.checked)}
      />
    </Group>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <Text size="sm">
      <Text span fw={700}>{label}: </Text>
      {value}
    </Text>
  );
}

function SettingsError({ error }: { error: Error }) {
  const apiError = error as ApiError;
  return (
    <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError.message || 'Could not load settings'}</Text>
      {apiError.detail ? <Text size="sm">{apiError.detail}</Text> : null}
    </Alert>
  );
}

function backendById(config: HermesBackendConfigResponse | null, id: string) {
  return config?.backends.find((backend) => backend.id === id) || null;
}

function firstLocalBackend(config: HermesBackendConfigResponse | null) {
  return config?.backends.find((backend) => isLocalBackend(backend)) || null;
}

function isLocalBackend(backend: HermesBackend | null | undefined) {
  return Boolean(backend?.id.match(/^local-\d+$/));
}

function nextLocalBackendId(backends: HermesBackend[]) {
  const maxIndex = backends
    .map((backend) => localIndex(backend.id))
    .reduce((max, index) => Math.max(max, index), -1);
  return `local-${maxIndex + 1}`;
}

function localIndex(id: string) {
  const match = id.match(/^local-(\d+)$/);
  return match ? Number(match[1]) : -1;
}

function localLabel(id: string) {
  return `#${localIndex(id)} local`;
}

function defaultRunnerUrl(provider: string) {
  switch (provider) {
    case 'lmstudio':
      return 'http://localhost:1234/v1';
    case 'vllm':
      return 'http://localhost:8000/v1';
    default:
      return 'http://localhost:11434/v1';
  }
}

function nextBackendProviderDisablesReasoning(provider: string) {
  return provider === 'ollama';
}

function statusText(value: boolean | null | undefined) {
  if (value == null) {
    return 'unknown';
  }
  return value ? 'available' : 'unavailable';
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}
