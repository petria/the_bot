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
          <Text c="dimmed">Live bot component status.</Text>
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
  const degraded = component.status === 'DEGRADED';
  const sidecar = component.componentType === 'SIDECAR';
  const springBoot = component.componentType === 'SPRING_BOOT';
  const openClawGateway = component.componentType === 'OPENCLAW_GATEWAY';
  const hermesGateway = component.componentType === 'HERMES_GATEWAY';
  const hermesManager = component.componentType === 'HERMES_MANAGER';
  const gateway = openClawGateway || hermesGateway || hermesManager;
  const showContainer = Boolean(component.containerName || component.containerState || component.image || component.containerError);
  return (
    <Card withBorder radius="sm" className="system-card">
      <Stack gap="sm">
        <Group justify="space-between" align="flex-start" gap="sm">
          <Group gap="sm" className="system-card-title">
            <Server size={22} />
            <div>
              <Text fw={700}>{component.name}</Text>
              <Text size="xs" c="dimmed">{component.artifact || component.componentType || '-'}</Text>
            </div>
          </Group>
          <Badge
            color={up ? 'green' : degraded ? 'yellow' : 'red'}
            variant={up ? 'filled' : 'light'}
            leftSection={up ? <CheckCircle2 size={12} /> : <AlertCircle size={12} />}
          >
            {component.status || 'UNKNOWN'}
          </Badge>
        </Group>

        <Stack gap={6}>
          <InfoLine label="Type" value={formatComponentType(component.componentType)} />
          {component.runtimeMode ? <InfoLine label="Mode" value={component.runtimeMode} /> : null}
          {!sidecar ? <InfoLine label={gateway ? 'Gateway' : 'Base URL'} value={component.baseUrl || '-'} /> : null}
          {component.healthUrl ? <InfoLine label="Health URL" value={component.healthUrl} /> : null}
          {component.healthStatus ? <InfoLine label="Health" value={component.healthStatus} /> : null}
          {hermesGateway || hermesManager ? <InfoLine label="Model" value={component.artifact || '-'} /> : null}
          {hermesGateway || hermesManager ? <InfoLine label={hermesManager ? 'Route' : 'API mode'} value={component.profiles || '-'} /> : null}
          {hermesGateway ? <InfoLine label="Timeout" value={component.version || '-'} /> : null}
          {springBoot ? <InfoLine label="Version" value={component.version || '-'} /> : null}
          {springBoot ? <InfoLine label="Profile" value={component.profiles || '-'} /> : null}
          <InfoLine label="Uptime" value={formatDuration(component.uptimeSeconds)} />
          <InfoLine label="Started" value={formatDateTime(component.startedAt)} />
          {springBoot ? <InfoLine label="Received" value={formatCount(component.receivedCalls)} /> : null}
          {springBoot ? <InfoLine label="Requested" value={formatCount(component.requestedCalls)} /> : null}
          {showContainer ? <InfoLine label="Container" value={component.containerName || '-'} /> : null}
          {showContainer ? <InfoLine label="Docker" value={formatDockerStatus(component)} /> : null}
          {showContainer ? <InfoLine label="Image" value={component.image || '-'} /> : null}
          {showContainer ? <InfoLine label="Restarts" value={formatCount(component.restartCount)} /> : null}
          <InfoLine label="Response" value={formatResponseTime(component.responseTimeMs)} />
          <InfoLine label="Checked" value={formatDateTime(component.checkedAt)} />
        </Stack>

        {component.containerError ? (
          <Alert color="yellow" icon={<AlertCircle size={18} />}>
            <Text size="sm">{component.containerError}</Text>
          </Alert>
        ) : null}

        {component.error && component.error !== component.containerError ? (
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

function formatComponentType(type: string | null) {
  if (type === 'SPRING_BOOT') {
    return 'Spring Boot';
  }
  if (type === 'SIDECAR') {
    return 'Sidecar';
  }
  if (type === 'OPENCLAW_GATEWAY') {
    return 'OpenClaw Gateway';
  }
  if (type === 'HERMES_GATEWAY') {
    return 'Hermes Gateway';
  }
  if (type === 'HERMES_MANAGER') {
    return 'Hermes Manager';
  }
  return type || '-';
}

function formatDockerStatus(component: SystemComponentStatus) {
  if (!component.containerState) {
    return '-';
  }
  return component.containerStatusText
    ? `${component.containerState} (${component.containerStatusText})`
    : component.containerState;
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
