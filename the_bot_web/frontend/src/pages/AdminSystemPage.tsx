import { Alert, Autocomplete, Badge, Button, Card, Group, Loader, NumberInput, Select, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
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
  const fallback = backendConfig?.fallback || null;

  useEffect(() => {
    if (!hermesBackendQuery.data) {
      return;
    }
    setBackendConfig(hermesBackendQuery.data);
    const firstOllama = hermesBackendQuery.data.profiles.find((profile) => profile.provider === 'ollama');
    setModelProfileId(firstOllama?.id || hermesBackendQuery.data.profiles[0]?.id || '');
  }, [hermesBackendQuery.data]);

  const updateBackendsMutation = useMutation({
    mutationFn: () => updateHermesBackendConfig(requireBackendConfig(backendConfig)),
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-hermes-backends'], response);
      setBackendConfig(response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });
  const fallbackModelsMutation = useMutation({
    mutationFn: (profile: HermesProfile) => getHermesFallbackModels(profile.baseUrl || ''),
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
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Hermes profile provider settings used by bot-engine.</Text>
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
                  <Text fw={700}>Environment Ollama fallback</Text>
                  <Text size="sm" c="dimmed">Used only when an OpenAI profile is unavailable and that profile permits fallback.</Text>
                </div>
                <Badge color={fallback?.enabled ? (fallback.healthy === false ? 'red' : 'green') : 'gray'}>
                  {fallback?.enabled ? 'Enabled' : 'Disabled'}
                </Badge>
              </Group>

              <Switch
                label="Enable Ollama fallback"
                checked={Boolean(fallback?.enabled)}
                onChange={(event) => updateFallback({ enabled: event.currentTarget.checked })}
              />

              <Group grow align="flex-start">
                <TextInput
                  label="Ollama base URL"
                  value={fallback?.baseUrl || ''}
                  onChange={(event) => updateFallback({ baseUrl: event.currentTarget.value })}
                />
                <Autocomplete
                  label="Fallback model"
                  data={loadedModelProfileId === '__fallback__' ? fallbackModels : []}
                  value={fallback?.model || ''}
                  onChange={(value) => updateFallback({ model: value })}
                />
                <NumberInput
                  label="Context window"
                  min={1024}
                  step={1024}
                  value={fallback?.contextWindow || 32768}
                  onChange={(value) => updateFallback({ contextWindow: typeof value === 'number' ? value : 32768 })}
                />
              </Group>

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
                  disabled={!fallback?.baseUrl}
                  onClick={() => fallback && fallbackModelsMutation.mutate({
                    id: '__fallback__',
                    label: 'Environment fallback',
                    provider: 'ollama',
                    baseUrl: fallback.baseUrl,
                    model: fallback.model,
                    apiMode: 'chat-completions',
                    timeoutSeconds: 120,
                    healthy: fallback.healthy,
                    toolCapable: fallback.toolCapable,
                    detail: fallback.detail,
                    contextWindow: fallback.contextWindow,
                    fallbackAllowed: false,
                    activeProvider: 'ollama',
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
                          { value: 'ollama', label: 'Ollama' },
                        ]}
                        value={profile.provider}
                        onChange={(value) => updateProfile(profile.id, {
                          provider: value || profile.provider,
                          baseUrl: value === 'openai' ? null : profile.baseUrl,
                        })}
                      />
                      <Select
                        label="API mode"
                        data={[
                          { value: 'responses', label: 'Responses' },
                          { value: 'chat-completions', label: 'Chat completions' },
                        ]}
                        value={profile.apiMode}
                        onChange={(value) => updateProfile(profile.id, { apiMode: value || profile.apiMode })}
                      />
                      <NumberInput
                        label="Timeout seconds"
                        min={1}
                        value={profile.timeoutSeconds || 120}
                        onChange={(value) => updateProfile(profile.id, { timeoutSeconds: typeof value === 'number' ? value : 120 })}
                      />
                    </Group>

                    <Group grow align="flex-start">
                      <TextInput
                        label="Label"
                        value={profile.label}
                        onChange={(event) => updateProfile(profile.id, { label: event.currentTarget.value })}
                      />
                      <Autocomplete
                        label="Model"
                        data={modelProfileId === profile.id ? fallbackModels : []}
                        value={profile.model}
                        onChange={(value) => updateProfile(profile.id, { model: value })}
                      />
                    </Group>

                    {profile.provider === 'ollama' ? (
                      <Group grow align="flex-start">
                        <TextInput
                          label="Ollama base URL"
                          value={profile.baseUrl || ''}
                          onChange={(event) => updateProfile(profile.id, { baseUrl: event.currentTarget.value })}
                        />
                      </Group>
                    ) : (
                      <Stack gap="xs">
                        <Switch
                          label="Allow Ollama fallback"
                          checked={Boolean(profile.fallbackAllowed)}
                          disabled={!fallback?.enabled}
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
                      {profile.provider === 'ollama' ? (
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
                data={orderedProfiles
                  .filter((profile) => profile.provider === 'ollama')
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
                disabled={selectedModelProfile?.provider !== 'ollama' || !selectedModelProfile?.baseUrl}
                onClick={() => selectedModelProfile && fallbackModelsMutation.mutate(selectedModelProfile)}
              >
                Load models
              </Button>
              <Select
                w={300}
                label="Discovered model"
                searchable
                disabled={!selectedModelProfile || loadedModelProfileId !== selectedModelProfile.id || fallbackModels.length === 0}
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
                disabled={!hasBackendChanges}
                onClick={() => updateBackendsMutation.mutate()}
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
        baseUrl: '',
        model: '',
        profiles: [],
        contextWindow: 32768,
        healthy: null,
        toolCapable: null,
        detail: null,
        lastValidatedAt: null,
        validationStatus: 'NOT_VALIDATED',
        ...current.fallback,
        ...patch,
      },
    });
  }
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
