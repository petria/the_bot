import { Alert, Button, Code, Group, Modal, PasswordInput, Stack, Text, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { KeyRound, UserPlus } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  createAdminUserFromObservedIdentity,
  type AdminObservedUserCreateRequest,
} from '../api/adminUsers';
import { ApiError } from '../api/client';
import {
  observedPrimaryName,
  observedSecondaryText,
  type KnownUserTarget,
} from '../api/knownUsers';

export function CreateObservedUserModal({
  opened,
  onClose,
  target,
}: {
  opened: boolean;
  onClose: () => void;
  target?: KnownUserTarget | null;
}) {
  const queryClient = useQueryClient();
  const [username, setUsername] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  useEffect(() => {
    if (!opened || !target) {
      setUsername('');
      setName('');
      setEmail('');
      setPassword('');
      setValidationError(null);
      return;
    }
    setUsername(suggestUsername(target));
    setName(suggestName(target));
    setEmail('');
    setPassword('');
    setValidationError(null);
  }, [opened, target]);

  const permissions = useMemo(() => defaultPermissions(target), [target]);
  const mutation = useMutation({
    mutationFn: createAdminUserFromObservedIdentity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['known-user-targets'] });
      handleClose();
    },
  });

  const handleClose = () => {
    mutation.reset();
    setValidationError(null);
    onClose();
  };

  const handleSubmit = () => {
    if (!target) {
      return;
    }
    const trimmedUsername = username.trim();
    if (!trimmedUsername) {
      setValidationError('Username is required.');
      return;
    }
    if (password.length < 10) {
      setValidationError('Password must be at least 10 characters.');
      return;
    }
    const request: AdminObservedUserCreateRequest = {
      username: trimmedUsername,
      password,
      name: name.trim(),
      email: email.trim(),
      connectionType: target.connectionType,
      network: target.network,
      echoToAlias: target.echoToAlias,
      observedUserId: target.observedUserId,
      observedUsername: target.observedUsername,
      observedDisplayName: target.observedDisplayName,
      source: target.lastSeenSource,
    };
    mutation.mutate(request);
  };

  const handleGeneratePassword = () => {
    setValidationError(null);
    mutation.reset();
    setPassword(generateSafePassword());
  };

  const error = mutation.error;

  return (
    <Modal opened={opened} onClose={handleClose} title="Create user from observed identity" size="lg">
      <Stack gap="md">
        {target && (
          <Stack gap={2}>
            <Text size="sm" fw={700}>{observedPrimaryName(target)}</Text>
            <Text size="xs" c="dimmed">{observedSecondaryText(target)}</Text>
            <Text size="xs" c="dimmed">{target.connectionType || '-'} / {target.network || '-'} / {target.echoToAlias || '-'}</Text>
          </Stack>
        )}

        <TextInput
          label="Username"
          value={username}
          onChange={(event) => {
            setValidationError(null);
            mutation.reset();
            setUsername(event.currentTarget.value);
          }}
        />
        <Group align="flex-end" gap="sm" wrap="nowrap">
          <PasswordInput
            label="Initial password"
            autoComplete="new-password"
            value={password}
            style={{ flex: 1 }}
            onChange={(event) => {
              setValidationError(null);
              mutation.reset();
              setPassword(event.currentTarget.value);
            }}
          />
          <Button
            variant="default"
            leftSection={<KeyRound size={16} />}
            onClick={handleGeneratePassword}
          >
            Generate
          </Button>
        </Group>
        <TextInput
          label="Name"
          value={name}
          onChange={(event) => setName(event.currentTarget.value)}
        />
        <TextInput
          label="Email"
          type="email"
          value={email}
          onChange={(event) => setEmail(event.currentTarget.value)}
        />

        <Stack gap={4}>
          <Text size="sm" fw={600}>Default permissions</Text>
          {permissions.map((permission) => (
            <Code key={permission}>{permission}</Code>
          ))}
        </Stack>

        {validationError && <Alert color="red" variant="light">{validationError}</Alert>}
        {error && (
          <Alert color={error instanceof ApiError && error.status === 409 ? 'yellow' : 'red'} variant="light">
            {error instanceof ApiError ? error.message : error.message}
          </Alert>
        )}

        <Group justify="flex-end">
          <Button variant="subtle" onClick={handleClose}>Cancel</Button>
          <Button
            leftSection={<UserPlus size={18} />}
            loading={mutation.isPending}
            disabled={!target}
            onClick={handleSubmit}
          >
            Create user
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function defaultPermissions(target?: KnownUserTarget | null) {
  if (!target?.connectionType || !target.echoToAlias) {
    return ['web.user'];
  }
  return ['web.user', `channels.view.${connectionKey(target.connectionType)}.${channelKey(target.echoToAlias)}`];
}

function suggestName(target: KnownUserTarget) {
  return firstNonBlank(target.observedDisplayName, target.observedUsername, readableUserId(target));
}

function suggestUsername(target: KnownUserTarget) {
  const base = firstNonBlank(target.observedUsername, target.observedDisplayName, readableUserId(target), 'user');
  const normalized = base
      .toLowerCase()
      .replace(/^@+/, '')
      .replace(/@.*$/, '')
      .replace(/[^a-z0-9._-]+/g, '_')
      .replace(/^_+|_+$/g, '')
      .slice(0, 40);
  return normalized || 'user';
}

function readableUserId(target: KnownUserTarget) {
  return target.observedUserId
      ?.replace(/@s\.whatsapp\.net$/i, '')
      .replace(/@lid$/i, '')
      .trim() ?? '';
}

function connectionKey(connectionType: string) {
  return connectionType
      .trim()
      .toLowerCase()
      .replace(/_connection$/i, '')
      .replace(/[^a-z0-9_-]/g, '_');
}

function channelKey(echoToAlias: string) {
  return echoToAlias
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9._-]/g, '_');
}

function firstNonBlank(...values: Array<string | null | undefined>) {
  return values.find((value) => value !== null && value !== undefined && value.trim() !== '')?.trim() ?? '';
}

function generateSafePassword() {
  const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  const lower = 'abcdefghijkmnopqrstuvwxyz';
  const digits = '23456789';
  const symbols = '!@#$%*-_=+?';
  const all = upper + lower + digits + symbols;
  return shuffle([
    pick(upper),
    pick(lower),
    pick(digits),
    pick(symbols),
    ...Array.from({ length: 16 }, () => pick(all)),
  ]).join('');
}

function pick(chars: string) {
  const values = new Uint32Array(1);
  window.crypto.getRandomValues(values);
  return chars[values[0] % chars.length];
}

function shuffle(chars: string[]) {
  const result = [...chars];
  for (let index = result.length - 1; index > 0; index -= 1) {
    const values = new Uint32Array(1);
    window.crypto.getRandomValues(values);
    const swapIndex = values[0] % (index + 1);
    [result[index], result[swapIndex]] = [result[swapIndex], result[index]];
  }
  return result;
}
