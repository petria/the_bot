import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Divider,
  Group,
  Loader,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useQuery } from '@tanstack/react-query';
import { RefreshCcw, Search, Users } from 'lucide-react';
import { useState } from 'react';
import { ApiError } from '../api/client';
import { getKnownUserTargets, KnownUserTarget } from '../api/knownUsers';

export function KnownUsersPage() {
  const [search, setSearch] = useState('');
  const [debouncedSearch] = useDebouncedValue(search, 250);
  const targetsQuery = useQuery({
    queryKey: ['known-user-targets', debouncedSearch],
    queryFn: () => getKnownUserTargets(debouncedSearch),
    refetchInterval: 15000,
  });

  const targets = targetsQuery.data ?? [];
  const groups = groupTargets(targets);
  const configuredCount = targets.filter((target) => target.matchedConfiguredUser).length;
  const privateCount = targets.filter((target) => target.targetType === 'PRIVATE').length;

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Known Users</Title>
          <Text c="dimmed">Resolved send targets observed by bot-io across IRC, Discord and Telegram.</Text>
        </div>
        <Tooltip label="Refresh">
          <ActionIcon
            variant="light"
            size="lg"
            aria-label="Refresh known users"
            onClick={() => targetsQuery.refetch()}
            loading={targetsQuery.isFetching}
          >
            <RefreshCcw size={18} />
          </ActionIcon>
        </Tooltip>
      </Group>

      <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
        <StatCard label="Targets" value={targets.length.toString()} />
        <StatCard label="Configured matches" value={configuredCount.toString()} />
        <StatCard label="Private targets" value={privateCount.toString()} />
      </SimpleGrid>

      <Group gap="sm" align="flex-end">
        <TextInput
          className="known-users-search"
          label="Search"
          placeholder="Name, nick, channel or network"
          leftSection={<Search size={16} />}
          value={search}
          onChange={(event) => setSearch(event.currentTarget.value)}
        />
        <Button variant="default" onClick={() => setSearch('')} disabled={!search}>
          Clear
        </Button>
      </Group>

      {targetsQuery.isLoading ? (
        <Loader />
      ) : targetsQuery.isError ? (
        <KnownUsersError error={targetsQuery.error} />
      ) : targets.length === 0 ? (
        <Card withBorder radius="sm">
          <Group gap="sm">
            <Users size={20} />
            <Text>No known user targets found.</Text>
          </Group>
        </Card>
      ) : (
        <KnownUserGroups groups={groups} />
      )}
    </Stack>
  );
}

function KnownUsersError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;

  return (
    <Card withBorder radius="sm">
      <Stack gap="xs">
        <Text c="red" fw={600}>{apiError?.message || 'Could not load known users from bot-io.'}</Text>
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

function KnownUserGroups({ groups }: { groups: ConnectionGroup[] }) {
  return (
    <Stack gap="md">
      {groups.map((connection) => (
        <Card withBorder radius="sm" key={connection.key} className="known-users-group">
          <Stack gap="md">
            <Group justify="space-between" align="flex-start" gap="sm">
              <Stack gap={2} className="known-users-heading">
                <Group gap="xs" wrap="wrap">
                  <Title order={3}>{connection.connectionType}</Title>
                  <Badge variant="light">{connection.network}</Badge>
                </Group>
                <Text size="sm" c="dimmed">
                  {connection.targetCount} targets in {connection.channels.length} channels
                </Text>
              </Stack>
              <Badge variant="outline">connection #{connection.connectionId}</Badge>
            </Group>

            <Stack gap="sm">
              {connection.channels.map((channel, index) => (
                <ChannelSection
                  key={channel.key}
                  channel={channel}
                  withDivider={index > 0}
                />
              ))}
            </Stack>
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function ChannelSection({ channel, withDivider }: { channel: ChannelGroup; withDivider: boolean }) {
  return (
    <Stack gap="sm">
      {withDivider && <Divider />}
      <Group justify="space-between" align="flex-start" gap="sm">
        <Stack gap={2} className="known-users-heading">
          <Group gap="xs" wrap="wrap">
            <Text fw={700}>{channel.echoToAlias}</Text>
            <Badge size="sm" variant="light">{channel.targetCount} targets</Badge>
          </Group>
          <Text size="sm" c="dimmed">{channel.channelName}</Text>
        </Stack>
        {channel.privateCount > 0 && <Badge>{channel.privateCount} private</Badge>}
      </Group>
      <ChannelTargetsTable targets={channel.targets} />
      <ChannelTargetsCards targets={channel.targets} />
    </Stack>
  );
}

function ChannelTargetsTable({ targets }: { targets: KnownUserTarget[] }) {
  return (
    <Table.ScrollContainer minWidth={760} className="known-users-table">
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>User</Table.Th>
            <Table.Th>Observed as</Table.Th>
            <Table.Th>Target</Table.Th>
            <Table.Th>Last seen</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {targets.map((target) => (
            <Table.Tr key={rowKey(target)}>
              <Table.Td><UserCell target={target} /></Table.Td>
              <Table.Td><ObservedCell target={target} /></Table.Td>
              <Table.Td><TargetType target={target} /></Table.Td>
              <Table.Td>{formatLastSeen(target.lastSeenAt)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function ChannelTargetsCards({ targets }: { targets: KnownUserTarget[] }) {
  return (
    <Stack gap="sm" className="known-users-cards">
      {targets.map((target) => (
        <Card withBorder radius="sm" key={rowKey(target)}>
          <Stack gap="xs">
            <Group justify="space-between" align="flex-start" gap="xs">
              <UserCell target={target} />
              <TargetType target={target} />
            </Group>
            <InfoLine label="Observed" value={observedName(target)} />
            <InfoLine label="Last seen" value={formatLastSeen(target.lastSeenAt)} />
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function UserCell({ target }: { target: KnownUserTarget }) {
  return (
    <Stack gap={2} className="known-users-cell">
      <Group gap="xs" wrap="nowrap">
        <Text fw={700} truncate>{target.configuredName || target.configuredUsername || target.logicalUserKey || '-'}</Text>
        {target.matchedConfiguredUser && <Badge size="xs" variant="light">configured</Badge>}
      </Group>
      <Text size="xs" c="dimmed" truncate>{target.matchSource || target.logicalUserKey || '-'}</Text>
    </Stack>
  );
}

function ObservedCell({ target }: { target: KnownUserTarget }) {
  return (
    <Stack gap={2} className="known-users-cell">
      <Text truncate>{observedName(target)}</Text>
      <Text size="xs" c="dimmed" truncate>{target.observedUserKey || '-'}</Text>
    </Stack>
  );
}

function TargetType({ target }: { target: KnownUserTarget }) {
  const type = target.targetType || 'UNKNOWN';
  return (
    <Badge variant={type === 'PRIVATE' ? 'filled' : 'light'}>
      {type.toLowerCase()}
    </Badge>
  );
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

function observedName(target: KnownUserTarget) {
  return target.observedDisplayName || target.observedUsername || target.observedUserId || '-';
}

function formatLastSeen(value: number | null) {
  if (!value) {
    return 'never';
  }
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'medium',
  }).format(new Date(value));
}

function rowKey(target: KnownUserTarget) {
  return [
    target.logicalUserKey,
    target.observedUserKey,
    target.connectionId,
    target.echoToAlias,
    target.targetType,
  ].join('|');
}

type ConnectionGroup = {
  key: string;
  connectionId: number;
  connectionType: string;
  network: string;
  channels: ChannelGroup[];
  targetCount: number;
};

type ChannelGroup = {
  key: string;
  echoToAlias: string;
  channelName: string;
  targets: KnownUserTarget[];
  targetCount: number;
  privateCount: number;
};

function groupTargets(targets: KnownUserTarget[]): ConnectionGroup[] {
  const connections = new Map<string, ConnectionGroup>();

  for (const target of targets) {
    const connectionType = target.connectionType || 'UNKNOWN_CONNECTION';
    const network = target.network || 'unknown';
    const connectionKey = [
      target.connectionId,
      connectionType,
      network,
    ].join('|');
    let connection = connections.get(connectionKey);
    if (!connection) {
      connection = {
        key: connectionKey,
        connectionId: target.connectionId,
        connectionType,
        network,
        channels: [],
        targetCount: 0,
      };
      connections.set(connectionKey, connection);
    }

    const channelKey = [
      target.echoToAlias || '',
      target.channelId || '',
      target.channelName || '',
    ].join('|');
    let channel = connection.channels.find((candidate) => candidate.key === channelKey);
    if (!channel) {
      channel = {
        key: channelKey,
        echoToAlias: target.echoToAlias || '-',
        channelName: target.channelName || target.channelId || '-',
        targets: [],
        targetCount: 0,
        privateCount: 0,
      };
      connection.channels.push(channel);
    }

    channel.targets.push(target);
    channel.targetCount += 1;
    channel.privateCount += target.targetType === 'PRIVATE' ? 1 : 0;
    connection.targetCount += 1;
  }

  return Array.from(connections.values())
      .sort((left, right) => sortText(left.connectionType, right.connectionType)
          || sortText(left.network, right.network)
          || left.connectionId - right.connectionId)
      .map((connection) => ({
        ...connection,
        channels: connection.channels
            .sort((left, right) => sortText(left.echoToAlias, right.echoToAlias)
                || sortText(left.channelName, right.channelName))
            .map((channel) => ({
              ...channel,
              targets: channel.targets.sort(compareTargets),
            })),
      }));
}

function compareTargets(left: KnownUserTarget, right: KnownUserTarget) {
  return sortText(userSortName(left), userSortName(right))
      || sortText(observedName(left), observedName(right))
      || sortText(left.targetType || '', right.targetType || '');
}

function userSortName(target: KnownUserTarget) {
  return target.configuredName || target.configuredUsername || target.logicalUserKey || '';
}

function sortText(left: string, right: string) {
  return left.localeCompare(right, undefined, { sensitivity: 'base' });
}
