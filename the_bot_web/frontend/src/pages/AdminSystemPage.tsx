import { Alert, Autocomplete, Badge, Button, Card, Group, Loader, Select, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, RefreshCw, Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getHermesBackendConfig,
  getHermesFallbackModels,
  updateHermesBackendConfig,
  type HermesBackendConfigResponse,
  type HermesBackendProfile,
  type HermesAiRoute,
} from '../api/adminSystem';

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const hermesBackendQuery = useQuery({
    queryKey: ['admin-system-hermes-backends'],
    queryFn: getHermesBackendConfig,
  });
  const [backendConfig, setBackendConfig] = useState<HermesBackendConfigResponse | null>(null);
  const [modelProfileId, setModelProfileId] = useState<string>('');
  const [fallbackModels, setFallbackModels] = useState<string[]>([]);

  useEffect(() => {
    if (hermesBackendQuery.data) {
      setBackendConfig(hermesBackendQuery.data);
      const firstOpenAi = hermesBackendQuery.data.profiles.find((profile) => profile.type === 'OPENAI_COMPATIBLE');
      setModelProfileId(firstOpenAi?.id || '');
    }
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
    mutationFn: (profile: HermesBackendProfile) => getHermesFallbackModels(profile.baseUrl),
    onSuccess: setFallbackModels,
  });

  const selectedModelProfile = backendConfig?.profiles.find((profile) => profile.id === modelProfileId) || null;
  const hasBackendChanges = Boolean(
    backendConfig && JSON.stringify(backendConfig) !== JSON.stringify(hermesBackendQuery.data)
  );

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Runtime system settings used by bot-engine.</Text>
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
                <Text fw={700}>Hermes Backend Profiles</Text>
                <Text size="sm" c="dimmed">Configure reusable Hermes and OpenAI-compatible backends.</Text>
              </div>
              <Badge color={backendConfig.profiles.every((profile) => profile.healthy !== false) ? 'green' : 'yellow'}>
                {backendConfig.profiles.length} profiles
              </Badge>
            </Group>

            <Stack gap="sm">
              {backendConfig.profiles.map((profile) => (
                <Card key={profile.id} withBorder radius="sm">
                  <Stack gap="sm">
                    <Group justify="space-between" align="flex-start" gap="sm">
                      <div>
                        <Text fw={600}>{profile.label}</Text>
                        <Text size="xs" c="dimmed">{profile.id}</Text>
                      </div>
                      <Badge color={profile.healthy === false ? 'red' : 'green'} leftSection={profile.healthy === false ? <AlertCircle size={12} /> : <CheckCircle2 size={12} />}>
                        {profile.type}
                      </Badge>
                    </Group>
                    <Group grow align="flex-start">
                      <Select
                        label="Type"
                        data={[
                          { value: 'HERMES_PROFILE', label: 'Hermes profile' },
                          { value: 'OPENAI_COMPATIBLE', label: 'OpenAI-compatible' },
                        ]}
                        value={profile.type}
                        onChange={(value) => updateProfile(profile.id, { type: value || profile.type })}
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
                    </Group>
                    <Group grow align="flex-start">
                      <TextInput
                        label="Base URL"
                        value={profile.baseUrl}
                        onChange={(event) => updateProfile(profile.id, { baseUrl: event.currentTarget.value })}
                      />
                      <Autocomplete
                        label="Model"
                        data={modelProfileId === profile.id ? fallbackModels : []}
                        value={profile.model}
                        onChange={(value) => updateProfile(profile.id, { model: value })}
                      />
                    </Group>
                    <TextInput
                      label="Label"
                      value={profile.label}
                      onChange={(event) => updateProfile(profile.id, { label: event.currentTarget.value })}
                    />
                    <Stack gap={4}>
                      <InfoLine label="Health URL" value={profile.healthUrl || '-'} />
                      <InfoLine label="Status" value={profile.detail || '-'} />
                    </Stack>
                  </Stack>
                </Card>
              ))}
            </Stack>

            <div>
              <Text fw={700}>Hermes Routes</Text>
              <Text size="sm" c="dimmed">Choose which backend handles each AI use case.</Text>
            </div>

            <Stack gap="xs">
              {backendConfig.routes.map((route) => (
                <Group key={route.routeId} justify="space-between" gap="sm" className="system-info-line">
                  <div>
                    <Text size="sm" fw={600}>{route.label}</Text>
                    <Text size="xs" c="dimmed">{route.model || '-'} | {route.apiMode || '-'}</Text>
                  </div>
                  <Select
                    w={260}
                    value={route.backendProfileId}
                    data={backendConfig.profiles.map((profile) => ({ value: profile.id, label: profile.label }))}
                    onChange={(value) => updateRoute(route.routeId, { backendProfileId: value || route.backendProfileId })}
                  />
                </Group>
              ))}
            </Stack>

            <Group justify="flex-end">
              <Select
                w={260}
                label="Model discovery profile"
                value={modelProfileId}
                data={backendConfig.profiles
                  .filter((profile) => profile.type === 'OPENAI_COMPATIBLE')
                  .map((profile) => ({ value: profile.id, label: profile.label }))}
                onChange={(value) => setModelProfileId(value || '')}
              />
              <Button
                variant="light"
                leftSection={<RefreshCw size={18} />}
                loading={fallbackModelsMutation.isPending}
                disabled={!selectedModelProfile?.baseUrl}
                onClick={() => selectedModelProfile && fallbackModelsMutation.mutate(selectedModelProfile)}
              >
                Load models
              </Button>
              <Button
                leftSection={<Save size={18} />}
                loading={updateBackendsMutation.isPending}
                disabled={!hasBackendChanges}
                onClick={() => updateBackendsMutation.mutate()}
              >
                Validate and apply
              </Button>
            </Group>
          </Stack>
        </Card>
      ) : null}
    </Stack>
  );

  function updateProfile(profileId: string, patch: Partial<HermesBackendProfile>) {
    setBackendConfig((current) => current == null ? current : {
      ...current,
      profiles: current.profiles.map((profile) => profile.id === profileId ? { ...profile, ...patch } : profile),
    });
  }

  function updateRoute(routeId: string, patch: Partial<HermesAiRoute>) {
    setBackendConfig((current) => current == null ? current : {
      ...current,
      routes: current.routes.map((route) => route.routeId === routeId ? { ...route, ...patch } : route),
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
