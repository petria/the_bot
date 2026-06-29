import {
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Select,
  Stack,
  Tabs,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation, useQuery } from '@tanstack/react-query';
import { AlertTriangle, CornerDownLeft, MessageSquare, Plus, X } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ApiError } from '../api/client';
import {
  getLiveChannelEventStreamUrl,
  getLiveChannels,
  getLiveChannelUsers,
  sendLiveChannelMessage,
  type LiveChannel,
  type LiveChannelEvent,
  type LiveChannelUser,
} from '../api/liveChannels';
import { getMe } from '../api/me';

type OpenChannel = {
  echoToAlias: string;
  label: string;
  sendAllowed: boolean;
};

type ChannelLine = {
  id: number;
  text: string;
};

const maxMessageLength = 900;
const openChannelsStorageKey = 'the-bot-live-channels-open';
const activeAliasStorageKey = 'the-bot-live-channels-active';

export function LiveChannelsPage() {
  const [selectedAlias, setSelectedAlias] = useState<string | null>(null);
  const [openChannels, setOpenChannels] = useState<OpenChannel[]>(readOpenChannels);
  const [activeAlias, setActiveAlias] = useState<string | null>(readActiveAlias);
  const liveChannelsQuery = useQuery({
    queryKey: ['live-channels'],
    queryFn: getLiveChannels,
    refetchInterval: 15000,
  });

  const channelOptions = useMemo(() => {
    const options = (liveChannelsQuery.data ?? []).map((channel) => ({
      value: channel.echoToAlias,
      label: channel.label,
      channel,
    }));
    return options.sort((left, right) => left.label.localeCompare(right.label, undefined, { sensitivity: 'base' }));
  }, [liveChannelsQuery.data]);

  const addChannel = () => {
    if (!selectedAlias || openChannels.some((channel) => channel.echoToAlias === selectedAlias)) {
      return;
    }
    const option = channelOptions.find((channel) => channel.value === selectedAlias);
    const channel = {
      echoToAlias: selectedAlias,
      label: option?.label ?? selectedAlias,
      sendAllowed: option?.channel.sendAllowed ?? false,
    };
    setOpenChannels((current) => [...current, channel]);
    setActiveAlias(selectedAlias);
    setSelectedAlias(null);
  };

  const closeChannel = (echoToAlias: string) => {
    setOpenChannels((current) => {
      const next = current.filter((channel) => channel.echoToAlias !== echoToAlias);
      if (activeAlias === echoToAlias) {
        setActiveAlias(next[0]?.echoToAlias ?? null);
      }
      return next;
    });
  };

  useEffect(() => {
    if (!liveChannelsQuery.data) {
      return;
    }
    const allowedChannels = new Map(liveChannelsQuery.data.map((channel) => [channel.echoToAlias, channel]));
    setOpenChannels((current) => current
        .filter((channel) => allowedChannels.has(channel.echoToAlias))
        .map((channel) => {
          const allowedChannel = allowedChannels.get(channel.echoToAlias) as LiveChannel;
          return {
            ...channel,
            label: allowedChannel.label,
            sendAllowed: allowedChannel.sendAllowed,
          };
        }));
  }, [liveChannelsQuery.data]);

  useEffect(() => {
    if (openChannels.length === 0) {
      if (activeAlias != null) {
        setActiveAlias(null);
      }
      return;
    }
    if (!activeAlias || !openChannels.some((channel) => channel.echoToAlias === activeAlias)) {
      setActiveAlias(openChannels[0].echoToAlias);
    }
  }, [activeAlias, openChannels]);

  useEffect(() => {
    window.sessionStorage.setItem(openChannelsStorageKey, JSON.stringify(openChannels));
    if (activeAlias && openChannels.some((channel) => channel.echoToAlias === activeAlias)) {
      window.sessionStorage.setItem(activeAliasStorageKey, activeAlias);
    } else {
      window.sessionStorage.removeItem(activeAliasStorageKey);
    }
  }, [activeAlias, openChannels]);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Live Channels</Title>
          <Text c="dimmed">Monitor public channel messages and send as your web user.</Text>
        </div>
      </Group>

      <Card withBorder radius="sm">
        <Group gap="sm" align="flex-end">
          <Select
            label="Channel"
            placeholder={liveChannelsQuery.isLoading ? 'Loading channels' : 'Select public channel'}
            data={channelOptions}
            value={selectedAlias}
            onChange={setSelectedAlias}
            searchable
            clearable
            disabled={liveChannelsQuery.isLoading || channelOptions.length === 0}
            className="live-channel-select"
          />
          <Button
            leftSection={<Plus size={18} />}
            disabled={!selectedAlias || openChannels.some((channel) => channel.echoToAlias === selectedAlias)}
            onClick={addChannel}
          >
            Open tab
          </Button>
        </Group>
      </Card>

      {liveChannelsQuery.isLoading ? <Loader size="sm" /> : null}
      {liveChannelsQuery.isError ? (
        <Alert color="red" variant="light" icon={<AlertTriangle size={18} />} title="Could not load channels">
          <Text>{liveChannelsQuery.error instanceof ApiError ? liveChannelsQuery.error.message : liveChannelsQuery.error.message}</Text>
        </Alert>
      ) : null}

      {openChannels.length === 0 ? (
        <Card withBorder radius="sm">
          <Group gap="sm">
            <MessageSquare size={20} />
            <Text c="dimmed">Open a channel tab to start watching live messages.</Text>
          </Group>
        </Card>
      ) : (
        <Tabs value={activeAlias} onChange={setActiveAlias} keepMounted>
          <Tabs.List>
            {openChannels.map((channel) => (
              <Tabs.Tab
                key={channel.echoToAlias}
                value={channel.echoToAlias}
                rightSection={
                  <Button
                    variant="subtle"
                    size="compact-xs"
                    color="gray"
                    aria-label={`Close ${channel.echoToAlias}`}
                    onClick={(event) => {
                      event.stopPropagation();
                      closeChannel(channel.echoToAlias);
                    }}
                  >
                    <X size={14} />
                  </Button>
                }
              >
                {channel.echoToAlias}
              </Tabs.Tab>
            ))}
          </Tabs.List>

          {openChannels.map((channel) => (
            <Tabs.Panel key={channel.echoToAlias} value={channel.echoToAlias} pt="md">
              <LiveChannelTab channel={channel} />
            </Tabs.Panel>
          ))}
        </Tabs>
      )}
    </Stack>
  );
}

function LiveChannelTab({ channel }: { channel: OpenChannel }) {
  const [lines, setLines] = useState<ChannelLine[]>([
    {
      id: 1,
      text: `system> Watching ${channel.label}`,
    },
  ]);
  const [message, setMessage] = useState('');
  const [streamError, setStreamError] = useState<string | null>(null);
  const [streamConnected, setStreamConnected] = useState(false);
  const outputRef = useRef<HTMLTextAreaElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const nextIdRef = useRef(2);
  const latestEventIdRef = useRef(0);
  const seenEventIdsRef = useRef(new Set<number>());
  const trimmedMessage = message.trim();

  const usersQuery = useQuery({
    queryKey: ['live-channel-users', channel.echoToAlias],
    queryFn: () => getLiveChannelUsers(channel.echoToAlias),
    refetchInterval: 15000,
    refetchIntervalInBackground: true,
  });

  const channelUsers = useMemo(() => {
    return [...(usersQuery.data ?? [])].sort((left, right) =>
        compareChannelUsers(left, right));
  }, [usersQuery.data]);

  const sendMutation = useMutation({
    mutationFn: () => sendLiveChannelMessage(channel.echoToAlias, trimmedMessage),
    onError: (error) => {
      const apiError = error instanceof ApiError ? error : null;
      appendLine(`error> ${apiError?.detail || apiError?.message || error.message}`);
    },
    onSettled: () => {
      focusInput();
    },
  });

  useEffect(() => {
    focusInput();
  }, []);

  useEffect(() => {
    const output = outputRef.current;
    if (output) {
      output.scrollTop = output.scrollHeight;
    }
  }, [lines]);

  useEffect(() => {
    let closed = false;
    const source = new EventSource(getLiveChannelEventStreamUrl(channel.echoToAlias, latestEventIdRef.current));
    source.onopen = () => {
      if (closed) {
        return;
      }
      setStreamConnected(true);
      setStreamError(null);
    };
    source.onmessage = (messageEvent) => {
      if (closed || !messageEvent.data) {
        return;
      }
      try {
        const event = JSON.parse(messageEvent.data) as LiveChannelEvent;
        if (!event.id || seenEventIdsRef.current.has(event.id)) {
          return;
        }
        seenEventIdsRef.current.add(event.id);
        latestEventIdRef.current = Math.max(latestEventIdRef.current, event.id);
        appendLine(formatEvent(event));
        focusInput();
      } catch {
        setStreamError('Live stream returned an unreadable event.');
      }
    };
    source.onerror = () => {
      if (closed) {
        return;
      }
      setStreamConnected(false);
      setStreamError('Live stream disconnected; reconnecting...');
      void getMe().catch(() => undefined);
    };
    return () => {
      closed = true;
      source.close();
      setStreamConnected(false);
    };
  }, [channel.echoToAlias]);

  const canSend = trimmedMessage.length > 0
      && trimmedMessage.length <= maxMessageLength
      && channel.sendAllowed
      && !sendMutation.isPending;

  const send = () => {
    if (!canSend) {
      return;
    }
    sendMutation.mutate();
    setMessage('');
  };

  const transcript = lines.map((line) => line.text).join('\n');

  function appendLine(text: string) {
    setLines((current) => [
      ...current,
      {
        id: nextId(),
        text,
      },
    ]);
  }

  function nextId() {
    const value = nextIdRef.current;
    nextIdRef.current += 1;
    return value;
  }

  function focusInput() {
    window.setTimeout(() => {
      inputRef.current?.focus();
    }, 0);
  }

  return (
    <Card withBorder radius="sm" className="live-channel-card">
      <Stack gap="md">
        <Group gap="xs" wrap="wrap">
          <Badge variant="light">{channel.echoToAlias}</Badge>
          <Text size="sm" c="dimmed">{channel.label}</Text>
        </Group>

        <div className="live-channel-panel">
          <Stack gap="md" className="live-channel-transcript">
            <Textarea
              ref={outputRef}
              aria-label={`${channel.echoToAlias} live output`}
              className="console-output"
              value={transcript}
              readOnly
              autosize={false}
              minRows={18}
            />

            <Group gap="sm" align="flex-end" wrap="nowrap" className="console-input-row">
              <TextInput
                ref={inputRef}
                className="console-command-input"
                label="Message"
                placeholder="Message to channel"
                value={message}
                leftSection={<MessageSquare size={18} />}
                onChange={(event) => {
                  setMessage(event.currentTarget.value);
                  sendMutation.reset();
                }}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    send();
                  }
                }}
                disabled={sendMutation.isPending || !channel.sendAllowed}
              />
              <Button
                leftSection={<CornerDownLeft size={18} />}
                loading={sendMutation.isPending}
                disabled={!canSend}
                onClick={send}
              >
                Send
              </Button>
            </Group>
          </Stack>

          <Card withBorder radius="sm" className="live-channel-users">
            <Stack gap="sm">
              <Group justify="space-between" gap="xs">
                <Text fw={600}>Users</Text>
                <Badge variant="light">{channelUsers.length}</Badge>
              </Group>
              {usersQuery.isLoading ? <Loader size="sm" /> : null}
              {usersQuery.isError ? (
                <Text size="sm" c="red">
                  {usersQuery.error instanceof ApiError ? usersQuery.error.message : usersQuery.error.message}
                </Text>
              ) : null}
              {!usersQuery.isLoading && !usersQuery.isError && channelUsers.length === 0 ? (
                <Text size="sm" c="dimmed">No users reported.</Text>
              ) : null}
              <Stack gap="xs" className="live-channel-users-list">
                {channelUsers.map((user, index) => (
                  <div key={userKey(user, index)} className="live-channel-user-row">
                    <Text size="sm" fw={500} truncate="end">{userDisplayName(user)}</Text>
                    <Text size="xs" c="dimmed" truncate="end">{userDetail(user)}</Text>
                  </div>
                ))}
              </Stack>
            </Stack>
          </Card>
        </div>

        <Group justify="space-between">
          <Text size="sm" c={trimmedMessage.length > maxMessageLength ? 'red' : 'dimmed'}>
            {trimmedMessage.length}/{maxMessageLength}
          </Text>
          <Text size="sm" c={streamConnected ? 'dimmed' : 'orange'}>
            {streamConnected ? 'Live stream connected' : 'Live stream reconnecting...'}
          </Text>
        </Group>

        {!channel.sendAllowed ? (
          <Text size="sm" c="dimmed">You have view-only access to this channel.</Text>
        ) : null}

        {streamError ? (
          <Alert color="red" variant="light" icon={<AlertTriangle size={18} />} title="Could not load live messages">
            <Text>{streamError}</Text>
          </Alert>
        ) : null}
        {sendMutation.isError ? (
          <Alert color="red" variant="light" icon={<AlertTriangle size={18} />} title="Could not send message">
            <Text>{sendMutation.error instanceof ApiError ? sendMutation.error.message : sendMutation.error.message}</Text>
          </Alert>
        ) : null}
      </Stack>
    </Card>
  );
}

function userDisplayName(user: LiveChannelUser) {
  const baseName = user.nick || user.realName || user.account || user.userString || 'unknown';
  const prefix = user.displayPrefix?.trim() || '';
  return `${prefix}${baseName}`;
}

function compareChannelUsers(left: LiveChannelUser, right: LiveChannelUser) {
  const leftOperator = isChannelOperator(left);
  const rightOperator = isChannelOperator(right);
  if (leftOperator !== rightOperator) {
    return leftOperator ? -1 : 1;
  }
  return userSortName(left).localeCompare(userSortName(right), undefined, { sensitivity: 'base' });
}

function userSortName(user: LiveChannelUser) {
  return user.nick || user.realName || user.account || user.userString || 'unknown';
}

function isChannelOperator(user: LiveChannelUser) {
  const prefix = user.displayPrefix?.trim();
  if (prefix === '@') {
    return true;
  }
  const modes = user.channelModes?.map((mode) => mode.trim().toLowerCase()) ?? [];
  if (modes.includes('@') || modes.includes('o')) {
    return true;
  }
  const roles = user.channelRoles?.map((role) => role.trim().toLowerCase()) ?? [];
  return roles.includes('operator') || roles.includes('op');
}

function userDetail(user: LiveChannelUser) {
  const roles = user.channelRoles?.filter(Boolean) ?? [];
  const modes = user.channelModes?.filter((mode) => mode && mode !== user.displayPrefix) ?? [];
  const parts = [
    user.realName && user.realName !== user.nick ? user.realName : null,
    ...roles,
    ...modes,
    user.operatorInformation && !roles.some((role) => role.toLowerCase() === user.operatorInformation?.toLowerCase())
      ? user.operatorInformation
      : null,
    user.away ? 'away' : null,
  ].filter(Boolean);
  return parts.length === 0 ? (user.account || user.userString || '-') : parts.join(' / ');
}

function userKey(user: LiveChannelUser, index: number) {
  return user.account || user.userString || user.nick || user.realName || `user-${index}`;
}

function formatEvent(event: LiveChannelEvent) {
  const timestamp = formatTime(event.createdAt);
  const message = event.message || '';
  if (event.direction === 'WEB_OUTBOUND') {
    return `${timestamp} ${message}`;
  }
  const sender = event.sender || 'unknown';
  return `${timestamp} ${sender}: ${message}`;
}

function formatTime(value: number) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '--:--:--';
  }
  return [
    date.getHours(),
    date.getMinutes(),
    date.getSeconds(),
  ].map((part) => part.toString().padStart(2, '0')).join(':');
}

function readOpenChannels() {
  try {
    const raw = window.sessionStorage.getItem(openChannelsStorageKey);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw) as OpenChannel[];
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
        .filter((channel) => typeof channel.echoToAlias === 'string' && typeof channel.label === 'string')
        .map((channel) => ({
          echoToAlias: channel.echoToAlias,
          label: channel.label,
          sendAllowed: channel.sendAllowed === true,
        }));
  } catch {
    return [];
  }
}

function readActiveAlias() {
  return window.sessionStorage.getItem(activeAliasStorageKey);
}
