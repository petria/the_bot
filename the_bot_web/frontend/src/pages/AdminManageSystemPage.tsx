import { Alert, Button, Card, Group, Loader, Stack, Switch, Text, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { getSystemSettings, saveSystemSettings, type SystemSettings } from '../api/adminSystemSettings';

export function AdminManageSystemPage() {
  const client = useQueryClient();
  const query = useQuery({ queryKey: ['admin-system-settings'], queryFn: getSystemSettings });
  const [draft, setDraft] = useState<SystemSettings | null>(null);
  const settings = draft ?? query.data;
  const save = useMutation({
    mutationFn: (settings: SystemSettings) => saveSystemSettings(settings),
    onSuccess: (response) => {
      client.setQueryData(['admin-system-settings'], response);
      setDraft(null);
    },
  });
  const changed = !!settings && !!query.data && settings.waterTemperatureToWatch !== query.data.waterTemperatureToWatch;
  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" className="field-row">
        <div>
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Configure system features for this bot instance.</Text>
        </div>
        <Button className="stack-fixed" leftSection={<Save size={18} />} loading={save.isPending}
          disabled={!changed || query.isError || save.isPending} onClick={() => settings && save.mutate(settings)}>Save</Button>
      </Group>
      {query.isLoading && <Loader />}
      {query.isError && <Alert color="red">{query.error.message}</Alert>}
      {save.isError && <Alert color="red">{save.error.message}</Alert>}
      {save.isSuccess && <Alert color="green">System settings saved.</Alert>}
      {settings && !query.isError && (
        <Card withBorder radius="sm">
          <Switch label="Water temperature to watch" onLabel="On" offLabel="Off"
            checked={settings.waterTemperatureToWatch} disabled={save.isPending}
            onChange={(event) => {
              save.reset();
              setDraft({ waterTemperatureToWatch: event.currentTarget.checked });
            }} />
        </Card>
      )}
    </Stack>
  );
}
