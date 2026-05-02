import { Badge, Card, Group, Loader, SimpleGrid, Stack, Text, Title } from '@mantine/core';
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
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Overview</Title>
          <Text c="dimmed">Signed in as {me.username}</Text>
        </div>
        <Group gap="xs" wrap="wrap">
          {me.roles.map((role) => (
            <Badge key={role} variant={role === 'ROLE_ADMIN' ? 'filled' : 'light'}>
              {role.replace('ROLE_', '')}
            </Badge>
          ))}
        </Group>
      </Group>

      <Card withBorder radius="sm">
        <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }} spacing="md">
          <InfoItem label="Name" value={me.name} />
          <InfoItem label="Email" value={me.email} />
          <InfoItem label="IRC nick" value={me.ircNick} />
          <InfoItem label="Telegram id" value={me.telegramId} />
          <InfoItem label="Discord id" value={me.discordId} />
          <InfoItem label="IRC op" value={me.canDoIrcOp ? 'yes' : 'no'} />
        </SimpleGrid>
      </Card>
    </Stack>
  );
}

function InfoItem({ label, value }: { label: string; value: string | null }) {
  return (
    <Stack gap={2} className="info-item">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text className="info-value">{value || '-'}</Text>
    </Stack>
  );
}
