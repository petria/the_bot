import { Alert, Autocomplete, Badge, Button, Card, Group, Loader, Modal, NumberInput, Select, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, RefreshCw, Save } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getHermesBackendConfig,
  getHermesFallbackModels,
  updateHermesBackendConfig,
  type HermesBackendConfigResponse,
  type HermesFallbackModel,
  type HermesProfile,
} from '../api/adminSystem';

const PROFILE_ORDER = ['chat', 'coder', 'ai-command'] as const;
const LOCAL_RUNNER_OPTIONS = [
  { value: 'ollama', label: 'Ollama' },
  { value: 'lmstudio', label: 'LM Studio' },
  { value: 'vllm', label: 'vLLM' },
];

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const hermesBackendQuery = useQuery({
    queryKey: ['admin-system-hermes-backends'],
    queryFn: getHermesBackendConfig,
  });
  const [backendConfig, setBackendConfig] = useState<HermesBackendConfigResponse | null>(null);
  const [modelProfileId, setModelProfileId] = useState<string>('');
  const [fallbackModels, setFallbackModels] = useState<string[]>([]);
  const [fallbackModelItems, setFallbackModelItems] = useState<HermesFallbackModel[]>([]);
  const [loadedModelProfileId, setLoadedModelProfileId] = useState<string>('');
  const [overrideConfirmation, setOverrideConfirmation] = useState<'enable' | 'disable' | null>(null);
  const fallback = backendConfig?.fallback || null;
  const globalOverride = backendConfig?.globalOverride || null;
  const overrideEnabled = Boolean(globalOverride?.enabled);

  useEffect(() => {
    if (!hermesBackendQuery.data) {
      return;
    }
    setBackendConfig(hermesBackendQuery.data);
    const firstLocal = hermesBackendQuery.data.profiles.find((profile) => isLocalRunner(profile.provider));
    setModelProfileId(firstLocal?.id || hermesBackendQuery.data.profiles[0]?.id || '');
  }, [hermesBackendQuery.data]);

  const updateBackendsMutation = useMutation({
    mutationFn: (config: HermesBackendConfigResponse) => updateHermesBackendConfig(config),
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-hermes-backends'], response);
      setBackendConfig(response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });
  const fallbackModelsMutation = useMutation({
    mutationFn: (profile: HermesProfile) =>
      getHermesFallbackModels(profile.provider, profile.baseUrl || '', profile.apiKey, profile.id),
    onSuccess: (response, profile) => {
      setFallbackModels(response.models || []);
      setFallbackModelItems(response.items?.length ? response.items : (response.models || []).map((model) => ({
        id: model,
        suitability: 'unknown',
        label: 'tool support unknown',
        toolCapable: null,
        detail: null,
      })));
      setLoadedModelProfileId(profile.id);
    },
  });

  const selectedModelProfile = backendConfig?.profiles.find((profile) => profile.id === modelProfileId) || null;
  const selectedDiscoveredModel = fallbackModelItems.find((model) => model.id === selectedModelProfile?.model) || null;
  const discoveredModelOptions = useMemo(
    () => fallbackModelItems.map((model) => ({
      value: model.id,
      label: `${model.id} - ${model.label}`,
    })),
    [fallbackModelItems]
  );
  const orderedProfiles = useMemo(
    () => (backendConfig?.profiles || []).slice().sort((left, right) => PROFILE_ORDER.indexOf(left.id as never) - PROFILE_ORDER.indexOf(right.id as never)),
    [backendConfig?.profiles]
  );
  const hasBackendChanges = Boolean(
    backendConfig && JSON.stringify(backendConfig) !== JSON.stringify(hermesBackendQuery.data)
  );

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage AI Routes</Title>
          <Text c="dimmed">Hermes profiles, models, local runners, and fallback routing.</Text>
        </div>
      </Group>

      {hermesBackendQuery.isLoading ? <Loader /> : null}
      {hermesBackendQuery.isError ? <SettingsError error={hermesBackendQuery.error} /> : null}
      {updateBackendsMutation.isError ? <SettingsError error={updateBackendsMutation.error} /> : null}
      {fallbackModelsMutation.isError ? <SettingsError error={fallbackModelsMutation.error} /> : null}

      {backendConfig ? (
        <Stack gap="md">
          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start" gap="sm">
                <div>
                  <Text fw={700}>Emergency local override</Text>
                  <Text size="sm" c="dimmed">
                    Forces chat, coder, and AI-command routes through the configured local fallback backend.
                  </Text>
                </div>
                <Badge color={overrideEnabled ? 'red' : 'gray'}>
                  {overrideEnabled ? 'Forced local route' : 'Inactive'}
                </Badge>
              </Group>

              {overrideEnabled ? (
                <Alert color="red" variant="filled" icon={<AlertCircle size={18} />}>
                  <Text fw={700}>All AI routes are forced to the local backend.</Text>
                  <Text size="sm">Requests fail closed if this backend is unavailable. Normal route editing is locked.</Text>
                </Alert>
              ) : null}

              <Group grow align="flex-start">
                <InfoLine label="Runner" value={displayRunner(fallback?.provider)} />
                <InfoLine label="Model" value={fallback?.model || '-'} />
                <InfoLine label="Endpoint" value={fallback?.baseUrl || '-'} />
                <InfoLine label="Context window" value={String(fallback?.contextWindow || '-')} />
              </Group>
              <Group justify="space-between" align="flex-end">
                <Stack gap={2}>
                  <InfoLine label="Status" value={globalOverride?.detail || 'Global override disabled'} />
                  <InfoLine label="Activated" value={formatDate(globalOverride?.activatedAt)} />
                  <InfoLine label="Changed by" value={globalOverride?.updatedBy || '-'} />
                </Stack>
                <Button
                  color={overrideEnabled ? 'red' : 'orange'}
                  loading={updateBackendsMutation.isPending}
                  disabled={!overrideEnabled && (!fallback?.baseUrl || !fallback?.model)}
                  onClick={() => setOverrideConfirmation(overrideEnabled ? 'disable' : 'enable')}
                >
                  {overrideEnabled ? 'Disable emergency override' : 'Validate and force all routes'}
                </Button>
              </Group>
            </Stack>
          </Card>

          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start" gap="sm">
                <div>
                  <Text fw={700}>Local LLM fallback</Text>
                  <Text size="sm" c="dimmed">Used only when an OpenAI profile is unavailable and that profile permits fallback.</Text>
                </div>
                <Badge color={fallback?.enabled ? (fallback.healthy === false ? 'red' : 'green') : 'gray'}>
                  {fallback?.enabled ? 'Enabled' : 'Disabled'}
                </Badge>
              </Group>

              <Switch
                label="Enable local LLM fallback"
                checked={Boolean(fallback?.enabled)}
                disabled={overrideEnabled}
                onChange={(event) => updateFallback({ enabled: event.currentTarget.checked })}
              />

              <Group grow align="flex-start">
                <Select
                  label="Local runner"
                  data={LOCAL_RUNNER_OPTIONS}
                  value={fallback?.provider || 'ollama'}
                  disabled={overrideEnabled}
                  onChange={(value) => updateFallback({
                    provider: value || 'ollama',
                    baseUrl: defaultRunnerUrl(value || 'ollama'),
                  })}
                />
                <TextInput
                  label="OpenAI-compatible base URL"
                  value={fallback?.baseUrl || ''}
                  disabled={overrideEnabled}
                  onChange={(event) => updateFallback({ baseUrl: event.currentTarget.value })}
                />
                <Autocomplete
                  label="Fallback model"
                  data={loadedModelProfileId === '__fallback__' ? fallbackModels : []}
                  value={fallback?.model || ''}
                  disabled={overrideEnabled}
                  onChange={(value) => updateFallback({ model: value })}
                />
                <NumberInput
                  label="Context window"
                  min={65536}
                  step={1024}
                  value={fallback?.contextWindow || 65536}
                  disabled={overrideEnabled}
                  onChange={(value) => updateFallback({ contextWindow: typeof value === 'number' ? value : 65536 })}
                />
              </Group>
              <CredentialFields
                configured={Boolean(fallback?.apiKeyConfigured)}
                disabled={overrideEnabled}
                value={fallback?.apiKey || ''}
                clear={Boolean(fallback?.clearApiKey)}
                onChange={(apiKey) => updateFallback({ apiKey, clearApiKey: false })}
                onClear={(clearApiKey) => updateFallback({ clearApiKey, apiKey: '' })}
              />

              <Group justify="space-between" align="flex-end">
                <Stack gap={2}>
                  <InfoLine label="Reachability" value={fallback?.detail || 'Not checked'} />
                  <InfoLine label="Validation" value={fallback?.validationStatus || 'NOT_VALIDATED'} />
                  <InfoLine label="Last validated" value={formatDate(fallback?.lastValidatedAt)} />
                </Stack>
                <Button
                  variant="light"
                  leftSection={<RefreshCw size={18} />}
                  loading={fallbackModelsMutation.isPending && loadedModelProfileId === '__fallback__'}
                  disabled={overrideEnabled || !fallback?.baseUrl}
                  onClick={() => fallback && fallbackModelsMutation.mutate({
                    id: '__fallback__',
                    label: 'Environment fallback',
                    provider: fallback.provider || 'ollama',
                    baseUrl: fallback.baseUrl,
                    model: fallback.model,
                    apiMode: 'chat-completions',
                    timeoutSeconds: 120,
                    healthy: fallback.healthy,
                    toolCapable: fallback.toolCapable,
                    detail: fallback.detail,
                    contextWindow: fallback.contextWindow,
                    fallbackAllowed: false,
                    activeProvider: fallback.provider || 'ollama',
                    gatewayHealthy: null,
                    primaryProviderHealthy: fallback.healthy,
                    fallbackHealthy: fallback.healthy,
                    cooldownUntil: null,
                    fallbackReason: null,
                    fallbackActivatedAt: null,
                    lastProviderError: null,
                    lastProviderErrorAt: null,
                    lastValidatedAt: fallback.lastValidatedAt,
                    validationStatus: fallback.validationStatus,
                    apiKeyConfigured: fallback.apiKeyConfigured,
                    apiKey: fallback.apiKey,
                  })}
                >
                  Load fallback models
                </Button>
              </Group>
            </Stack>
          </Card>

          <Card withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start" gap="sm">
                <div>
                  <Text fw={700}>Hermes Profiles</Text>
                  <Text size="sm" c="dimmed">Each logical Hermes profile chooses its provider directly.</Text>
                </div>
                <Badge color={backendConfig.profiles.every((profile) => profile.healthy !== false) ? 'green' : 'yellow'}>
                  {backendConfig.profiles.length} profiles
                </Badge>
              </Group>

            <Stack gap="sm">
              {orderedProfiles.map((profile) => (
                <Card key={profile.id} withBorder radius="sm">
                  <Stack gap="sm">
                    <Group justify="space-between" align="flex-start" gap="sm">
                      <div>
                        <Text fw={600}>{profile.label}</Text>
                        <Text size="xs" c="dimmed">{profile.id}</Text>
                      </div>
                      <Badge
                        color={profile.gatewayHealthy === false || profile.primaryProviderHealthy === false ? 'red' : 'green'}
                        leftSection={profile.healthy === false ? <AlertCircle size={12} /> : <CheckCircle2 size={12} />}
                      >
                        {profile.activeProvider || profile.provider}
                      </Badge>
                    </Group>

                    <Group grow align="flex-start">
                      <Select
                        label="Provider"
                        data={[
                          { value: 'openai', label: 'OpenAI' },
                          ...LOCAL_RUNNER_OPTIONS,
                        ]}
                        value={profile.provider}
                        disabled={overrideEnabled}
                        onChange={(value) => updateProfile(profile.id, {
                          provider: value || profile.provider,
                          baseUrl: value === 'openai'
                            ? null
                            : defaultRunnerUrl(value || 'ollama'),
                        })}
                      />
                      <Select
                        label="API mode"
                        data={[
                          { value: 'responses', label: 'Responses' },
                          { value: 'chat-completions', label: 'Chat completions' },
                        ]}
                        value={profile.apiMode}
                        disabled={overrideEnabled}
                        onChange={(value) => updateProfile(profile.id, { apiMode: value || profile.apiMode })}
                      />
                      <NumberInput
                        label="Timeout seconds"
                        min={1}
                        value={profile.timeoutSeconds || 120}
                        disabled={overrideEnabled}
                        onChange={(value) => updateProfile(profile.id, { timeoutSeconds: typeof value === 'number' ? value : 120 })}
                      />
                    </Group>

                    <Group grow align="flex-start">
                      <TextInput
                        label="Label"
                        value={profile.label}
                        disabled={overrideEnabled}
                        onChange={(event) => updateProfile(profile.id, { label: event.currentTarget.value })}
                      />
                      <Autocomplete
                        label="Model"
                        data={modelProfileId === profile.id ? fallbackModels : []}
                        value={profile.model}
                        disabled={overrideEnabled}
                        onChange={(value) => updateProfile(profile.id, { model: value })}
                      />
                    </Group>

                    {isLocalRunner(profile.provider) ? (
                      <Stack gap="sm">
                        <Group grow align="flex-start">
                        <TextInput
                          label="OpenAI-compatible base URL"
                          value={profile.baseUrl || ''}
                          disabled={overrideEnabled}
                          onChange={(event) => updateProfile(profile.id, { baseUrl: event.currentTarget.value })}
                        />
                        <NumberInput
                          label="Context window"
                          min={65536}
                          step={1024}
                          value={profile.contextWindow || 65536}
                          disabled={overrideEnabled}
                          onChange={(value) => updateProfile(profile.id, {
                            contextWindow: typeof value === 'number' ? value : 65536,
                          })}
                        />
                        </Group>
                        <CredentialFields
                          configured={Boolean(profile.apiKeyConfigured)}
                          disabled={overrideEnabled}
                          value={profile.apiKey || ''}
                          clear={Boolean(profile.clearApiKey)}
                          onChange={(apiKey) => updateProfile(profile.id, { apiKey, clearApiKey: false })}
                          onClear={(clearApiKey) => updateProfile(profile.id, { clearApiKey, apiKey: '' })}
                        />
                      </Stack>
                    ) : (
                      <Stack gap="xs">
                        <Switch
                          label="Allow local LLM fallback"
                          checked={Boolean(profile.fallbackAllowed)}
                          disabled={overrideEnabled || !fallback?.enabled}
                          onChange={(event) => updateProfile(profile.id, { fallbackAllowed: event.currentTarget.checked })}
                        />
                        <Alert color="blue" variant="light" icon={<CheckCircle2 size={18} />}>
                          <Text size="sm">OpenAI credentials stay in the Hermes profile. The model field is the upstream OpenAI model.</Text>
                        </Alert>
                      </Stack>
                    )}

                    <Stack gap={4}>
                      <InfoLine label="Configured provider" value={profile.provider} />
                      <InfoLine label="Active provider" value={profile.activeProvider || profile.provider} />
                      <InfoLine label="Gateway" value={statusText(profile.gatewayHealthy)} />
                      <InfoLine label="Primary provider" value={statusText(profile.primaryProviderHealthy)} />
                      <InfoLine label="Provider status" value={profile.fallbackReason || profile.lastProviderError || profile.detail || '-'} />
                      <InfoLine label="Fallback active since" value={formatDate(profile.fallbackActivatedAt)} />
                      {isLocalRunner(profile.provider) ? (
                        <InfoLine label="Tool support" value={profile.toolCapable === false ? 'Not verified' : 'Available'} />
                      ) : (
                        <InfoLine label="Gateway mode" value="Shared Hermes profile" />
                      )}
                    </Stack>
                  </Stack>
                </Card>
              ))}
            </Stack>

            <Group justify="flex-end" align="flex-end">
              <Select
                w={260}
                label="Model discovery profile"
                value={modelProfileId}
                disabled={overrideEnabled}
                data={orderedProfiles
                  .filter((profile) => isLocalRunner(profile.provider))
                  .map((profile) => ({ value: profile.id, label: profile.label }))}
                onChange={(value) => {
                  setModelProfileId(value || '');
                  setFallbackModels([]);
                  setFallbackModelItems([]);
                  setLoadedModelProfileId('');
                }}
              />
              <Button
                variant="light"
                leftSection={<RefreshCw size={18} />}
                loading={fallbackModelsMutation.isPending}
                disabled={overrideEnabled || !isLocalRunner(selectedModelProfile?.provider) || !selectedModelProfile?.baseUrl}
                onClick={() => selectedModelProfile && fallbackModelsMutation.mutate(selectedModelProfile)}
              >
                Load models
              </Button>
              <Select
                w={300}
                label="Discovered model"
                searchable
                disabled={overrideEnabled || !selectedModelProfile || loadedModelProfileId !== selectedModelProfile.id || fallbackModels.length === 0}
                data={discoveredModelOptions}
                value={selectedModelProfile && fallbackModels.includes(selectedModelProfile.model) ? selectedModelProfile.model : null}
                placeholder={loadedModelProfileId === selectedModelProfile?.id ? `${fallbackModels.length} models loaded` : 'Load models first'}
                onChange={(value) => {
                  if (selectedModelProfile && value) {
                    updateProfile(selectedModelProfile.id, { model: value });
                  }
                }}
              />
              <Button
                leftSection={<Save size={18} />}
                loading={updateBackendsMutation.isPending}
                disabled={overrideEnabled || !hasBackendChanges}
                onClick={() => updateBackendsMutation.mutate(requireBackendConfig(backendConfig))}
              >
                Validate and apply
              </Button>
            </Group>
            {selectedModelProfile && loadedModelProfileId === selectedModelProfile.id ? (
              <Stack gap={2} align="flex-end">
                <Text size="sm" c="dimmed" ta="right">
                  Loaded {fallbackModels.length} models for {selectedModelProfile.label}. Tool-capable models are listed first.
                </Text>
                {selectedDiscoveredModel ? (
                  <Text size="sm" c={selectedDiscoveredModel.toolCapable === false ? 'yellow' : 'dimmed'} ta="right">
                    Selected discovery result: {selectedDiscoveredModel.label}
                    {selectedDiscoveredModel.detail ? ` - ${selectedDiscoveredModel.detail}` : ''}
                  </Text>
                ) : null}
              </Stack>
            ) : null}
            </Stack>
          </Card>
        </Stack>
      ) : null}

      <Modal
        opened={overrideConfirmation != null}
        onClose={() => setOverrideConfirmation(null)}
        title={overrideConfirmation === 'enable' ? 'Force all AI routes locally?' : 'Restore normal AI routing?'}
        centered
      >
        <Stack gap="md">
          <Text size="sm">
            {overrideConfirmation === 'enable'
              ? `All AI traffic will use ${displayRunner(fallback?.provider)} model ${fallback?.model || 'unknown'} at ${fallback?.baseUrl || 'unknown'}. Requests will fail closed if it is unavailable.`
              : 'The saved provider and fallback settings for each Hermes profile will be restored.'}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setOverrideConfirmation(null)}>Cancel</Button>
            <Button
              color={overrideConfirmation === 'enable' ? 'orange' : 'red'}
              loading={updateBackendsMutation.isPending}
              onClick={() => {
                if (!backendConfig || overrideConfirmation == null) {
                  return;
                }
                const next = {
                  ...backendConfig,
                  globalOverride: {
                    enabled: overrideConfirmation === 'enable',
                    provider: fallback?.provider || 'ollama',
                    baseUrl: fallback?.baseUrl || '',
                    model: fallback?.model || '',
                    contextWindow: fallback?.contextWindow || 65536,
                    healthy: globalOverride?.healthy || null,
                    detail: globalOverride?.detail || null,
                    activatedAt: globalOverride?.activatedAt || null,
                    updatedBy: globalOverride?.updatedBy || null,
                    validationStatus: globalOverride?.validationStatus || null,
                    lastValidatedAt: globalOverride?.lastValidatedAt || null,
                  },
                };
                setOverrideConfirmation(null);
                updateBackendsMutation.mutate(next);
              }}
            >
              {overrideConfirmation === 'enable' ? 'Validate and force' : 'Disable override'}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );

  function updateProfile(profileId: string, patch: Partial<HermesProfile>) {
    setBackendConfig((current) => current == null ? current : {
      ...current,
      profiles: current.profiles.map((profile) => profile.id === profileId ? { ...profile, ...patch } : profile),
    });
  }

  function updateFallback(patch: Partial<NonNullable<HermesBackendConfigResponse['fallback']>>) {
    setBackendConfig((current) => current == null ? current : {
      ...current,
      fallback: {
        enabled: false,
        provider: 'ollama',
        baseUrl: '',
        model: '',
        profiles: [],
        contextWindow: 65536,
        healthy: null,
        toolCapable: null,
        detail: null,
        lastValidatedAt: null,
        validationStatus: 'NOT_VALIDATED',
        apiKeyConfigured: false,
        apiKey: '',
        clearApiKey: false,
        ...current.fallback,
        ...patch,
      },
    });
  }
}

function CredentialFields({
  configured,
  value,
  clear,
  disabled = false,
  onChange,
  onClear,
}: {
  configured: boolean;
  value: string;
  clear: boolean;
  disabled?: boolean;
  onChange: (value: string) => void;
  onClear: (value: boolean) => void;
}) {
  return (
    <Group grow align="flex-end">
      <TextInput
        type="password"
        label="Optional API key"
        description={configured
          ? 'A key is configured. Leave blank to preserve it.'
          : 'Leave blank for an unauthenticated LAN endpoint.'}
        placeholder={configured ? 'Configured' : 'Not configured'}
        value={value}
        disabled={disabled || clear}
        onChange={(event) => onChange(event.currentTarget.value)}
      />
      <Switch
        label="Clear configured API key"
        checked={clear}
        disabled={disabled || (!configured && !value)}
        onChange={(event) => onClear(event.currentTarget.checked)}
      />
    </Group>
  );
}

function isLocalRunner(provider: string | null | undefined) {
  return provider === 'ollama' || provider === 'lmstudio' || provider === 'vllm';
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

function displayRunner(provider: string | null | undefined) {
  return LOCAL_RUNNER_OPTIONS.find((option) => option.value === provider)?.label || provider || '-';
}

function requireBackendConfig(config: HermesBackendConfigResponse | null) {
  if (config == null) {
    throw new Error('Hermes backend configuration is not loaded.');
  }
  return config;
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <Group justify="space-between" gap="sm" wrap="nowrap" className="system-info-line">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text size="sm" ta="right" className="system-info-value">{value}</Text>
    </Group>
  );
}

function statusText(value: boolean | null | undefined) {
  if (value == null) {
    return 'Unknown';
  }
  return value ? 'Available' : 'Unavailable';
}

function formatDate(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function SettingsError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError?.message || 'Could not load system settings.'}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}
