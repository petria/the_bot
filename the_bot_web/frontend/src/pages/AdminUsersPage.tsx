import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Checkbox,
  Group,
  Loader,
  Modal,
  PasswordInput,
  SimpleGrid,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient, type UseMutationResult } from '@tanstack/react-query';
import { Edit, KeyRound, Plus, RefreshCcw, Trash2, UserPlus } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  createAdminUser,
  deleteAdminUser,
  getAdminUsers,
  unlinkAdminUserChatIdentity,
  resetAdminUserPassword,
  updateAdminUser,
  type AdminUser,
  type AdminChatIdentity,
  type AdminUserCreateRequest,
  type AdminUserUpdateRequest,
} from '../api/adminUsers';
import { ApiError } from '../api/client';
import { LinkObservedIdentityModal } from './LinkObservedIdentityModal';

const emptyUserForm: AdminUserCreateRequest = {
  username: '',
  password: '',
  name: '',
  email: '',
  ircNick: '',
  telegramId: '',
  discordId: '',
  whatsappId: '',
  admin: false,
  canDoIrcOp: false,
};

type UserModalState =
  | { mode: 'create'; user: null }
  | { mode: 'edit'; user: AdminUser }
  | null;

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: getAdminUsers,
  });
  const [userModal, setUserModal] = useState<UserModalState>(null);
  const [passwordUser, setPasswordUser] = useState<AdminUser | null>(null);
  const [deleteUser, setDeleteUser] = useState<AdminUser | null>(null);
  const [linkUser, setLinkUser] = useState<AdminUser | null>(null);

  const users = useMemo(
      () => [...(usersQuery.data?.users ?? [])].sort(compareUsers),
      [usersQuery.data?.users],
  );

  const invalidateUsers = () => queryClient.invalidateQueries({ queryKey: ['admin-users'] });

  const deleteMutation = useMutation({
    mutationFn: (user: AdminUser) => deleteAdminUser(user.id as number),
    onSuccess: () => {
      setDeleteUser(null);
      invalidateUsers();
    },
  });
  const unlinkIdentityMutation = useMutation({
    mutationFn: ({ user, identity }: { user: AdminUser; identity: AdminChatIdentity }) =>
      unlinkAdminUserChatIdentity(user.id as number, identity.identityKey as string),
    onSuccess: () => {
      invalidateUsers();
      queryClient.invalidateQueries({ queryKey: ['known-user-targets'] });
    },
  });

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Admin Users</Title>
          <Text c="dimmed">Manage web login users and bot identity links.</Text>
        </div>
        <Group gap="xs">
          <Tooltip label="Refresh">
            <ActionIcon
              variant="light"
              size="lg"
              aria-label="Refresh users"
              onClick={() => usersQuery.refetch()}
              loading={usersQuery.isFetching}
            >
              <RefreshCcw size={18} />
            </ActionIcon>
          </Tooltip>
          <Button leftSection={<Plus size={18} />} onClick={() => setUserModal({ mode: 'create', user: null })}>
            Add user
          </Button>
        </Group>
      </Group>

      {usersQuery.isLoading ? (
        <Loader />
      ) : usersQuery.isError ? (
        <AdminUsersError error={usersQuery.error} />
      ) : users.length === 0 ? (
        <Card withBorder radius="sm">
          <Group gap="sm">
            <UserPlus size={20} />
            <Text>No users found.</Text>
          </Group>
        </Card>
      ) : (
        <>
          <UsersTable
            users={users}
            onEdit={(user) => setUserModal({ mode: 'edit', user })}
            onResetPassword={setPasswordUser}
            onLink={setLinkUser}
            onUnlinkIdentity={(user, identity) => unlinkIdentityMutation.mutate({ user, identity })}
            onDelete={setDeleteUser}
          />
          <UsersCards
            users={users}
            onEdit={(user) => setUserModal({ mode: 'edit', user })}
            onResetPassword={setPasswordUser}
            onLink={setLinkUser}
            onUnlinkIdentity={(user, identity) => unlinkIdentityMutation.mutate({ user, identity })}
            onDelete={setDeleteUser}
          />
        </>
      )}

      <UserEditorModal
        state={userModal}
        onClose={() => setUserModal(null)}
        onSaved={() => {
          setUserModal(null);
          invalidateUsers();
          queryClient.invalidateQueries({ queryKey: ['me'] });
        }}
      />
      <PasswordResetModal
        user={passwordUser}
        onClose={() => setPasswordUser(null)}
        onSaved={() => {
          setPasswordUser(null);
          invalidateUsers();
        }}
      />
      <DeleteUserModal
        user={deleteUser}
        mutation={deleteMutation}
        onClose={() => setDeleteUser(null)}
      />
      <LinkObservedIdentityModal
        opened={!!linkUser}
        user={linkUser}
        onClose={() => setLinkUser(null)}
      />
    </Stack>
  );
}

function UsersTable({
  users,
  onEdit,
  onResetPassword,
  onLink,
  onUnlinkIdentity,
  onDelete,
}: {
  users: AdminUser[];
  onEdit: (user: AdminUser) => void;
  onResetPassword: (user: AdminUser) => void;
  onLink: (user: AdminUser) => void;
  onUnlinkIdentity: (user: AdminUser, identity: AdminChatIdentity) => void;
  onDelete: (user: AdminUser) => void;
}) {
  return (
    <Table.ScrollContainer minWidth={920} className="admin-users-table">
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>User</Table.Th>
            <Table.Th>Identity links</Table.Th>
            <Table.Th>Access</Table.Th>
            <Table.Th>Actions</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {users.map((user) => (
              <Table.Tr key={user.id ?? user.username}>
                <Table.Td><UserCell user={user} /></Table.Td>
              <Table.Td><IdentityCell user={user} onUnlinkIdentity={onUnlinkIdentity} /></Table.Td>
              <Table.Td><AccessBadges user={user} /></Table.Td>
              <Table.Td>
                <UserActions
                  user={user}
                  onEdit={onEdit}
                  onResetPassword={onResetPassword}
                  onLink={onLink}
                  onDelete={onDelete}
                />
              </Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}

function UsersCards({
  users,
  onEdit,
  onResetPassword,
  onLink,
  onUnlinkIdentity,
  onDelete,
}: {
  users: AdminUser[];
  onEdit: (user: AdminUser) => void;
  onResetPassword: (user: AdminUser) => void;
  onLink: (user: AdminUser) => void;
  onUnlinkIdentity: (user: AdminUser, identity: AdminChatIdentity) => void;
  onDelete: (user: AdminUser) => void;
}) {
  return (
    <Stack gap="sm" className="admin-users-cards">
      {users.map((user) => (
        <Card withBorder radius="sm" key={user.id ?? user.username}>
          <Stack gap="sm">
            <Group justify="space-between" align="flex-start" gap="sm">
              <UserCell user={user} />
              <AccessBadges user={user} />
            </Group>
            <IdentityCell user={user} onUnlinkIdentity={onUnlinkIdentity} />
            <UserActions
              user={user}
              onEdit={onEdit}
              onResetPassword={onResetPassword}
              onLink={onLink}
              onDelete={onDelete}
            />
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function UserEditorModal({
  state,
  onClose,
  onSaved,
}: {
  state: UserModalState;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState<AdminUserCreateRequest>(emptyUserForm);
  const [validationError, setValidationError] = useState<string | null>(null);
  const isOpen = state !== null;
  const isCreate = state?.mode === 'create';

  useEffect(() => {
    if (!state) {
      setForm(emptyUserForm);
      setValidationError(null);
      return;
    }
    if (state.mode === 'create') {
      setForm(emptyUserForm);
    } else {
      setForm(toForm(state.user));
    }
    setValidationError(null);
  }, [state]);

  const createMutation = useMutation({
    mutationFn: createAdminUser,
    onSuccess: onSaved,
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: AdminUserUpdateRequest }) => updateAdminUser(id, request),
    onSuccess: onSaved,
  });

  const mutationError = createMutation.error || updateMutation.error;
  const isPending = createMutation.isPending || updateMutation.isPending;

  const setField = (field: keyof AdminUserCreateRequest, value: string | boolean) => {
    setValidationError(null);
    createMutation.reset();
    updateMutation.reset();
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSave = () => {
    if (isCreate) {
      if (!form.username.trim()) {
        setValidationError('Username is required.');
        return;
      }
      if (form.password.length < 10) {
        setValidationError('Password must be at least 10 characters.');
        return;
      }
      createMutation.mutate(trimCreateForm(form));
      return;
    }
    if (state?.mode === 'edit' && state.user.id !== null) {
      updateMutation.mutate({ id: state.user.id, request: trimUpdateForm(form) });
    }
  };

  return (
    <Modal opened={isOpen} onClose={onClose} title={isCreate ? 'Add user' : 'Edit user'} size="lg">
      <Stack gap="md">
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput
            label="Username"
            value={form.username}
            disabled={!isCreate}
            onChange={(event) => setField('username', event.currentTarget.value)}
          />
          {isCreate && (
            <PasswordInput
              label="Password"
              autoComplete="new-password"
              value={form.password}
              onChange={(event) => setField('password', event.currentTarget.value)}
            />
          )}
          <TextInput label="Name" value={form.name} onChange={(event) => setField('name', event.currentTarget.value)} />
          <TextInput label="Email" type="email" value={form.email} onChange={(event) => setField('email', event.currentTarget.value)} />
        </SimpleGrid>

        <Group gap="lg">
          <Checkbox
            label="Admin"
            checked={form.admin}
            onChange={(event) => setField('admin', event.currentTarget.checked)}
          />
          <Checkbox
            label="Can do IRC op"
            checked={form.canDoIrcOp}
            onChange={(event) => setField('canDoIrcOp', event.currentTarget.checked)}
          />
        </Group>

        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput
            label="IRC nick"
            value={form.ircNick}
            onChange={(event) => setField('ircNick', event.currentTarget.value)}
          />
          <TextInput
            label="Telegram id"
            value={form.telegramId}
            onChange={(event) => setField('telegramId', event.currentTarget.value)}
          />
          <TextInput
            label="Discord id"
            value={form.discordId}
            onChange={(event) => setField('discordId', event.currentTarget.value)}
          />
          <TextInput
            label="WhatsApp id"
            value={form.whatsappId}
            onChange={(event) => setField('whatsappId', event.currentTarget.value)}
          />
        </SimpleGrid>

        {validationError && <Alert color="red" variant="light">{validationError}</Alert>}
        {mutationError && <AdminUsersError error={mutationError} />}

        <Group justify="flex-end">
          <Button variant="subtle" onClick={onClose}>Cancel</Button>
          <Button loading={isPending} onClick={handleSave}>
            {isCreate ? 'Create user' : 'Save user'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function PasswordResetModal({
  user,
  onClose,
  onSaved,
}: {
  user: AdminUser | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [password, setPassword] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);
  const mutation = useMutation({
    mutationFn: ({ id, value }: { id: number; value: string }) => resetAdminUserPassword(id, { password: value }),
    onSuccess: () => {
      setPassword('');
      setValidationError(null);
      onSaved();
    },
  });

  const handleSave = () => {
    if (!user?.id) {
      return;
    }
    if (password.length < 10) {
      setValidationError('Password must be at least 10 characters.');
      return;
    }
    mutation.mutate({ id: user.id, value: password });
  };

  return (
    <Modal opened={!!user} onClose={onClose} title={`Reset password${user?.username ? ` for ${user.username}` : ''}`}>
      <Stack gap="md">
        <PasswordInput
          label="New password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => {
            setValidationError(null);
            mutation.reset();
            setPassword(event.currentTarget.value);
          }}
        />
        {validationError && <Alert color="red" variant="light">{validationError}</Alert>}
        {mutation.error && <AdminUsersError error={mutation.error} />}
        <Group justify="flex-end">
          <Button variant="subtle" onClick={onClose}>Cancel</Button>
          <Button leftSection={<KeyRound size={18} />} loading={mutation.isPending} onClick={handleSave}>
            Reset password
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function DeleteUserModal({
  user,
  mutation,
  onClose,
}: {
  user: AdminUser | null;
  mutation: UseMutationResult<AdminUser, Error, AdminUser>;
  onClose: () => void;
}) {
  return (
    <Modal opened={!!user} onClose={onClose} title="Delete user">
      <Stack gap="md">
        <Text>
          Delete user <strong>{user?.username}</strong>?
        </Text>
        {mutation.error && <AdminUsersError error={mutation.error} />}
        <Group justify="flex-end">
          <Button variant="subtle" onClick={onClose}>Cancel</Button>
          <Button
            color="red"
            leftSection={<Trash2 size={18} />}
            loading={mutation.isPending}
            disabled={!user || user.reserved}
            onClick={() => user && mutation.mutate(user)}
          >
            Delete
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function UserActions({
  user,
  onEdit,
  onResetPassword,
  onLink,
  onDelete,
}: {
  user: AdminUser;
  onEdit: (user: AdminUser) => void;
  onResetPassword: (user: AdminUser) => void;
  onLink: (user: AdminUser) => void;
  onDelete: (user: AdminUser) => void;
}) {
  const disabled = user.reserved || user.id === null;
  return (
    <Group gap="xs" wrap="nowrap">
      <Tooltip label="Edit">
        <ActionIcon variant="light" aria-label="Edit user" disabled={disabled} onClick={() => onEdit(user)}>
          <Edit size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Reset password">
        <ActionIcon variant="light" aria-label="Reset password" disabled={disabled} onClick={() => onResetPassword(user)}>
          <KeyRound size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Link observed identity">
        <ActionIcon variant="light" aria-label="Link observed identity" disabled={disabled} onClick={() => onLink(user)}>
          <Plus size={16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label="Delete">
        <ActionIcon color="red" variant="light" aria-label="Delete user" disabled={disabled} onClick={() => onDelete(user)}>
          <Trash2 size={16} />
        </ActionIcon>
      </Tooltip>
    </Group>
  );
}

function UserCell({ user }: { user: AdminUser }) {
  return (
    <Stack gap={2} className="admin-users-cell">
      <Group gap="xs" wrap="wrap">
        <Text fw={700}>{user.username || '-'}</Text>
        {user.reserved && <Badge variant="outline">reserved</Badge>}
      </Group>
      <Text size="xs" c="dimmed" truncate>{user.name || user.email || `user #${user.id}`}</Text>
    </Stack>
  );
}

function IdentityCell({
  user,
  onUnlinkIdentity,
}: {
  user: AdminUser;
  onUnlinkIdentity: (user: AdminUser, identity: AdminChatIdentity) => void;
}) {
  const identities = user.chatIdentities ?? [];
  if (identities.length === 0) {
    return <Text size="sm" c="dimmed">No linked identities</Text>;
  }
  return (
    <Stack gap={2} className="admin-users-cell">
      {identities.map((identity) => (
        <Group key={identity.identityKey} gap="xs" wrap="nowrap" justify="space-between">
          <Stack gap={0} className="admin-users-cell">
            <Text size="sm" truncate>{identityLabel(identity)}</Text>
            <Text size="xs" c="dimmed" truncate>{identity.identityKey || '-'}</Text>
          </Stack>
          <ActionIcon
            size="sm"
            color="red"
            variant="subtle"
            aria-label="Unlink identity"
            disabled={!identity.identityKey || user.reserved}
            onClick={() => onUnlinkIdentity(user, identity)}
          >
            <Trash2 size={14} />
          </ActionIcon>
        </Group>
      ))}
    </Stack>
  );
}

function AccessBadges({ user }: { user: AdminUser }) {
  return (
    <Group gap="xs" wrap="wrap">
      <Badge variant={user.admin ? 'filled' : 'light'} color={user.admin ? 'blue' : 'gray'}>
        {user.admin ? 'Admin' : 'User'}
      </Badge>
      {user.canDoIrcOp && <Badge variant="light">IRC op</Badge>}
    </Group>
  );
}

function identityLabel(identity: AdminChatIdentity) {
  const observed = identity.displayName || identity.username || identity.userId || '-';
  return `${identity.connectionType || 'unknown'}: ${observed}`;
}

function AdminUsersError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" variant="light">
      {apiError?.message || error.message || 'Could not manage users.'}
    </Alert>
  );
}

function toForm(user: AdminUser): AdminUserCreateRequest {
  return {
    username: user.username ?? '',
    password: '',
    name: user.name ?? '',
    email: user.email ?? '',
    ircNick: user.ircNick ?? '',
    telegramId: user.telegramId ?? '',
    discordId: user.discordId ?? '',
    whatsappId: user.whatsappId ?? '',
    admin: user.admin,
    canDoIrcOp: user.canDoIrcOp,
  };
}

function trimCreateForm(form: AdminUserCreateRequest): AdminUserCreateRequest {
  return {
    ...trimUpdateForm(form),
    username: form.username.trim(),
    password: form.password,
  };
}

function trimUpdateForm(form: AdminUserCreateRequest): AdminUserUpdateRequest {
  return {
    name: form.name.trim(),
    email: form.email.trim(),
    ircNick: form.ircNick.trim(),
    telegramId: form.telegramId.trim(),
    discordId: form.discordId.trim(),
    whatsappId: form.whatsappId.trim(),
    admin: form.admin,
    canDoIrcOp: form.canDoIrcOp,
  };
}

function compareUsers(left: AdminUser, right: AdminUser) {
  if (left.reserved !== right.reserved) {
    return left.reserved ? -1 : 1;
  }
  return (left.username || '').localeCompare(right.username || '');
}
