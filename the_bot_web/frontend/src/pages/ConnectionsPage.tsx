import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  SimpleGrid,
  Stack,
  Table,
  Text,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, RefreshCcw, RadioTower } from 'lucide-react';
import { getMe } from '../api/me';
import { ApiError } from '../api/client';
import {
  BotConnection,
  BotConnectionChannel,
  ChannelActivity,
  getConnectionsOverview,
  promoteConnectionChannel,
} from '../api/connections';
import { CONFIG_EDIT_PERMISSION, hasPermission } from '../permissions';

export function ConnectionsPage() {
  const queryClient = useQueryClient();
  const connectionsQuery = useQuery({
    queryKey: ['connections-overview'],
    queryFn: getConnectionsOverview,
    refetchInterval: 15000,
  });
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });
  const promoteMutation = useMutation({
    mutationFn: promoteConnectionChannel,
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-connection-config'], response);
      queryClient.invalidateQueries({ queryKey: ['connections-overview'] });
    },
  });

  const connections = connectionsQuery.data?.connections ?? [];
  const activities = connectionsQuery.data?.activities ?? [];
  const activityByAlias = new Map(
      activities
          .filter((activity) => activity.echoToAlias)
          .map((activity) => [activity.echoToAlias as string, activity]),
  );
  const channelCount = connections.reduce(
      (total, connection) => total + (connection.channels?.length ?? 0),
      0,
  );
  const activeChannelCount = activities.filter((activity) => activity.lastReceivedMessageAt).length;
  const canEditConfig = hasPermission(meQuery.data?.permissions, CONFIG_EDIT_PERMISSION);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Connections</Title>
          <Text c="dimmed">Configured chat connections and channels currently known by bot-io.</Text>
        </div>
        <Tooltip label="Refresh">
          <ActionIcon
            variant="light"
            size="lg"
            aria-label="Refresh connections"
            onClick={() => connectionsQuery.refetch()}
            loading={connectionsQuery.isFetching}
          >
            <RefreshCcw size={18} />
          </ActionIcon>
        </Tooltip>
      </Group>

      <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
        <StatCard label="Connections" value={connections.length.toString()} />
        <StatCard label="Channels" value={channelCount.toString()} />
        <StatCard label="Active channels" value={activeChannelCount.toString()} />
      </SimpleGrid>

      {promoteMutation.isSuccess && (
        <Alert color="green" variant="light" icon={<CheckCircle2 size={18} />}>
          Channel saved to connection configuration.
        </Alert>
      )}

      {promoteMutation.isError && (
        <Alert color="red" variant="light" icon={<AlertTriangle size={18} />}>
          {promoteErrorMessage(promoteMutation.error)}
        </Alert>
      )}

      {connectionsQuery.isLoading ? (
        <Loader />
      ) : connectionsQuery.isError ? (
        <ConnectionsError error={connectionsQuery.error} />
      ) : connections.length === 0 ? (
        <Card withBorder radius="sm">
          <Group gap="sm">
            <RadioTower size={20} />
            <Text>No connections found.</Text>
          </Group>
        </Card>
      ) : (
        <Stack gap="md">
          {connections.map((connection) => (
            <ConnectionCard
              key={connection.id}
              connection={connection}
              activityByAlias={activityByAlias}
              canPromote={canEditConfig}
              promoting={promoteMutation.isPending}
              onPromote={(channel) => promoteMutation.mutate({
                connectionType: connection.type,
                network: connection.network,
                channel: {
                  id: channel.id,
                  description: null,
                  name: channel.name,
                  type: channel.type,
                  echoToAlias: channel.echoToAlias,
                  echoToAliases: [],
                  joinOnStart: false,
                  publicAiEnabled: false,
                  allowAnonymousAiCommands: false,
                  resolveUrls: false,
                  alertMessages: false,
                  captureImages: false,
                  captureImageToAliases: [],
                },
              })}
            />
          ))}
        </Stack>
      )}
    </Stack>
  );
}

function ConnectionCard({
  connection,
  activityByAlias,
  canPromote,
  promoting,
  onPromote,
}: {
  connection: BotConnection;
  activityByAlias: Map<string, ChannelActivity>;
  canPromote: boolean;
  promoting: boolean;
  onPromote: (channel: BotConnectionChannel) => void;
}) {
  const channels = [...(connection.channels ?? [])].sort(compareChannels);

  return (
    <Card withBorder radius="sm" className="connections-card">
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" gap="sm">
          <Stack gap={2} className="connections-heading">
            <Group gap="xs" wrap="wrap">
              <Title order={3}>{connection.type || 'UNKNOWN_CONNECTION'}</Title>
              <Badge variant="light">{connection.network || 'unknown'}</Badge>
            </Group>
            <Text size="sm" c="dimmed">
              {channels.length} channels
            </Text>
          </Stack>
          <Badge variant="outline">connection #{connection.id}</Badge>
        </Group>

        {channels.length === 0 ? (
          <Text c="dimmed">No channels are known for this connection.</Text>
        ) : (
          <>
            <ChannelsTable
              channels={channels}
              activityByAlias={activityByAlias}
              canPromote={canPromote}
              promoting={promoting}
              onPromote={onPromote}
            />
            <ChannelsCards
              channels={channels}
              activityByAlias={activityByAlias}
              canPromote={canPromote}
              promoting={promoting}
              onPromote={onPromote}
            />
          </>
        )}
      </Stack>
    </Card>
  );
}

function ChannelsTable({
  channels,
  activityByAlias,
  canPromote,
  promoting,
  onPromote,
}: {
  channels: BotConnectionChannel[];
  activityByAlias: Map<string, ChannelActivity>;
  canPromote: boolean;
  promoting: boolean;
  onPromote: (channel: BotConnectionChannel) => void;
}) {
  return (
    <Table.ScrollContainer minWidth={760} className="connections-table">
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Channel</Table.Th>
            <Table.Th>Alias</Table.Th>
            <Table.Th>Type</Table.Th>
            <Table.Th>Last activity</Table.Th>
            <Table.Th>Config</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {channels.map((channel) => {
            const activity = activityFor(channel, activityByAlias);
            return (
              <Table.Tr key={channelKey(channel)}>
                <Table.Td><ChannelCell channel={channel} /></Table.Td>
                <Table.Td><AliasBadge value={channel.echoToAlias} /></Table.Td>
                <Table.Td>{channel.type || '-'}</Table.Td>
                <Table.Td><ActivityCell activity={activity} /></Table.Td>
                <Table.Td><ConfigCell channel={channel} canPromote={canPromote} promoting={promoting} onPromote={onPromote} /></Table.Td>
              </Table.Tr>
            );
          })}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function ChannelsCards({
  channels,
  activityByAlias,
  canPromote,
  promoting,
  onPromote,
}: {
  channels: BotConnectionChannel[];
  activityByAlias: Map<string, ChannelActivity>;
  canPromote: boolean;
  promoting: boolean;
  onPromote: (channel: BotConnectionChannel) => void;
}) {
  return (
    <Stack gap="sm" className="connections-cards">
      {channels.map((channel) => {
        const activity = activityFor(channel, activityByAlias);
        return (
          <Card withBorder radius="sm" key={channelKey(channel)}>
            <Stack gap="xs">
              <Group justify="space-between" align="flex-start" gap="xs">
                <ChannelCell channel={channel} />
                <AliasBadge value={channel.echoToAlias} />
              </Group>
              <InfoLine label="Type" value={channel.type || '-'} />
              <InfoLine label="Last activity" value={formatLastActivity(activity)} />
              <ConfigCell channel={channel} canPromote={canPromote} promoting={promoting} onPromote={onPromote} />
              {activity?.lastReceivedMessageBy && (
                <InfoLine label="By" value={activity.lastReceivedMessageBy} />
              )}
            </Stack>
          </Card>
        );
      })}
    </Stack>
  );
}

function ConfigCell({
  channel,
  canPromote,
  promoting,
  onPromote,
}: {
  channel: BotConnectionChannel;
  canPromote: boolean;
  promoting: boolean;
  onPromote: (channel: BotConnectionChannel) => void;
}) {
  if (channel.configured !== false && !channel.observedOnly) {
    return <Badge variant="light">Configured</Badge>;
  }

  return (
    <Group gap="xs" wrap="nowrap">
      <Badge color="yellow" variant="light">Observed only</Badge>
      {canPromote && (
        <Button
          size="compact-xs"
          variant="subtle"
          loading={promoting}
          disabled={!channel.echoToAlias}
          onClick={() => onPromote(channel)}
        >
          Add to config
        </Button>
      )}
    </Group>
  );
}

function ChannelCell({ channel }: { channel: BotConnectionChannel }) {
  return (
    <Stack gap={2} className="connections-cell">
      <Text fw={700} truncate>{channel.name || '-'}</Text>
      <Text size="xs" c="dimmed" truncate>{channel.id || channel.echoToAlias || '-'}</Text>
    </Stack>
  );
}

function ActivityCell({ activity }: { activity?: ChannelActivity }) {
  return (
    <Stack gap={2} className="connections-cell">
      <Text>{formatLastActivity(activity)}</Text>
      <Text size="xs" c="dimmed" truncate>
        {activity?.lastReceivedMessageBy || activity?.lastReceivedMessageSource || '-'}
      </Text>
    </Stack>
  );
}

function AliasBadge({ value }: { value: string | null }) {
  return (
    <Badge variant="light">
      {value || '-'}
    </Badge>
  );
}

function ConnectionsError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;

  return (
    <Card withBorder radius="sm">
      <Stack gap="xs">
        <Text c="red" fw={600}>{apiError?.message || 'Could not load connections from bot-io.'}</Text>
        {apiError?.backendUrl && (
          <InfoLine label="bot-io URL" value={apiError.backendUrl} />
        )}
        {apiError?.detail && (
          <Text size="sm" c="dimmed" className="info-value">{apiError.detail}</Text>
        )}
      </Stack>
    </Card>
  );
}

function promoteErrorMessage(error: Error) {
  if (error instanceof ApiError) {
    return error.detail || error.message || 'Could not save channel to configuration.';
  }
  return 'Could not save channel to configuration.';
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <Group justify="space-between" gap="sm" wrap="nowrap">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text size="sm" ta="right" className="info-value">{value || '-'}</Text>
    </Group>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <Card withBorder radius="sm">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text size="xl" fw={700}>{value}</Text>
    </Card>
  );
}

function activityFor(channel: BotConnectionChannel, activityByAlias: Map<string, ChannelActivity>) {
  if (!channel.echoToAlias) {
    return undefined;
  }
  return activityByAlias.get(channel.echoToAlias);
}

function formatLastActivity(activity?: ChannelActivity) {
  if (!activity?.lastReceivedMessageAt) {
    return 'never';
  }
  return formatTimestamp(activity.lastReceivedMessageAt);
}

function formatTimestamp(value: number) {
  const date = new Date(value);
  const day = padDatePart(date.getDate());
  const month = padDatePart(date.getMonth() + 1);
  const year = date.getFullYear();
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());
  const seconds = padDatePart(date.getSeconds());
  return `${day}.${month}.${year} ${hours}:${minutes}:${seconds}`;
}

function padDatePart(value: number) {
  return value.toString().padStart(2, '0');
}

function compareChannels(left: BotConnectionChannel, right: BotConnectionChannel) {
  return sortText(left.echoToAlias || '', right.echoToAlias || '')
      || sortText(left.name || '', right.name || '')
      || sortText(left.id || '', right.id || '');
}

function sortText(left: string, right: string) {
  return left.localeCompare(right, undefined, { sensitivity: 'base' });
}

function channelKey(channel: BotConnectionChannel) {
  return [
    channel.id,
    channel.echoToAlias,
    channel.name,
  ].join('|');
}
