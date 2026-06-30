import { Alert, Badge, Button, Card, Group, Loader, NumberInput, Select, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, RefreshCw, Save } from 'lucide-react';
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

const ROUTE_OPTIONS = [
  { value: 'openai', label: 'OpenAI backend' },
  { value: 'local', label: 'Local LLM backend' },
];

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const backendQuery = useQuery({
    queryKey: ['admin-system-hermes-backends'],
    queryFn: getHermesBackendConfig,
  });
  const [config, setConfig] = useState<HermesBackendConfigResponse | null>(null);
  const [modelBackendId, setModelBackendId] = useState<string>('local');
  const [loadedModelBackendId, setLoadedModelBackendId] = useState<string>('');
  const [models, setModels] = useState<string[]>([]);
  const [modelItems, setModelItems] = useState<HermesFallbackModel[]>([]);
  const [applyFailed, setApplyFailed] = useState(false);

  useEffect(() => {
    if (backendQuery.data) {
      setConfig(backendQuery.data);
      setModelBackendId('local');
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
  const local = backendById(config, 'local');
  const selectedModelBackend = backendById(config, modelBackendId);
  const selectedModel = modelItems.find((item) => item.id === selectedModelBackend?.model) || null;
  const hasChanges = Boolean(config && JSON.stringify(config) !== JSON.stringify(backendQuery.data));
  const discoveredModelOptions = useMemo(
    () => modelItems.map((model) => ({ value: model.id, label: `${model.id} - ${model.label}` })),
    [modelItems]
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
              modelOptions={loadedModelBackendId === 'openai' ? models : []}
              disabledProvider
              onChange={(patch) => updateBackend('openai', patch)}
            />
          ) : null}

          {local ? (
            <BackendCard
              title="Local LLM backend"
              backend={local}
              modelOptions={loadedModelBackendId === 'local' ? models : []}
              onChange={(patch) => updateBackend('local', patch)}
            />
          ) : null}

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
                    data={ROUTE_OPTIONS}
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

          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-end">
                <Select
                  label="Model list source"
                  data={config.backends.map((backend) => ({ value: backend.id, label: backend.label }))}
                  value={modelBackendId}
                  onChange={(value) => setModelBackendId(value || 'local')}
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
                value={selectedModelBackend && models.includes(selectedModelBackend.model) ? selectedModelBackend.model : null}
                placeholder={loadedModelBackendId === selectedModelBackend?.id ? `${models.length} models loaded` : 'Load models first'}
                disabled={!selectedModelBackend || loadedModelBackendId !== selectedModelBackend.id || models.length === 0}
                searchable
                onChange={(value) => selectedModelBackend && value && updateBackend(selectedModelBackend.id, { model: value })}
              />
              {selectedModel ? (
                <InfoLine label="Selected model" value={`${selectedModel.label}; tools=${statusText(selectedModel.toolCapable)}`} />
              ) : null}
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
}

function BackendCard({
  title,
  backend,
  modelOptions,
  disabledProvider,
  onChange,
}: {
  title: string;
  backend: HermesBackend;
  modelOptions: string[];
  disabledProvider?: boolean;
  onChange: (patch: Partial<HermesBackend>) => void;
}) {
  const local = backend.id === 'local';
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
        <Group grow align="flex-start">
          <Select
            label="Provider"
            data={local ? LOCAL_RUNNER_OPTIONS : [{ value: 'openai', label: 'OpenAI' }]}
            value={backend.provider}
            disabled={disabledProvider}
            onChange={(value) => onChange({
              provider: value || backend.provider,
              baseUrl: local ? defaultRunnerUrl(value || backend.provider) : null,
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
