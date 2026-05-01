import { Badge, Card, Group, Loader, Stack, Table, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { getMe } from '../api/me';

export function DashboardPage() {
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });

  if (meQuery.isLoading) {
    return <Loader />;
  }

  if (meQuery.isError) {
    return (
      <Card withBorder radius="sm">
        <Title order={2}>Session</Title>
        <Text c="red" mt="sm">Could not load logged-in user.</Text>
      </Card>
    );
  }

  const me = meQuery.data;
  if (!me) {
    return null;
  }

  return (
    <Stack gap="md">
      <Group justify="space-between">
        <div>
          <Title order={2}>Overview</Title>
          <Text c="dimmed">Signed in as {me.username}</Text>
        </div>
        <Group gap="xs">
          {me.roles.map((role) => (
            <Badge key={role} variant={role === 'ROLE_ADMIN' ? 'filled' : 'light'}>
              {role.replace('ROLE_', '')}
            </Badge>
          ))}
        </Group>
      </Group>

      <Card withBorder radius="sm">
        <Table withTableBorder={false}>
          <Table.Tbody>
            <InfoRow label="Name" value={me.name} />
            <InfoRow label="Email" value={me.email} />
            <InfoRow label="IRC nick" value={me.ircNick} />
            <InfoRow label="Telegram id" value={me.telegramId} />
            <InfoRow label="Discord id" value={me.discordId} />
            <InfoRow label="IRC op" value={me.canDoIrcOp ? 'yes' : 'no'} />
          </Table.Tbody>
        </Table>
      </Card>
    </Stack>
  );
}

function InfoRow({ label, value }: { label: string; value: string | null }) {
  return (
    <Table.Tr>
      <Table.Th w={160}>{label}</Table.Th>
      <Table.Td>{value || '-'}</Table.Td>
    </Table.Tr>
  );
}
