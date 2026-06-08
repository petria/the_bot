import { Alert, Autocomplete, Badge, Button, Card, Group, Loader, Radio, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, RefreshCw, Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getHermesSettings,
  getHermesFallback,
  getHermesFallbackModels,
  getOpenClawSettings,
  updateHermesSettings,
  updateHermesFallback,
  updateOpenClawSettings,
} from '../api/adminSystem';

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const settingsQuery = useQuery({
    queryKey: ['admin-system-openclaw'],
    queryFn: getOpenClawSettings,
  });
  const hermesSettingsQuery = useQuery({
    queryKey: ['admin-system-hermes'],
    queryFn: getHermesSettings,
  });
  const hermesFallbackQuery = useQuery({
    queryKey: ['admin-system-hermes-fallback'],
    queryFn: getHermesFallback,
  });
  const [selectedInstanceId, setSelectedInstanceId] = useState<string>('');
  const [selectedHermesProfileId, setSelectedHermesProfileId] = useState<string>('');
  const [fallbackBaseUrl, setFallbackBaseUrl] = useState('');
  const [fallbackModel, setFallbackModel] = useState('');
  const [fallbackModels, setFallbackModels] = useState<string[]>([]);

  useEffect(() => {
    if (settingsQuery.data?.currentInstanceId) {
      setSelectedInstanceId(settingsQuery.data.currentInstanceId);
    }
  }, [settingsQuery.data?.currentInstanceId]);

  useEffect(() => {
    if (hermesSettingsQuery.data?.currentProfileId) {
      setSelectedHermesProfileId(hermesSettingsQuery.data.currentProfileId);
    }
  }, [hermesSettingsQuery.data?.currentProfileId]);

  useEffect(() => {
    if (hermesFallbackQuery.data) {
      setFallbackBaseUrl(hermesFallbackQuery.data.baseUrl);
      setFallbackModel(hermesFallbackQuery.data.model);
    }
  }, [hermesFallbackQuery.data]);

  const updateMutation = useMutation({
    mutationFn: updateOpenClawSettings,
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-openclaw'], response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });
  const updateHermesMutation = useMutation({
    mutationFn: updateHermesSettings,
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-hermes'], response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });
  const fallbackModelsMutation = useMutation({
    mutationFn: getHermesFallbackModels,
    onSuccess: setFallbackModels,
  });
  const updateFallbackMutation = useMutation({
    mutationFn: () => updateHermesFallback(fallbackBaseUrl, fallbackModel),
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-hermes-fallback'], response);
    },
  });

  const hasChanges = Boolean(selectedInstanceId && selectedInstanceId !== settingsQuery.data?.currentInstanceId);
  const hasHermesChanges = Boolean(
    selectedHermesProfileId && selectedHermesProfileId !== hermesSettingsQuery.data?.currentProfileId
  );
  const hasFallbackChanges = Boolean(
    fallbackBaseUrl && fallbackModel
      && (fallbackBaseUrl !== hermesFallbackQuery.data?.baseUrl || fallbackModel !== hermesFallbackQuery.data?.model)
  );

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Runtime system settings used by bot-engine.</Text>
        </div>
      </Group>

      {settingsQuery.isLoading ? <Loader /> : null}
      {settingsQuery.isError ? <SettingsError error={settingsQuery.error} /> : null}
      {updateMutation.isError ? <SettingsError error={updateMutation.error} /> : null}
      {hermesSettingsQuery.isLoading ? <Loader /> : null}
      {hermesSettingsQuery.isError ? <SettingsError error={hermesSettingsQuery.error} /> : null}
      {updateHermesMutation.isError ? <SettingsError error={updateHermesMutation.error} /> : null}
      {hermesFallbackQuery.isLoading ? <Loader /> : null}
      {hermesFallbackQuery.isError ? <SettingsError error={hermesFallbackQuery.error} /> : null}
      {fallbackModelsMutation.isError ? <SettingsError error={fallbackModelsMutation.error} /> : null}
      {updateFallbackMutation.isError ? <SettingsError error={updateFallbackMutation.error} /> : null}

      {settingsQuery.data ? (
        <Card withBorder radius="sm">
          <Stack gap="md">
            <Group justify="space-between" align="flex-start" gap="sm">
              <div>
                <Text fw={700}>OpenClaw Backend</Text>
                <Text size="sm" c="dimmed">Select which OpenClaw gateway bot-engine uses for AI commands.</Text>
              </div>
              {settingsQuery.data.currentInstanceId ? (
                <Badge color="green" leftSection={<CheckCircle2 size={12} />}>
                  {settingsQuery.data.currentInstanceId}
                </Badge>
              ) : (
                <Badge color="yellow" leftSection={<AlertCircle size={12} />}>custom</Badge>
              )}
            </Group>

            <Radio.Group value={selectedInstanceId} onChange={setSelectedInstanceId}>
              <Stack gap="sm">
                {settingsQuery.data.options.map((option) => (
                  <Card key={option.id} withBorder radius="sm">
                    <Radio
                      value={option.id}
                      label={option.label}
                      description={`${option.wsUrl} | ${option.healthUrl}`}
                    />
                  </Card>
                ))}
              </Stack>
            </Radio.Group>

            <Stack gap={4}>
              <InfoLine label="Current WebSocket URL" value={settingsQuery.data.currentWsUrl || '-'} />
              <InfoLine label="Current Origin URL" value={settingsQuery.data.currentOriginUrl || '-'} />
              <InfoLine label="Current Health URL" value={settingsQuery.data.currentHealthUrl || '-'} />
            </Stack>

            <Group justify="flex-end">
              <Button
                leftSection={<Save size={18} />}
                disabled={!hasChanges}
                loading={updateMutation.isPending}
                onClick={() => updateMutation.mutate(selectedInstanceId)}
              >
                Save OpenClaw
              </Button>
            </Group>
          </Stack>
        </Card>
      ) : null}

      {hermesSettingsQuery.data ? (
        <Card withBorder radius="sm">
          <Stack gap="md">
            <Group justify="space-between" align="flex-start" gap="sm">
              <div>
                <Text fw={700}>Hermes Backend</Text>
                <Text size="sm" c="dimmed">Select which Hermes profile bot-engine uses for !hermes commands.</Text>
              </div>
              {hermesSettingsQuery.data.currentProfileId ? (
                <Badge color="green" leftSection={<CheckCircle2 size={12} />}>
                  {hermesSettingsQuery.data.currentProfileId}
                </Badge>
              ) : (
                <Badge color="yellow" leftSection={<AlertCircle size={12} />}>custom</Badge>
              )}
            </Group>

            <Radio.Group value={selectedHermesProfileId} onChange={setSelectedHermesProfileId}>
              <Stack gap="sm">
                {hermesSettingsQuery.data.options.map((option) => (
                  <Card key={option.id} withBorder radius="sm">
                    <Radio
                      value={option.id}
                      label={option.label}
                      description={`${option.baseUrl} | ${option.model} | ${option.apiMode}`}
                    />
                  </Card>
                ))}
              </Stack>
            </Radio.Group>

            <Stack gap={4}>
              <InfoLine label="Current Base URL" value={hermesSettingsQuery.data.baseUrl || '-'} />
              <InfoLine label="Current Model" value={hermesSettingsQuery.data.model || '-'} />
              <InfoLine label="Current API mode" value={hermesSettingsQuery.data.apiMode || '-'} />
              <InfoLine
                label="Current Timeout"
                value={hermesSettingsQuery.data.timeoutSeconds == null ? '-' : `${hermesSettingsQuery.data.timeoutSeconds}s`}
              />
              <InfoLine label="Current Health URL" value={hermesSettingsQuery.data.healthUrl || '-'} />
            </Stack>

            <Group justify="flex-end">
              <Button
                leftSection={<Save size={18} />}
                disabled={!hasHermesChanges}
                loading={updateHermesMutation.isPending}
                onClick={() => updateHermesMutation.mutate(selectedHermesProfileId)}
              >
                Save Hermes
              </Button>
            </Group>
          </Stack>
        </Card>
      ) : null}

      {hermesFallbackQuery.data ? (
        <Card withBorder radius="sm">
          <Stack gap="md">
            <div>
              <Text fw={700}>Hermes Ollama Fallback</Text>
              <Text size="sm" c="dimmed">
                Shared fallback used by chat, coder, and AI command profiles when OpenAI is unavailable.
              </Text>
            </div>

            <TextInput
              label="Ollama OpenAI-compatible base URL"
              value={fallbackBaseUrl}
              onChange={(event) => setFallbackBaseUrl(event.currentTarget.value)}
            />
            <Autocomplete
              label="Model"
              description="Select a discovered model or enter a custom model name."
              data={fallbackModels}
              value={fallbackModel}
              onChange={setFallbackModel}
            />
            <Group justify="flex-end">
              <Button
                variant="light"
                leftSection={<RefreshCw size={18} />}
                loading={fallbackModelsMutation.isPending}
                disabled={!fallbackBaseUrl}
                onClick={() => fallbackModelsMutation.mutate(fallbackBaseUrl)}
              >
                Load models
              </Button>
              <Button
                leftSection={<Save size={18} />}
                loading={updateFallbackMutation.isPending}
                disabled={!hasFallbackChanges}
                onClick={() => updateFallbackMutation.mutate()}
              >
                Validate and apply
              </Button>
            </Group>

            <Stack gap="xs">
              {hermesFallbackQuery.data.profiles.map((profile) => (
                <Group key={profile.profileId} justify="space-between" gap="sm" className="system-info-line">
                  <div>
                    <Text size="sm" fw={600}>{profile.profileId}</Text>
                    <Text size="xs" c="dimmed">{profile.detail}</Text>
                    {profile.cooldownUntil ? (
                      <Text size="xs" c="dimmed">Cooldown until {profile.cooldownUntil}</Text>
                    ) : null}
                  </div>
                  <Badge color={profile.healthy ? 'green' : 'red'}>{profile.expectedRoute}</Badge>
                </Group>
              ))}
            </Stack>
          </Stack>
        </Card>
      ) : null}
    </Stack>
  );
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
