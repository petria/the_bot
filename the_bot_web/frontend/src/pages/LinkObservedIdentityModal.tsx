import { Alert, Button, Group, Modal, Select, Stack, Text } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import {
  getAdminUsers,
  linkAdminUserChatIdentity,
  type AdminUser,
  type AdminChatIdentityLinkRequest,
} from '../api/adminUsers';
import { ApiError } from '../api/client';
import { getKnownUserTargets, type KnownUserTarget } from '../api/knownUsers';

export function LinkObservedIdentityModal({
  opened,
  onClose,
  target,
  user,
}: {
  opened: boolean;
  onClose: () => void;
  target?: KnownUserTarget | null;
  user?: AdminUser | null;
}) {
  const queryClient = useQueryClient();
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedTargetKey, setSelectedTargetKey] = useState<string | null>(null);
  const [allowMove, setAllowMove] = useState(false);

  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: getAdminUsers,
    enabled: opened && !user,
  });
  const targetsQuery = useQuery({
    queryKey: ['known-user-targets', 'observed-only'],
    queryFn: () => getKnownUserTargets(''),
    enabled: opened && !target,
  });

  const observedTargets = useMemo(
      () => (targetsQuery.data ?? []).filter((candidate) => !candidate.matchedConfiguredUser),
      [targetsQuery.data],
  );
  const effectiveTarget = target ?? observedTargets.find((candidate) => targetKey(candidate) === selectedTargetKey) ?? null;
  const effectiveUser = user ?? usersQuery.data?.users.find((candidate) => String(candidate.id) === selectedUserId) ?? null;

  const linkMutation = useMutation({
    mutationFn: ({ userId, request }: { userId: number; request: AdminChatIdentityLinkRequest }) =>
      linkAdminUserChatIdentity(userId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['known-user-targets'] });
      setSelectedUserId(null);
      setSelectedTargetKey(null);
      setAllowMove(false);
      onClose();
    },
  });

  const error = linkMutation.error;
  const conflict = error instanceof ApiError && error.status === 409;
  const canLink = !!effectiveTarget && !!effectiveUser?.id;

  const handleLink = (moveIfOwned: boolean) => {
    if (!effectiveTarget || !effectiveUser?.id) {
      return;
    }
    setAllowMove(moveIfOwned);
    linkMutation.mutate({
      userId: effectiveUser.id,
      request: {
        connectionType: effectiveTarget.connectionType,
        network: effectiveTarget.network,
        observedUserId: effectiveTarget.observedUserId,
        observedUsername: effectiveTarget.observedUsername,
        observedDisplayName: effectiveTarget.observedDisplayName,
        source: effectiveTarget.lastSeenSource,
        moveIfOwned,
      },
    });
  };

  const handleClose = () => {
    setSelectedUserId(null);
    setSelectedTargetKey(null);
    setAllowMove(false);
    linkMutation.reset();
    onClose();
  };

  return (
    <Modal opened={opened} onClose={handleClose} title="Link observed identity" size="lg">
      <Stack gap="md">
        {!user && (
          <Select
            label="Registered user"
            placeholder="Select user"
            searchable
            data={(usersQuery.data?.users ?? [])
                .filter((candidate) => !candidate.reserved && candidate.id !== null)
                .map((candidate) => ({
                  value: String(candidate.id),
                  label: `${candidate.username || candidate.id}${candidate.name ? ` - ${candidate.name}` : ''}`,
                }))}
            value={selectedUserId}
            onChange={(value) => {
              linkMutation.reset();
              setSelectedUserId(value);
            }}
          />
        )}

        {!target && (
          <Select
            label="Observed identity"
            placeholder="Select observed user"
            searchable
            data={observedTargets.map((candidate) => ({
              value: targetKey(candidate),
              label: targetLabel(candidate),
            }))}
            value={selectedTargetKey}
            onChange={(value) => {
              linkMutation.reset();
              setSelectedTargetKey(value);
            }}
          />
        )}

        {effectiveTarget && (
          <Stack gap={2}>
            <Text size="sm" fw={700}>{targetLabel(effectiveTarget)}</Text>
            <Text size="xs" c="dimmed">{effectiveTarget.observedUserKey || '-'}</Text>
          </Stack>
        )}

        {error && (
          <Alert color={conflict ? 'yellow' : 'red'} variant="light">
            {error instanceof ApiError ? error.message : error.message}
          </Alert>
        )}

        <Group justify="flex-end">
          <Button variant="subtle" onClick={handleClose}>Cancel</Button>
          {conflict && !allowMove ? (
            <Button color="yellow" loading={linkMutation.isPending} onClick={() => handleLink(true)}>
              Move identity
            </Button>
          ) : (
            <Button
              leftSection={<Link2 size={18} />}
              loading={linkMutation.isPending}
              disabled={!canLink}
              onClick={() => handleLink(false)}
            >
              Link
            </Button>
          )}
        </Group>
      </Stack>
    </Modal>
  );
}

function targetKey(target: KnownUserTarget) {
  return [
    target.connectionType || '',
    target.network || '',
    target.observedUserId || '',
    target.observedUsername || '',
    target.observedDisplayName || '',
    target.echoToAlias || '',
  ].join('|');
}

function targetLabel(target: KnownUserTarget) {
  const observed = target.observedDisplayName || target.observedUsername || target.observedUserId || '-';
  return `${observed} / ${target.connectionType || 'unknown'} / ${target.network || 'unknown'}`;
}
