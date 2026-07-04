import { Alert, Badge, Button, Card, Group, Loader, NumberInput, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getMediaStorageSettings,
  updateMediaStorageSettings,
  type MediaStorageSettings,
} from '../api/adminMediaStorage';

const defaultSettings: MediaStorageSettings = {
  enabled: true,
  storageDir: '/runtime/media',
  publicUrlPrefix: 'http://localhost:8091/media',
  maxFileSizeMb: 25,
  retentionDays: 30,
  directoryExists: false,
  writable: false,
  detail: null,
};

export function AdminMediaStoragePage() {
  const queryClient = useQueryClient();
  const settingsQuery = useQuery({
    queryKey: ['admin-media-storage'],
    queryFn: getMediaStorageSettings,
  });
  const [settings, setSettings] = useState<MediaStorageSettings>(defaultSettings);

  useEffect(() => {
    if (settingsQuery.data) {
      setSettings(settingsQuery.data);
    }
  }, [settingsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: () => updateMediaStorageSettings({
      enabled: Boolean(settings.enabled),
      storageDir: settings.storageDir || '/runtime/media',
      maxFileSizeMb: settings.maxFileSizeMb || 25,
      retentionDays: settings.retentionDays || 30,
    }),
    onSuccess: (response) => {
      setSettings(response);
      queryClient.setQueryData(['admin-media-storage'], response);
    },
  });

  const hasChanges = Boolean(settingsQuery.data && JSON.stringify(settings) !== JSON.stringify(settingsQuery.data));

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>Manage Media Storage</Title>
          <Text c="dimmed">Configure persistent storage for chat images and future shareable media links.</Text>
        </div>
        <Button
          leftSection={<Save size={18} />}
          loading={saveMutation.isPending}
          disabled={!hasChanges}
          onClick={() => saveMutation.mutate()}
        >
          Save
        </Button>
      </Group>

      <Alert color="yellow" variant="light" icon={<AlertCircle size={18} />}>
        Media files are stored outside containers through the existing Docker runtime mount. For v1 the path must stay under /runtime.
      </Alert>

      {settingsQuery.isLoading ? <Loader /> : null}
      {settingsQuery.isError ? <SettingsError error={settingsQuery.error} /> : null}
      {saveMutation.isError ? <SettingsError error={saveMutation.error} /> : null}
      {saveMutation.isSuccess ? <Alert color="green" variant="light">Media storage settings saved.</Alert> : null}

      <Card withBorder radius="sm">
        <Stack gap="md">
          <Group justify="space-between" align="center">
            <Stack gap={2}>
              <Text fw={700}>Media storage</Text>
              <Text size="sm" c="dimmed">When enabled, bot services can use this directory for downloaded media.</Text>
            </Stack>
            <Switch
              label={settings.enabled ? 'Enabled' : 'Disabled'}
              checked={Boolean(settings.enabled)}
              onChange={(event) => updateSettings({ enabled: event.currentTarget.checked })}
            />
          </Group>

          <TextInput
            label="Storage directory"
            description="Container path. The host directory is the matching path under ~/the_bot/runtime."
            value={settings.storageDir || ''}
            onChange={(event) => updateSettings({ storageDir: event.currentTarget.value })}
          />

          <Group grow align="flex-start">
            <NumberInput
              label="Max file size"
              description="Megabytes"
              min={1}
              max={500}
              value={settings.maxFileSizeMb || 25}
              onChange={(value) => updateSettings({ maxFileSizeMb: Number(value) || 25 })}
            />
            <NumberInput
              label="Retention"
              description="Days"
              min={1}
              max={3650}
              value={settings.retentionDays || 30}
              onChange={(value) => updateSettings({ retentionDays: Number(value) || 30 })}
            />
          </Group>
        </Stack>
      </Card>

      <Card withBorder radius="sm">
        <Stack gap="sm">
          <Group justify="space-between">
            <Text fw={700}>Current status</Text>
            <Badge color={settings.writable ? 'green' : 'red'} variant="light">
              {settings.writable ? 'Writable' : 'Not writable'}
            </Badge>
          </Group>
          <Text size="sm">Public URL prefix: <Text span ff="monospace">{settings.publicUrlPrefix}</Text></Text>
          <Text size="sm">Directory exists: {settings.directoryExists ? 'yes' : 'no'}</Text>
          <Text size="sm">Host default: <Text span ff="monospace">~/the_bot/runtime/media</Text></Text>
          <Text size="sm">Container default: <Text span ff="monospace">/runtime/media</Text></Text>
          {settings.detail ? <Text size="sm" c="red">{settings.detail}</Text> : null}
        </Stack>
      </Card>
    </Stack>
  );

  function updateSettings(patch: Partial<MediaStorageSettings>) {
    setSettings((current) => ({ ...current, ...patch }));
  }
}

function SettingsError({ error }: { error: Error }) {
  if (error instanceof ApiError) {
    return (
      <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
        <Text fw={700}>Could not save media storage settings</Text>
        <Text>{error.message}</Text>
        {error.detail ? <Text size="sm">{error.detail}</Text> : null}
      </Alert>
    );
  }
  return (
    <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
      {error.message}
    </Alert>
  );
}
