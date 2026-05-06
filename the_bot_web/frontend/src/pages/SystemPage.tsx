import { Alert, Badge, Button, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, RefreshCw, Server } from 'lucide-react';
import { ApiError } from '../api/client';
import { getSystemStatus, type SystemComponentStatus } from '../api/system';

export function SystemPage() {
  const systemQuery = useQuery({
    queryKey: ['system-status'],
    queryFn: getSystemStatus,
    refetchInterval: 5000,
  });

  const components = systemQuery.data?.components ?? [];

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>System</Title>
          <Text c="dimmed">Live Spring Boot application status.</Text>
        </div>
        <Button
          variant="light"
          leftSection={<RefreshCw size={18} />}
          onClick={() => systemQuery.refetch()}
          loading={systemQuery.isFetching}
        >
          Refresh
        </Button>
      </Group>

      {systemQuery.isLoading ? <Loader /> : null}
      {systemQuery.isError ? <SystemError error={systemQuery.error} /> : null}

      {!systemQuery.isLoading && !systemQuery.isError ? (
        <>
          <Text size="sm" c="dimmed">
            Last checked {formatDateTime(systemQuery.data?.checkedAt)}
            {systemQuery.isFetching ? ' - refreshing' : ''}
          </Text>
          <SimpleGrid cols={{ base: 1, md: 3 }} spacing="md">
            {components.map((component) => (
              <SystemComponentCard key={component.name} component={component} />
            ))}
          </SimpleGrid>
        </>
      ) : null}
    </Stack>
  );
}

function SystemComponentCard({ component }: { component: SystemComponentStatus }) {
  const up = component.status === 'UP';
  return (
    <Card withBorder radius="sm" className="system-card">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="sm">
          <Group gap="sm" className="system-card-title">
            <Server size={22} />
            <div>
              <Text fw={700}>{component.name}</Text>
              <Text size="xs" c="dimmed">{component.artifact || '-'}</Text>
            </div>
          </Group>
          <Badge
            color={up ? 'green' : 'red'}
            variant={up ? 'filled' : 'light'}
            leftSection={up ? <CheckCircle2 size={12} /> : <AlertCircle size={12} />}
          >
            {component.status || 'UNKNOWN'}
          </Badge>
        </Group>

        <Stack gap={6}>
          <InfoLine label="Base URL" value={component.baseUrl || '-'} />
          <InfoLine label="Version" value={component.version || '-'} />
          <InfoLine label="Profile" value={component.profiles || '-'} />
          <InfoLine label="Uptime" value={formatDuration(component.uptimeSeconds)} />
          <InfoLine label="Started" value={formatDateTime(component.startedAt)} />
          <InfoLine label="Received" value={formatCount(component.receivedCalls)} />
          <InfoLine label="Requested" value={formatCount(component.requestedCalls)} />
          <InfoLine label="Response" value={formatResponseTime(component.responseTimeMs)} />
          <InfoLine label="Checked" value={formatDateTime(component.checkedAt)} />
        </Stack>

        {component.error ? (
          <Alert color="red" icon={<AlertCircle size={18} />}>
            <Text size="sm">{component.error}</Text>
          </Alert>
        ) : null}
      </Stack>
    </Card>
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

function SystemError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError?.message || 'Could not load system status.'}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}

function formatDuration(seconds: number | null) {
  if (seconds == null) {
    return '-';
  }
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) {
    return `${days}d ${hours}h ${minutes}m`;
  }
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  return `${minutes}m`;
}

function formatResponseTime(responseTimeMs: number | null) {
  return responseTimeMs == null ? '-' : `${responseTimeMs} ms`;
}

function formatCount(count: number | null) {
  return count == null ? '-' : count.toLocaleString('fi-FI');
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('fi-FI', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date);
}
