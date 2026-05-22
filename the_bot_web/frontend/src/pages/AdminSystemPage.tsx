import { Alert, Badge, Button, Card, Group, Loader, Radio, Stack, Text, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { getOpenClawSettings, updateOpenClawSettings } from '../api/adminSystem';

export function AdminSystemPage() {
  const queryClient = useQueryClient();
  const settingsQuery = useQuery({
    queryKey: ['admin-system-openclaw'],
    queryFn: getOpenClawSettings,
  });
  const [selectedInstanceId, setSelectedInstanceId] = useState<string>('');

  useEffect(() => {
    if (settingsQuery.data?.currentInstanceId) {
      setSelectedInstanceId(settingsQuery.data.currentInstanceId);
    }
  }, [settingsQuery.data?.currentInstanceId]);

  const updateMutation = useMutation({
    mutationFn: updateOpenClawSettings,
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-system-openclaw'], response);
      queryClient.invalidateQueries({ queryKey: ['system-status'] });
    },
  });

  const hasChanges = Boolean(selectedInstanceId && selectedInstanceId !== settingsQuery.data?.currentInstanceId);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Runtime system settings used by bot-engine.</Text>
        </div>
        <Button
          leftSection={<Save size={18} />}
          disabled={!hasChanges}
          loading={updateMutation.isPending}
          onClick={() => updateMutation.mutate(selectedInstanceId)}
        >
          Save
        </Button>
      </Group>

      {settingsQuery.isLoading ? <Loader /> : null}
      {settingsQuery.isError ? <SettingsError error={settingsQuery.error} /> : null}
      {updateMutation.isError ? <SettingsError error={updateMutation.error} /> : null}

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
