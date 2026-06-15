import { Alert, Autocomplete, Badge, Button, Card, Group, Loader, NumberInput, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, RefreshCw, Save } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getHermesBackendConfig,
  getHermesFallbackModels,
  updateHermesBackendConfig,
  type HermesBackendConfigResponse,
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
  const [loadedModelProfileId, setLoadedModelProfileId] = useState<string>('');

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
    onSuccess: (models, profile) => {
      setFallbackModels(models);
      setLoadedModelProfileId(profile.id);
    },
  });

  const selectedModelProfile = backendConfig?.profiles.find((profile) => profile.id === modelProfileId) || null;
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
                        color={profile.healthy === false ? 'red' : 'green'}
                        leftSection={profile.healthy === false ? <AlertCircle size={12} /> : <CheckCircle2 size={12} />}
                      >
                        {profile.provider}
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
                          contextWindow: value === 'openai' ? null : profile.contextWindow,
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
                        <NumberInput
                          label="Context window"
                          min={1}
                          value={profile.contextWindow || ''}
                          onChange={(value) => updateProfile(profile.id, {
                            contextWindow: typeof value === 'number' ? value : null,
                          })}
                        />
                      </Group>
                    ) : (
                      <Alert color="blue" variant="light" icon={<CheckCircle2 size={18} />}>
                        <Text size="sm">OpenAI credentials stay in the runtime environment. This UI edits provider selection and model only.</Text>
                      </Alert>
                    )}

                    <Stack gap={4}>
                      <InfoLine label="Provider status" value={profile.detail || '-'} />
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
                data={fallbackModels}
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
              <Text size="sm" c="dimmed" ta="right">
                Loaded {fallbackModels.length} models for {selectedModelProfile.label}.
              </Text>
            ) : null}
          </Stack>
        </Card>
      ) : null}
    </Stack>
  );

  function updateProfile(profileId: string, patch: Partial<HermesProfile>) {
    setBackendConfig((current) => current == null ? current : {
      ...current,
      profiles: current.profiles.map((profile) => profile.id === profileId ? { ...profile, ...patch } : profile),
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

function SettingsError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError?.message || 'Could not load system settings.'}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}
