import { Alert, Button, Card, Group, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { KeyRound, Save } from 'lucide-react';
import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import {
  changePassword,
  getMe,
  updateProfile,
  type MeResponse,
  type PasswordChangeRequest,
  type ProfileUpdateRequest,
} from '../api/me';

const emptyProfile: ProfileUpdateRequest = {
  name: '',
  email: '',
  ircNick: '',
  telegramId: '',
  discordId: '',
};

const emptyPasswordChange: PasswordChangeRequest = {
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
};

export function ProfilePage() {
  const queryClient = useQueryClient();
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });
  const [profile, setProfile] = useState<ProfileUpdateRequest>(emptyProfile);
  const [passwordChange, setPasswordChange] = useState<PasswordChangeRequest>(emptyPasswordChange);
  const [passwordValidationError, setPasswordValidationError] = useState<string | null>(null);

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

  const passwordMutation = useMutation({
    mutationFn: changePassword,
    onSuccess: () => {
      setPasswordChange(emptyPasswordChange);
      setPasswordValidationError(null);
    },
  });

  const setField = (field: keyof ProfileUpdateRequest, value: string) => {
    setProfile((current) => ({ ...current, [field]: value }));
  };

  const setPasswordField = (field: keyof PasswordChangeRequest, value: string) => {
    setPasswordValidationError(null);
    passwordMutation.reset();
    setPasswordChange((current) => ({ ...current, [field]: value }));
  };

  const submitPasswordChange = () => {
    if (passwordChange.newPassword.length < 10) {
      setPasswordValidationError('New password must be at least 10 characters.');
      return;
    }
    if (passwordChange.newPassword !== passwordChange.confirmNewPassword) {
      setPasswordValidationError('New passwords do not match.');
      return;
    }
    passwordMutation.mutate(passwordChange);
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

      <Card withBorder radius="sm">
        <Stack gap="md">
          <div>
            <Title order={3}>Change Password</Title>
            <Text c="dimmed" size="sm">Update the password used for web login.</Text>
          </div>

          <Stack gap="sm">
            <PasswordInput
              label="Current password"
              autoComplete="current-password"
              value={passwordChange.currentPassword}
              onChange={(event) => setPasswordField('currentPassword', event.currentTarget.value)}
            />
            <Group grow align="flex-start" className="profile-grid">
              <PasswordInput
                label="New password"
                autoComplete="new-password"
                value={passwordChange.newPassword}
                onChange={(event) => setPasswordField('newPassword', event.currentTarget.value)}
              />
              <PasswordInput
                label="Confirm new password"
                autoComplete="new-password"
                value={passwordChange.confirmNewPassword}
                onChange={(event) => setPasswordField('confirmNewPassword', event.currentTarget.value)}
              />
            </Group>
          </Stack>

          {passwordValidationError && (
            <Alert color="red" variant="light">{passwordValidationError}</Alert>
          )}
          {passwordMutation.isError && (
            <Alert color="red" variant="light">Could not change password.</Alert>
          )}
          {passwordMutation.isSuccess && (
            <Alert color="green" variant="light">Password changed.</Alert>
          )}

          <Group justify="flex-end">
            <Button
              leftSection={<KeyRound size={18} />}
              loading={passwordMutation.isPending}
              disabled={!passwordChange.currentPassword || !passwordChange.newPassword || !passwordChange.confirmNewPassword}
              onClick={submitPasswordChange}
            >
              Change password
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
