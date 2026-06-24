import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Code,
  Group,
  NumberInput,
  PasswordInput,
  Select,
  Stack,
  Switch,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, KeyRound, Link, Pencil, Plus, Save, Trash2, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import { getConnectionsOverview } from '../api/connections';
import {
  changePassword,
  createIrcClaimToken,
  createNotifyRule,
  deleteNotifyRule,
  getMe,
  getNotifyRules,
  updateNotifyRule,
  updateProfile,
  type IrcClaimTokenResponse,
  type MeResponse,
  type NotifyPatternType,
  type PasswordChangeRequest,
  type ProfileUpdateRequest,
  type UserNotifyRule,
  type UserNotifyRuleInput,
} from '../api/me';

const emptyProfile: ProfileUpdateRequest = {
  name: '',
  email: '',
  ircNick: '',
  telegramId: '',
  discordId: '',
  whatsappId: '',
};

const emptyPasswordChange: PasswordChangeRequest = {
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
};

const emptyNotifyRule: UserNotifyRuleInput = {
  enabled: true,
  sourceEchoToAlias: '',
  sourceDisplayName: '',
  patternType: 'PRESET_MENTION',
  pattern: '',
  destinationConnectionType: 'WHATSAPP_CONNECTION',
  cooldownSeconds: 60,
};

export function ProfilePage() {
  const queryClient = useQueryClient();
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });
  const notifyRulesQuery = useQuery({
    queryKey: ['notify-rules'],
    queryFn: getNotifyRules,
  });
  const connectionsQuery = useQuery({
    queryKey: ['connections-overview'],
    queryFn: getConnectionsOverview,
  });
  const [profile, setProfile] = useState<ProfileUpdateRequest>(emptyProfile);
  const [passwordChange, setPasswordChange] = useState<PasswordChangeRequest>(emptyPasswordChange);
  const [passwordValidationError, setPasswordValidationError] = useState<string | null>(null);
  const [ircClaimToken, setIrcClaimToken] = useState<IrcClaimTokenResponse | null>(null);
  const [notifyForm, setNotifyForm] = useState<UserNotifyRuleInput>(emptyNotifyRule);
  const [editingNotifyRuleId, setEditingNotifyRuleId] = useState<string | null>(null);
  const [notifyValidationError, setNotifyValidationError] = useState<string | null>(null);

  const sourceOptions = useMemo(() => {
    const seen = new Set<string>();
    return (connectionsQuery.data?.connections ?? [])
        .flatMap((connection) => connection.channels ?? [])
        .filter((channel) => Boolean(channel.echoToAlias))
        .map((channel) => {
          const alias = channel.echoToAlias || '';
          const label = [channel.type, channel.network, channel.name || channel.id, alias]
              .filter(Boolean)
              .join(' / ');
          return { value: alias, label };
        })
        .filter((option) => {
          if (seen.has(option.value)) {
            return false;
          }
          seen.add(option.value);
          return true;
        })
        .sort((left, right) => left.label.localeCompare(right.label, undefined, { sensitivity: 'base' }));
  }, [connectionsQuery.data]);

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

  const ircClaimMutation = useMutation({
    mutationFn: createIrcClaimToken,
    onSuccess: (created) => setIrcClaimToken(created),
  });

  const saveNotifyRuleMutation = useMutation({
    mutationFn: (rule: UserNotifyRuleInput) => editingNotifyRuleId
        ? updateNotifyRule(editingNotifyRuleId, rule)
        : createNotifyRule(rule),
    onSuccess: () => {
      setNotifyForm(emptyNotifyRule);
      setEditingNotifyRuleId(null);
      setNotifyValidationError(null);
      queryClient.invalidateQueries({ queryKey: ['notify-rules'] });
    },
  });

  const deleteNotifyRuleMutation = useMutation({
    mutationFn: deleteNotifyRule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notify-rules'] });
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

  const setNotifyField = <K extends keyof UserNotifyRuleInput>(field: K, value: UserNotifyRuleInput[K]) => {
    setNotifyValidationError(null);
    saveNotifyRuleMutation.reset();
    setNotifyForm((current) => ({ ...current, [field]: value }));
  };

  const selectSourceAlias = (alias: string | null) => {
    const selected = sourceOptions.find((option) => option.value === alias);
    setNotifyForm((current) => ({
      ...current,
      sourceEchoToAlias: alias ?? '',
      sourceDisplayName: selected?.label ?? current.sourceDisplayName,
    }));
  };

  const editNotifyRule = (rule: UserNotifyRule) => {
    setEditingNotifyRuleId(rule.id);
    setNotifyForm({
      enabled: rule.enabled,
      sourceEchoToAlias: rule.sourceEchoToAlias,
      sourceDisplayName: rule.sourceDisplayName ?? '',
      patternType: rule.patternType,
      pattern: rule.pattern ?? '',
      destinationConnectionType: rule.destinationConnectionType,
      cooldownSeconds: rule.cooldownSeconds || 60,
    });
    setNotifyValidationError(null);
  };

  const cancelNotifyEdit = () => {
    setEditingNotifyRuleId(null);
    setNotifyForm(emptyNotifyRule);
    setNotifyValidationError(null);
  };

  const submitNotifyRule = () => {
    const sourceEchoToAlias = notifyForm.sourceEchoToAlias.trim();
    if (!sourceEchoToAlias) {
      setNotifyValidationError('Source channel is required.');
      return;
    }
    if (notifyForm.patternType === 'REGEX' && !notifyForm.pattern?.trim()) {
      setNotifyValidationError('Regex pattern is required.');
      return;
    }
    saveNotifyRuleMutation.mutate({
      ...notifyForm,
      sourceEchoToAlias,
      sourceDisplayName: notifyForm.sourceDisplayName?.trim() || sourceEchoToAlias,
      pattern: notifyForm.patternType === 'REGEX' ? notifyForm.pattern?.trim() ?? '' : '',
      cooldownSeconds: Math.max(1, Math.min(86400, notifyForm.cooldownSeconds || 60)),
    });
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
            <TextInput
              label="WhatsApp id"
              value={profile.whatsappId}
              onChange={(event) => setField('whatsappId', event.currentTarget.value)}
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
            <Title order={3}>IRC Identity Claim</Title>
            <Text c="dimmed" size="sm">Create a one-time token and send it to the bot in an IRC private message.</Text>
          </div>

          {ircClaimMutation.isError && (
            <Alert color="red" variant="light">Could not create IRC claim token.</Alert>
          )}
          {ircClaimToken && (
            <Alert color="blue" variant="light">
              Send <Code>!claim {ircClaimToken.token}</Code> to the bot in IRC private message before{' '}
              {formatExpiresAt(ircClaimToken.expiresAt)}.
            </Alert>
          )}

          <Group justify="flex-end">
            <Button
              leftSection={<Link size={18} />}
              loading={ircClaimMutation.isPending}
              onClick={() => ircClaimMutation.mutate()}
            >
              Create IRC claim token
            </Button>
          </Group>
        </Stack>
      </Card>

      <Card withBorder radius="sm">
        <Stack gap="md">
          <div>
            <Group gap="xs">
              <Bell size={20} />
              <Title order={3}>Notify Patterns</Title>
            </Group>
            <Text c="dimmed" size="sm">Send private alerts when public channel messages match your rules.</Text>
          </div>

          {notifyRulesQuery.isError && (
            <Alert color="red" variant="light">Could not load notify patterns.</Alert>
          )}
          {connectionsQuery.isError && (
            <Alert color="yellow" variant="light">Known channels could not be loaded. Enter the source alias manually.</Alert>
          )}

          <Stack gap="sm">
            <Group grow align="flex-start" className="profile-grid">
              <Select
                label="Known source channel"
                placeholder="Select channel"
                data={sourceOptions}
                searchable
                clearable
                value={sourceOptions.some((option) => option.value === notifyForm.sourceEchoToAlias)
                  ? notifyForm.sourceEchoToAlias
                  : null}
                onChange={selectSourceAlias}
              />
              <TextInput
                label="Source echo alias"
                value={notifyForm.sourceEchoToAlias}
                onChange={(event) => setNotifyField('sourceEchoToAlias', event.currentTarget.value)}
              />
            </Group>
            <Group grow align="flex-start" className="profile-grid">
              <Select
                label="Pattern type"
                data={[
                  { value: 'PRESET_MENTION', label: 'Mention to me' },
                  { value: 'REGEX', label: 'Regex' },
                ]}
                value={notifyForm.patternType}
                onChange={(value) => setNotifyField('patternType', (value ?? 'PRESET_MENTION') as NotifyPatternType)}
              />
              <TextInput
                label="Regex pattern"
                disabled={notifyForm.patternType !== 'REGEX'}
                value={notifyForm.pattern ?? ''}
                onChange={(event) => setNotifyField('pattern', event.currentTarget.value)}
              />
            </Group>
            <Group grow align="flex-start" className="profile-grid">
              <Select
                label="Destination"
                data={[{ value: 'WHATSAPP_CONNECTION', label: 'WhatsApp private message' }]}
                value={notifyForm.destinationConnectionType}
                onChange={(value) => setNotifyField('destinationConnectionType', value ?? 'WHATSAPP_CONNECTION')}
              />
              <NumberInput
                label="Cooldown seconds"
                min={1}
                max={86400}
                value={notifyForm.cooldownSeconds}
                onChange={(value) => setNotifyField('cooldownSeconds', Number(value) || 60)}
              />
              <Switch
                label="Enabled"
                checked={notifyForm.enabled}
                onChange={(event) => setNotifyField('enabled', event.currentTarget.checked)}
              />
            </Group>
          </Stack>

          {notifyValidationError && (
            <Alert color="red" variant="light">{notifyValidationError}</Alert>
          )}
          {saveNotifyRuleMutation.isError && (
            <Alert color="red" variant="light">Could not save notify pattern.</Alert>
          )}
          {deleteNotifyRuleMutation.isError && (
            <Alert color="red" variant="light">Could not delete notify pattern.</Alert>
          )}

          <Group justify="flex-end">
            {editingNotifyRuleId && (
              <Button variant="subtle" leftSection={<X size={18} />} onClick={cancelNotifyEdit}>
                Cancel
              </Button>
            )}
            <Button
              leftSection={editingNotifyRuleId ? <Save size={18} /> : <Plus size={18} />}
              loading={saveNotifyRuleMutation.isPending}
              onClick={submitNotifyRule}
            >
              {editingNotifyRuleId ? 'Save pattern' : 'Add pattern'}
            </Button>
          </Group>

          <Stack gap="xs">
            {(notifyRulesQuery.data ?? []).map((rule) => (
              <Group
                key={rule.id}
                justify="space-between"
                align="flex-start"
                p="sm"
                style={{ border: '1px solid var(--mantine-color-default-border)', borderRadius: 4 }}
              >
                <Stack gap={4}>
                  <Group gap="xs">
                    <Badge color={rule.enabled ? 'green' : 'gray'} variant="light">
                      {rule.enabled ? 'enabled' : 'disabled'}
                    </Badge>
                    <Badge variant="outline">{rule.patternType === 'REGEX' ? 'regex' : 'mention'}</Badge>
                    <Badge variant="outline">{destinationLabel(rule.destinationConnectionType)}</Badge>
                  </Group>
                  <Text fw={600}>{rule.sourceDisplayName || rule.sourceEchoToAlias}</Text>
                  <Text size="sm" c="dimmed">
                    {rule.patternType === 'REGEX' ? rule.pattern : 'Messages addressed to your profile names'}
                  </Text>
                  <Text size="xs" c="dimmed">Cooldown {rule.cooldownSeconds || 60}s</Text>
                </Stack>
                <Group gap="xs">
                  <Tooltip label="Edit pattern">
                    <ActionIcon variant="subtle" aria-label="Edit notify pattern" onClick={() => editNotifyRule(rule)}>
                      <Pencil size={18} />
                    </ActionIcon>
                  </Tooltip>
                  <Tooltip label="Delete pattern">
                    <ActionIcon
                      color="red"
                      variant="subtle"
                      aria-label="Delete notify pattern"
                      loading={deleteNotifyRuleMutation.isPending}
                      onClick={() => rule.id && deleteNotifyRuleMutation.mutate(rule.id)}
                    >
                      <Trash2 size={18} />
                    </ActionIcon>
                  </Tooltip>
                </Group>
              </Group>
            ))}
            {notifyRulesQuery.isSuccess && (notifyRulesQuery.data ?? []).length === 0 && (
              <Text c="dimmed" size="sm">No notify patterns configured.</Text>
            )}
          </Stack>
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

function formatExpiresAt(expiresAt: number): string {
  return new Date(expiresAt).toLocaleString(undefined, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function destinationLabel(connectionType: string): string {
  return connectionType === 'WHATSAPP_CONNECTION' ? 'WhatsApp' : connectionType;
}

function toProfileForm(me: MeResponse): ProfileUpdateRequest {
  return {
    name: me.name ?? '',
    email: me.email ?? '',
    ircNick: me.ircNick ?? '',
    telegramId: me.telegramId ?? '',
    discordId: me.discordId ?? '',
    whatsappId: me.whatsappId ?? '',
  };
}
