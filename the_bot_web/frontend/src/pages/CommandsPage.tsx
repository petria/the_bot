import {
  ActionIcon,
  Alert,
  Badge,
  Card,
  Group,
  Loader,
  Stack,
  Table,
  Text,
  Title,
  Tooltip,
} from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, ListTree, RefreshCcw } from 'lucide-react';
import { ApiError } from '../api/client';
import { CommandInfo, CommandProviderInfo, getCommands } from '../api/commands';

export function CommandsPage() {
  const commandsQuery = useQuery({
    queryKey: ['commands'],
    queryFn: getCommands,
  });

  const providers = [...(commandsQuery.data?.providers ?? [])]
      .sort((left, right) => sortText(left.namespace || '', right.namespace || ''));
  const commandCount = providers.reduce((total, provider) => total + (provider.commands?.length ?? 0), 0);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Commands</Title>
          <Text c="dimmed">Registered bot-engine command providers and command handlers.</Text>
        </div>
        <Tooltip label="Refresh">
          <ActionIcon
            variant="light"
            size="lg"
            aria-label="Refresh commands"
            onClick={() => commandsQuery.refetch()}
            loading={commandsQuery.isFetching}
          >
            <RefreshCcw size={18} />
          </ActionIcon>
        </Tooltip>
      </Group>

      {commandsQuery.isLoading ? (
        <Loader />
      ) : commandsQuery.isError ? (
        <CommandsError error={commandsQuery.error} />
      ) : providers.length === 0 ? (
        <Card withBorder radius="sm">
          <Group gap="sm">
            <ListTree size={20} />
            <Text>No command providers found.</Text>
          </Group>
        </Card>
      ) : (
        <Stack gap="md">
          <Group gap="xs">
            <Badge variant="light">{providers.length} providers</Badge>
            <Badge variant="light">{commandCount} commands</Badge>
          </Group>
          {providers.map((provider) => (
            <ProviderCard key={provider.namespace || provider.displayName} provider={provider} />
          ))}
        </Stack>
      )}
    </Stack>
  );
}

function ProviderCard({ provider }: { provider: CommandProviderInfo }) {
  const commands = [...(provider.commands ?? [])]
      .sort((left, right) => sortText(left.trigger || '', right.trigger || ''));

  return (
    <Card withBorder radius="sm">
      <Stack gap="md">
        <Group justify="space-between" align="flex-start" gap="sm">
          <Stack gap={2}>
            <Group gap="xs" wrap="wrap">
              <Title order={3}>{provider.displayName || provider.namespace || 'Unknown provider'}</Title>
              <Badge variant="outline">{provider.namespace}</Badge>
            </Group>
            {provider.description && (
              <Text size="sm" c="dimmed">{provider.description}</Text>
            )}
          </Stack>
          <Badge variant="light">{commands.length} commands</Badge>
        </Group>

        {commands.length === 0 ? (
          <Text c="dimmed">No commands registered for this provider.</Text>
        ) : (
          <>
            <CommandsTable commands={commands} />
            <CommandsCards commands={commands} />
          </>
        )}
      </Stack>
    </Card>
  );
}

function CommandsTable({ commands }: { commands: CommandInfo[] }) {
  return (
    <Table.ScrollContainer minWidth={860} className="connections-table">
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Command</Table.Th>
            <Table.Th>Aliases</Table.Th>
            <Table.Th>Permission</Table.Th>
            <Table.Th>Class</Table.Th>
            <Table.Th>Help</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {commands.map((command) => (
            <Table.Tr key={command.trigger || command.className}>
              <Table.Td><CommandTrigger command={command} /></Table.Td>
              <Table.Td><Aliases command={command} /></Table.Td>
              <Table.Td><PermissionBadge permission={command.requiredPermission} /></Table.Td>
              <Table.Td>
                <Text size="sm" ff="monospace">{shortClassName(command.className)}</Text>
              </Table.Td>
              <Table.Td>
                <Text size="sm" lineClamp={2}>{command.help || '-'}</Text>
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function CommandsCards({ commands }: { commands: CommandInfo[] }) {
  return (
    <Stack gap="sm" hiddenFrom="sm">
      {commands.map((command) => (
        <Card key={command.trigger || command.className} withBorder radius="sm" p="sm">
          <Stack gap="xs">
            <Group justify="space-between" gap="xs" align="flex-start">
              <CommandTrigger command={command} />
              <PermissionBadge permission={command.requiredPermission} />
            </Group>
            <Aliases command={command} />
            <Text size="sm" c="dimmed" ff="monospace">{shortClassName(command.className)}</Text>
            {command.help && <Text size="sm">{command.help}</Text>}
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function CommandTrigger({ command }: { command: CommandInfo }) {
  return (
    <Stack gap={2}>
      <Text fw={700} ff="monospace">{command.trigger || command.displayName || command.commandName}</Text>
      {command.displayName && command.displayName !== command.trigger && (
        <Text size="xs" c="dimmed">{command.displayName}</Text>
      )}
    </Stack>
  );
}

function Aliases({ command }: { command: CommandInfo }) {
  const aliases = command.aliases ?? [];
  if (aliases.length === 0) {
    return <Text size="sm" c="dimmed">-</Text>;
  }
  return (
    <Group gap={4} wrap="wrap">
      {aliases.map((alias) => (
        <Tooltip
          key={`${alias.alias}-${alias.target}`}
          label={`${alias.target || ''}${alias.withArgs ? ' with args' : ''}`}
        >
          <Badge variant="light" ff="monospace">{alias.alias}</Badge>
        </Tooltip>
      ))}
    </Group>
  );
}

function PermissionBadge({ permission }: { permission?: string | null }) {
  if (!permission) {
    return <Badge variant="light" color="green">public</Badge>;
  }
  return <Badge variant="light" color="orange">{permission}</Badge>;
}

function CommandsError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" variant="light" icon={<AlertTriangle size={18} />} title="Could not load commands">
      <Stack gap={4}>
        <Text>{apiError?.message || error.message}</Text>
        {apiError?.detail && <Text size="sm" c="dimmed">{apiError.detail}</Text>}
      </Stack>
    </Alert>
  );
}

function shortClassName(className?: string | null) {
  if (!className) {
    return '-';
  }
  const idx = className.lastIndexOf('.');
  return idx < 0 ? className : className.substring(idx + 1);
}

function sortText(left: string, right: string) {
  return left.localeCompare(right, undefined, { sensitivity: 'base' });
}
