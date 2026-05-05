import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Select,
  SimpleGrid,
  Stack,
  Switch,
  Tabs,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { useMutation, useQuery } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, Send } from 'lucide-react';
import { useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import { getConnectionsOverview, type BotConnectionChannel } from '../api/connections';
import { getKnownUserTargets, type KnownUserTarget } from '../api/knownUsers';
import {
  sendMessageByEchoToAlias,
  sendIrcPrivateMessage,
  sendMessageToKnownUser,
  type SendEchoToAliasResponse,
  type SendIrcPrivateResponse,
  type SendKnownUserResponse,
} from '../api/messages';

const maxMessageLength = 900;

export function SendPage() {
  return (
    <Stack gap="md">
      <div>
        <Title order={2}>Send Message</Title>
        <Text c="dimmed">Send through bot-io using resolved known users or direct channel aliases.</Text>
      </div>

      <Tabs defaultValue="user" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="user">To User</Tabs.Tab>
          <Tabs.Tab value="channel">To Channel</Tabs.Tab>
          <Tabs.Tab value="irc-private">IRC Private</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="user" pt="md">
          <SendToUser />
        </Tabs.Panel>

        <Tabs.Panel value="channel" pt="md">
          <SendToChannel />
        </Tabs.Panel>

        <Tabs.Panel value="irc-private" pt="md">
          <SendToIrcPrivate />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  );
}

function SendToUser() {
  const [query, setQuery] = useState('');
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [preferPrivate, setPreferPrivate] = useState(true);
  const [message, setMessage] = useState('');
  const [debouncedQuery] = useDebouncedValue(query, 250);
  const trimmedMessage = message.trim();

  const targetsQuery = useQuery({
    queryKey: ['send-known-user-targets', debouncedQuery],
    queryFn: () => getKnownUserTargets(debouncedQuery),
    refetchInterval: 15000,
  });

  const targets = targetsQuery.data ?? [];
  const targetOptions = useMemo(() => targets.map((target) => ({
    value: targetKey(target),
    label: userTargetLabel(target),
  })), [targets]);
  const selectedTarget = targets.find((target) => targetKey(target) === selectedKey) ?? null;
  const previewTarget = selectedTarget ?? targets[0] ?? null;
  const sendMutation = useMutation({
    mutationFn: () => sendMessageToKnownUser({
      query: knownUserQuery(selectedTarget, query),
      message: trimmedMessage,
      preferPrivate,
      connectionType: selectedTarget?.connectionType ?? null,
      echoToAlias: selectedTarget?.echoToAlias ?? null,
    }),
  });

  const canSend = knownUserQuery(selectedTarget, query).length > 0
      && trimmedMessage.length > 0
      && trimmedMessage.length <= maxMessageLength
      && !sendMutation.isPending;

  const handleSend = () => {
    if (canSend) {
      sendMutation.mutate();
    }
  };

  return (
    <Stack gap="md">
      <Card withBorder radius="sm">
        <Stack gap="md">
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
            <TextInput
              label="Search user"
              placeholder="Name, nick, Telegram or Discord"
              value={query}
              onChange={(event) => {
                setQuery(event.currentTarget.value);
                setSelectedKey(null);
                sendMutation.reset();
              }}
            />
            <Select
              label="Resolved target"
              placeholder={targetsQuery.isLoading ? 'Loading targets' : 'Best target will be selected'}
              data={targetOptions}
              value={selectedKey}
              onChange={(value) => {
                setSelectedKey(value);
                sendMutation.reset();
              }}
              searchable
              clearable
              disabled={targetsQuery.isLoading || targetOptions.length === 0}
            />
          </SimpleGrid>

          <Switch
            label="Prefer private target when bot knows one"
            checked={preferPrivate}
            onChange={(event) => setPreferPrivate(event.currentTarget.checked)}
          />

          <Textarea
            label="Message"
            minRows={4}
            autosize
            maxRows={8}
            maxLength={maxMessageLength}
            value={message}
            onChange={(event) => {
              setMessage(event.currentTarget.value);
              sendMutation.reset();
            }}
          />

          <PreviewCard
            title="Resolved send preview"
            emptyText={targetsQuery.isLoading ? 'Loading known user targets...' : 'Search or select a known user target.'}
            target={previewTarget}
            message={trimmedMessage}
          />

          <Group justify="space-between" align="center" gap="sm">
            <Text size="sm" c={trimmedMessage.length > maxMessageLength ? 'red' : 'dimmed'}>
              {trimmedMessage.length}/{maxMessageLength}
            </Text>
            <Button leftSection={<Send size={18} />} loading={sendMutation.isPending} disabled={!canSend} onClick={handleSend}>
              Send to user
            </Button>
          </Group>
        </Stack>
      </Card>

      {targetsQuery.isFetching && !targetsQuery.isLoading ? <Text size="sm" c="dimmed">Refreshing targets...</Text> : null}
      {targetsQuery.isLoading ? <Loader size="sm" /> : null}
      {targetsQuery.isError ? <SendError error={targetsQuery.error} fallback="Could not load known users from bot-io." /> : null}
      <SendResult result={sendMutation.data} error={sendMutation.error} />
    </Stack>
  );
}

function SendToChannel() {
  const [selectedAlias, setSelectedAlias] = useState<string | null>(null);
  const [message, setMessage] = useState('');
  const trimmedMessage = message.trim();
  const connectionsQuery = useQuery({
    queryKey: ['send-connections-overview'],
    queryFn: getConnectionsOverview,
    refetchInterval: 15000,
  });
  const channelOptions = useMemo(() => {
    const options: { value: string; label: string }[] = [];
    for (const connection of connectionsQuery.data?.connections ?? []) {
      for (const channel of connection.channels ?? []) {
        if (!channel.echoToAlias) {
          continue;
        }
        options.push({
          value: channel.echoToAlias,
          label: channelLabel(channel, connection.type, connection.network),
        });
      }
    }
    return options.sort((left, right) => left.label.localeCompare(right.label, undefined, { sensitivity: 'base' }));
  }, [connectionsQuery.data]);
  const selectedChannel = useMemo(() => {
    for (const connection of connectionsQuery.data?.connections ?? []) {
      for (const channel of connection.channels ?? []) {
        if (channel.echoToAlias === selectedAlias) {
          return { channel, connectionType: connection.type, network: connection.network };
        }
      }
    }
    return null;
  }, [connectionsQuery.data, selectedAlias]);
  const sendMutation = useMutation({
    mutationFn: () => sendMessageByEchoToAlias({
      echoToAlias: selectedAlias || '',
      message: trimmedMessage,
    }),
  });
  const canSend = !!selectedAlias
      && trimmedMessage.length > 0
      && trimmedMessage.length <= maxMessageLength
      && !sendMutation.isPending;

  const handleSend = () => {
    if (canSend) {
      sendMutation.mutate();
    }
  };

  return (
    <Stack gap="md">
      <Card withBorder radius="sm">
        <Stack gap="md">
          <Select
            label="Channel"
            placeholder={connectionsQuery.isLoading ? 'Loading channels' : 'Select connection channel'}
            data={channelOptions}
            value={selectedAlias}
            onChange={(value) => {
              setSelectedAlias(value);
              sendMutation.reset();
            }}
            searchable
            clearable
            disabled={connectionsQuery.isLoading || channelOptions.length === 0}
          />

          <Textarea
            label="Message"
            minRows={4}
            autosize
            maxRows={8}
            maxLength={maxMessageLength}
            value={message}
            onChange={(event) => {
              setMessage(event.currentTarget.value);
              sendMutation.reset();
            }}
          />

          <Card withBorder radius="sm" className="send-preview">
            <Stack gap="xs">
              <Text size="xs" c="dimmed" fw={700}>Direct channel preview</Text>
              {selectedChannel ? (
                <>
                  <Group gap="xs" wrap="wrap">
                    <Badge variant="light">{selectedChannel.connectionType || 'UNKNOWN'}</Badge>
                    <Badge variant="outline">{selectedChannel.channel.echoToAlias}</Badge>
                  </Group>
                  <Text fw={600}>{selectedChannel.channel.name || selectedChannel.channel.id || '-'}</Text>
                  <Text className="send-preview-message">{trimmedMessage || '-'}</Text>
                </>
              ) : (
                <Text c="dimmed">Select a channel target.</Text>
              )}
            </Stack>
          </Card>

          <Group justify="space-between" align="center" gap="sm">
            <Text size="sm" c={trimmedMessage.length > maxMessageLength ? 'red' : 'dimmed'}>
              {trimmedMessage.length}/{maxMessageLength}
            </Text>
            <Button leftSection={<Send size={18} />} loading={sendMutation.isPending} disabled={!canSend} onClick={handleSend}>
              Send to channel
            </Button>
          </Group>
        </Stack>
      </Card>

      {connectionsQuery.isLoading ? <Loader size="sm" /> : null}
      {connectionsQuery.isError ? <SendError error={connectionsQuery.error} fallback="Could not load channels from bot-io." /> : null}
      <ChannelSendResult result={sendMutation.data} error={sendMutation.error} />
    </Stack>
  );
}

function SendToIrcPrivate() {
  const [connectionId, setConnectionId] = useState<string | null>(null);
  const [nick, setNick] = useState('');
  const [message, setMessage] = useState('');
  const trimmedNick = nick.trim();
  const trimmedMessage = message.trim();
  const connectionsQuery = useQuery({
    queryKey: ['send-irc-private-connections'],
    queryFn: getConnectionsOverview,
    refetchInterval: 15000,
  });
  const ircConnections = useMemo(() => (connectionsQuery.data?.connections ?? [])
      .filter((connection) => connection.type === 'IRC_CONNECTION'), [connectionsQuery.data]);
  const connectionOptions = useMemo(() => ircConnections.map((connection) => ({
    value: connection.id.toString(),
    label: `${connection.network || 'IRC'} / connection ${connection.id}`,
  })), [ircConnections]);
  const selectedConnection = ircConnections.find((connection) => connection.id.toString() === connectionId) ?? null;
  const sendMutation = useMutation({
    mutationFn: () => sendIrcPrivateMessage({
      connectionId: Number(connectionId),
      nick: trimmedNick,
      message: trimmedMessage,
    }),
  });
  const canSend = !!selectedConnection
      && trimmedNick.length > 0
      && trimmedMessage.length > 0
      && trimmedMessage.length <= maxMessageLength
      && !sendMutation.isPending;

  const handleSend = () => {
    if (canSend) {
      sendMutation.mutate();
    }
  };

  return (
    <Stack gap="md">
      <Card withBorder radius="sm">
        <Stack gap="md">
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
            <Select
              label="IRC connection"
              placeholder={connectionsQuery.isLoading ? 'Loading IRC connections' : 'Select IRC connection'}
              data={connectionOptions}
              value={connectionId}
              onChange={(value) => {
                setConnectionId(value);
                sendMutation.reset();
              }}
              disabled={connectionsQuery.isLoading || connectionOptions.length === 0}
            />
            <TextInput
              label="IRC nick"
              placeholder="nick"
              value={nick}
              onChange={(event) => {
                setNick(event.currentTarget.value);
                sendMutation.reset();
              }}
            />
          </SimpleGrid>

          <Textarea
            label="Message"
            minRows={4}
            autosize
            maxRows={8}
            maxLength={maxMessageLength}
            value={message}
            onChange={(event) => {
              setMessage(event.currentTarget.value);
              sendMutation.reset();
            }}
          />

          <Card withBorder radius="sm" className="send-preview">
            <Stack gap="xs">
              <Text size="xs" c="dimmed" fw={700}>IRC private preview</Text>
              {selectedConnection && trimmedNick ? (
                <>
                  <Group gap="xs" wrap="wrap">
                    <Badge variant="light">{selectedConnection.network || 'IRC'}</Badge>
                    <Badge variant="outline">PRIVATE-{trimmedNick}</Badge>
                  </Group>
                  <Text fw={600}>PRIVMSG {trimmedNick}</Text>
                  <Text className="send-preview-message">{trimmedMessage || '-'}</Text>
                </>
              ) : (
                <Text c="dimmed">Select IRC connection and type target nick.</Text>
              )}
            </Stack>
          </Card>

          <Group justify="space-between" align="center" gap="sm">
            <Text size="sm" c={trimmedMessage.length > maxMessageLength ? 'red' : 'dimmed'}>
              {trimmedMessage.length}/{maxMessageLength}
            </Text>
            <Button leftSection={<Send size={18} />} loading={sendMutation.isPending} disabled={!canSend} onClick={handleSend}>
              Send IRC private
            </Button>
          </Group>
        </Stack>
      </Card>

      {connectionsQuery.isLoading ? <Loader size="sm" /> : null}
      {connectionsQuery.isError ? <SendError error={connectionsQuery.error} fallback="Could not load IRC connections from bot-io." /> : null}
      {!connectionsQuery.isLoading && !connectionsQuery.isError && connectionOptions.length === 0 ? (
        <Alert color="yellow" icon={<AlertCircle size={18} />}>
          <Text fw={700}>No IRC connections are currently known.</Text>
        </Alert>
      ) : null}
      <IrcPrivateSendResult result={sendMutation.data} error={sendMutation.error} />
    </Stack>
  );
}

function PreviewCard({
  title,
  emptyText,
  target,
  message,
}: {
  title: string;
  emptyText: string;
  target: KnownUserTarget | null;
  message: string;
}) {
  return (
    <Card withBorder radius="sm" className="send-preview">
      <Stack gap="xs">
        <Text size="xs" c="dimmed" fw={700}>{title}</Text>
        {target ? (
          <>
            <Group gap="xs" wrap="wrap">
              <Badge variant={target.targetType === 'PRIVATE' ? 'filled' : 'light'}>{target.targetType || 'UNKNOWN'}</Badge>
              <Badge variant="outline">{target.connectionType || 'UNKNOWN'}</Badge>
              {target.echoToAlias ? <Badge variant="outline">{target.echoToAlias}</Badge> : null}
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
              <PreviewItem label="User" value={target.configuredName || target.configuredUsername || target.logicalUserKey} />
              <PreviewItem label="Observed as" value={target.observedDisplayName || target.observedUsername || target.observedUserId} />
              <PreviewItem label="Network" value={target.network} />
              <PreviewItem label="Channel" value={target.channelName || target.channelId} />
            </SimpleGrid>
            <Text className="send-preview-message">{formatKnownUserPreviewMessage(message, target) || '-'}</Text>
          </>
        ) : (
          <Text c="dimmed">{emptyText}</Text>
        )}
      </Stack>
    </Card>
  );
}

function PreviewItem({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <Stack gap={2}>
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text size="sm">{value || '-'}</Text>
    </Stack>
  );
}

function SendResult({ result, error }: { result?: SendKnownUserResponse; error: Error | null }) {
  if (error) {
    return <SendError error={error} fallback="Could not send message." />;
  }
  if (!result) {
    return null;
  }

  const ok = result.status === 'OK';
  return (
    <Alert color={ok ? 'green' : 'yellow'} icon={ok ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}>
      <Stack gap={4}>
        <Text fw={700}>{result.message || (ok ? 'Message sent.' : 'Message was not sent.')}</Text>
        <Text size="sm">Target: {result.sentTo || '-'}</Text>
        {result.selectedTarget ? <Text size="sm">Selected: {userTargetLabel(result.selectedTarget)}</Text> : null}
      </Stack>
    </Alert>
  );
}

function ChannelSendResult({ result, error }: { result?: SendEchoToAliasResponse; error: Error | null }) {
  if (error) {
    return <SendError error={error} fallback="Could not send message." />;
  }
  if (!result) {
    return null;
  }
  const ok = !result.sentTo?.startsWith('NOK');
  return (
    <Alert color={ok ? 'green' : 'yellow'} icon={ok ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}>
      <Text fw={700}>{ok ? 'Message sent.' : 'Message was not sent.'}</Text>
      <Text size="sm">Target: {result.sentTo || '-'}</Text>
    </Alert>
  );
}

function IrcPrivateSendResult({ result, error }: { result?: SendIrcPrivateResponse; error: Error | null }) {
  if (error) {
    return <SendError error={error} fallback="Could not send IRC private message." />;
  }
  if (!result) {
    return null;
  }
  const ok = result.status === 'OK';
  return (
    <Alert color={ok ? 'green' : 'yellow'} icon={ok ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}>
      <Text fw={700}>{result.message || (ok ? 'IRC private message sent.' : 'IRC private message was not sent.')}</Text>
      <Text size="sm">Nick: {result.sentTo || '-'}</Text>
    </Alert>
  );
}

function SendError({ error, fallback }: { error: Error; fallback: string }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError?.message || fallback}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}

function targetKey(target: KnownUserTarget) {
  return [
    target.logicalUserKey,
    target.observedUserKey,
    target.connectionId,
    target.echoToAlias,
    target.targetType,
  ].join('|');
}

function userTargetLabel(target: KnownUserTarget) {
  const user = target.configuredName || target.configuredUsername || target.logicalUserKey || 'unknown user';
  const observed = target.observedDisplayName || target.observedUsername || target.observedUserId || 'unknown observed user';
  const channel = target.channelName || target.echoToAlias || target.channelId || 'private';
  return `${user} / ${observed} / ${target.connectionType || 'UNKNOWN'} / ${channel}`;
}

function knownUserQuery(target: KnownUserTarget | null, query: string) {
  return (
    target?.configuredUsername
    || target?.configuredName
    || target?.logicalUserKey
    || target?.observedUsername
    || target?.observedDisplayName
    || target?.observedUserId
    || query
  ).trim();
}

function formatKnownUserPreviewMessage(message: string, target: KnownUserTarget) {
  if (!message || target.targetType === 'PRIVATE') {
    return message;
  }
  if (target.connectionType === 'IRC_CONNECTION') {
    const nick = target.observedUsername || target.observedUserId;
    return nick ? `${nick}: ${message}` : message;
  }
  if (target.connectionType === 'DISCORD_CONNECTION') {
    return target.observedUserId ? `<@${target.observedUserId}> ${message}` : message;
  }
  if (target.connectionType === 'TELEGRAM_CONNECTION') {
    if (target.observedUsername) {
      return `@${target.observedUsername.replace(/^@/, '')} ${message}`;
    }
    return target.observedDisplayName ? `${target.observedDisplayName}: ${message}` : message;
  }
  return message;
}

function channelLabel(channel: BotConnectionChannel, connectionType: string | null, network: string | null) {
  const type = connectionType || channel.type || 'UNKNOWN';
  const name = channel.name || channel.id || channel.echoToAlias || 'unknown channel';
  const alias = channel.echoToAlias || '-';
  return `${type} / ${network || channel.network || '-'} / ${name} / ${alias}`;
}
