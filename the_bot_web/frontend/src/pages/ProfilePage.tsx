import { Alert, Button, Card, Group, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { getMe, updateProfile, type MeResponse, type ProfileUpdateRequest } from '../api/me';

const emptyProfile: ProfileUpdateRequest = {
  name: '',
  email: '',
  ircNick: '',
  telegramId: '',
  discordId: '',
};

export function ProfilePage() {
  const queryClient = useQueryClient();
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });
  const [profile, setProfile] = useState<ProfileUpdateRequest>(emptyProfile);

  useEffect(() => {
    if (meQuery.data) {
      setProfile(toProfileForm(meQuery.data));
    }
  }, [meQuery.data]);

  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(['me'], updated);
    },
  });

  const setField = (field: keyof ProfileUpdateRequest, value: string) => {
    setProfile((current) => ({ ...current, [field]: value }));
  };

  if (meQuery.isError) {
    const error = meQuery.error;
    return (
      <Alert color="red" variant="light">
        {error instanceof ApiError && error.authenticationRequired
          ? 'Sign in to edit your profile.'
          : 'Could not load your profile.'}
      </Alert>
    );
  }

  return (
    <Stack gap="md">
      <div>
        <Title order={2}>Profile</Title>
        <Text c="dimmed">Update the bot user identity linked to your web login.</Text>
      </div>

      <Card withBorder radius="sm">
        <Stack gap="md">
          <Group grow align="flex-start" className="profile-grid">
            <TextInput
              label="Name"
              value={profile.name}
              onChange={(event) => setField('name', event.currentTarget.value)}
            />
            <TextInput
              label="Email"
              type="email"
              value={profile.email}
              onChange={(event) => setField('email', event.currentTarget.value)}
            />
          </Group>
          <Group grow align="flex-start" className="profile-grid">
            <TextInput
              label="IRC nick"
              value={profile.ircNick}
              onChange={(event) => setField('ircNick', event.currentTarget.value)}
            />
            <TextInput
              label="Telegram id"
              value={profile.telegramId}
              onChange={(event) => setField('telegramId', event.currentTarget.value)}
            />
            <TextInput
              label="Discord id"
              value={profile.discordId}
              onChange={(event) => setField('discordId', event.currentTarget.value)}
            />
          </Group>

          {updateMutation.isError && (
            <Alert color="red" variant="light">Could not save profile.</Alert>
          )}
          {updateMutation.isSuccess && (
            <Alert color="green" variant="light">Profile saved.</Alert>
          )}

          <Group justify="flex-end">
            <Button
              leftSection={<Save size={18} />}
              loading={updateMutation.isPending}
              onClick={() => updateMutation.mutate(profile)}
            >
              Save profile
            </Button>
          </Group>
        </Stack>
      </Card>
    </Stack>
  );
}

function toProfileForm(me: MeResponse): ProfileUpdateRequest {
  return {
    name: me.name ?? '',
    email: me.email ?? '',
    ircNick: me.ircNick ?? '',
    telegramId: me.telegramId ?? '',
    discordId: me.discordId ?? '',
  };
}
