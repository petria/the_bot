import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, Copy, ExternalLink, FileAudio, FileVideo, Image as ImageIcon, Link2, RefreshCw, Search } from 'lucide-react';
import { ApiError } from '../api/client';
import { getLiveMedia, type LiveMediaItem } from '../api/liveMedia';

type TypeFilter = 'all' | 'media' | 'url' | 'image' | 'video' | 'audio';

const typeColumnStyle = { width: 132, minWidth: 132 };

export function LiveMediaPage() {
  const [typeFilter, setTypeFilter] = useState<TypeFilter>('all');
  const [channelFilter, setChannelFilter] = useState<string | null>('all');
  const [textFilter, setTextFilter] = useState('');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const mediaQuery = useQuery({
    queryKey: ['live-media'],
    queryFn: getLiveMedia,
    refetchInterval: 30000,
  });

  const items = mediaQuery.data?.items ?? [];
  const channelOptions = useMemo(() => {
    const channels = Array.from(new Set(items.map((item) => item.sourceChannelAlias).filter(Boolean) as string[])).sort();
    return [{ label: 'All channels', value: 'all' }, ...channels.map((value) => ({ label: value, value }))];
  }, [items]);
  const visibleItems = useMemo(() => {
    const query = textFilter.trim().toLowerCase();
    return items.filter((item) => {
      if (typeFilter === 'media' && item.type !== 'media') {
        return false;
      }
      if (typeFilter === 'url' && item.type !== 'url') {
        return false;
      }
      if ((typeFilter === 'image' || typeFilter === 'video' || typeFilter === 'audio') && item.mediaType !== typeFilter) {
        return false;
      }
      if (channelFilter && channelFilter !== 'all' && item.sourceChannelAlias !== channelFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [
        item.originalFileName,
        item.contentType,
        item.url,
        item.title,
        item.description,
        item.author,
        item.provider,
        item.sourceSender,
        item.sourceChannelAlias,
        item.sourceChannelName,
        item.sourceNetwork,
        item.sourceProtocol,
      ]
          .filter(Boolean)
          .some((value) => value?.toLowerCase().includes(query));
    });
  }, [channelFilter, items, textFilter, typeFilter]);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>Live Media</Title>
          <Text c="dimmed">Browse media and URLs collected from channels you can view.</Text>
        </div>
        <Button
          variant="light"
          leftSection={<RefreshCw size={16} />}
          onClick={() => mediaQuery.refetch()}
          loading={mediaQuery.isFetching}
        >
          Refresh
        </Button>
      </Group>

      {mediaQuery.isLoading ? <Loader /> : null}
      {mediaQuery.isError ? <LiveMediaError error={mediaQuery.error} /> : null}
      {mediaQuery.data && !mediaQuery.data.enabled ? (
        <Alert color="yellow" variant="light" icon={<AlertCircle size={18} />}>
          Media storage is disabled.
        </Alert>
      ) : null}
      {mediaQuery.data?.detail ? (
        <Alert color={mediaQuery.data.enabled ? 'yellow' : 'red'} variant="light" icon={<AlertCircle size={18} />}>
          {mediaQuery.data.detail}
        </Alert>
      ) : null}

      <Card withBorder radius="sm">
        <Stack gap="md">
          <Group grow align="flex-end">
            <TextInput
              label="Filter"
              placeholder="Title, file, URL, sender, channel"
              leftSection={<Search size={16} />}
              value={textFilter}
              onChange={(event) => setTextFilter(event.currentTarget.value)}
            />
            <Select
              label="Type"
              value={typeFilter}
              data={[
                { label: 'All types', value: 'all' },
                { label: 'Media', value: 'media' },
                { label: 'URLs', value: 'url' },
                { label: 'Images', value: 'image' },
                { label: 'Videos', value: 'video' },
                { label: 'Audio', value: 'audio' },
              ]}
              onChange={(value) => setTypeFilter((value ?? 'all') as TypeFilter)}
              allowDeselect={false}
            />
            <Select
              label="Channel"
              value={channelFilter}
              data={channelOptions}
              onChange={setChannelFilter}
              allowDeselect={false}
            />
          </Group>

          <Group gap="xs">
            <Badge variant="light">{visibleItems.length.toLocaleString('fi-FI')} shown</Badge>
            <Badge variant="light" color="gray">{items.length.toLocaleString('fi-FI')} accessible</Badge>
          </Group>

          <Table.ScrollContainer minWidth={1080}>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th style={typeColumnStyle}>Type</Table.Th>
                  <Table.Th>Content</Table.Th>
                  <Table.Th>Source</Table.Th>
                  <Table.Th>Sender</Table.Th>
                  <Table.Th>Created</Table.Th>
                  <Table.Th>Expires</Table.Th>
                  <Table.Th>Open</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {visibleItems.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={7}>
                      <Text c="dimmed">No live media found.</Text>
                    </Table.Td>
                  </Table.Tr>
                ) : visibleItems.map((item) => (
                  <Table.Tr key={`${item.type}:${item.id}`}>
                    <Table.Td style={typeColumnStyle}><TypeCell item={item} /></Table.Td>
                    <Table.Td><ContentCell item={item} /></Table.Td>
                    <Table.Td><SourceCell item={item} /></Table.Td>
                    <Table.Td>{item.sourceSender || '-'}</Table.Td>
                    <Table.Td>{formatDateTime(item.createdAt)}</Table.Td>
                    <Table.Td>{formatDateTime(item.expiresAt)}</Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        {itemHref(item) ? (
                          <Tooltip label="Open">
                            <ActionIcon
                              component="a"
                              href={itemHref(item)}
                              target="_blank"
                              rel="noreferrer"
                              variant="light"
                              aria-label="Open item"
                            >
                              <ExternalLink size={16} />
                            </ActionIcon>
                          </Tooltip>
                        ) : null}
                        {copyValue(item) ? (
                          <Tooltip label={copiedKey === itemKey(item) ? 'Copied' : 'Copy link'}>
                            <ActionIcon
                              variant="light"
                              aria-label="Copy link"
                              onClick={() => copyItem(item)}
                            >
                              <Copy size={16} />
                            </ActionIcon>
                          </Tooltip>
                        ) : null}
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Stack>
      </Card>
    </Stack>
  );

  async function copyItem(item: LiveMediaItem) {
    const value = copyValue(item);
    if (!value) {
      return;
    }
    await navigator.clipboard.writeText(value);
    setCopiedKey(itemKey(item));
    window.setTimeout(() => setCopiedKey((current) => current === itemKey(item) ? null : current), 1500);
  }
}

function TypeCell({ item }: { item: LiveMediaItem }) {
  if (item.type === 'url') {
    return (
      <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
        <Link2 size={18} />
        <Badge variant="light">{item.provider || 'Web'}</Badge>
      </Group>
    );
  }
  const Icon = item.mediaType === 'image' ? ImageIcon : item.mediaType === 'video' ? FileVideo : FileAudio;
  return (
    <Group gap="xs" wrap="nowrap" style={{ flexShrink: 0 }}>
      <Icon size={18} />
      <Badge variant="light">{item.mediaType || 'media'}</Badge>
    </Group>
  );
}

function ContentCell({ item }: { item: LiveMediaItem }) {
  if (item.type === 'url') {
    return (
      <Stack gap={1}>
        <Text fw={700}>{item.title || item.url}</Text>
        {item.description ? <Text size="xs" c="dimmed" lineClamp={2}>{item.description}</Text> : null}
        <Text size="xs" c="dimmed" lineClamp={1}>{item.url}</Text>
        <Group gap="xs">
          {item.author ? <Badge variant="light" color="gray">{item.author}</Badge> : null}
          {item.duration ? <Badge variant="light" color="gray">{formatDuration(item.duration)}</Badge> : null}
          {item.viewCount != null ? <Badge variant="light" color="gray">{item.viewCount.toLocaleString('fi-FI')} views</Badge> : null}
        </Group>
      </Stack>
    );
  }
  return (
    <Group gap="sm" wrap="nowrap">
      {item.mediaType === 'image' && item.shortCode ? (
        <img
          src={shortPath(item)}
          alt=""
          loading="lazy"
          style={{ width: 46, height: 46, objectFit: 'cover', borderRadius: 4 }}
        />
      ) : null}
      <Stack gap={1}>
        <Text fw={700}>{item.originalFileName || item.id}</Text>
        <Text size="xs" c="dimmed">{item.contentType || '-'}</Text>
        <Text size="xs" c="dimmed">{formatBytes(item.sizeBytes)}</Text>
      </Stack>
    </Group>
  );
}

function SourceCell({ item }: { item: LiveMediaItem }) {
  return (
    <Stack gap={1}>
      <Text>{item.sourceChannelAlias || item.sourceChannelName || '-'}</Text>
      <Text size="xs" c="dimmed">{[item.sourceProtocol, item.sourceNetwork].filter(Boolean).join(' / ') || '-'}</Text>
    </Stack>
  );
}

function LiveMediaError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
      <Text fw={700}>Could not load live media</Text>
      <Text>{apiError?.message || error.message}</Text>
      {apiError?.detail ? <Text size="sm">{apiError.detail}</Text> : null}
    </Alert>
  );
}

function itemKey(item: LiveMediaItem) {
  return `${item.type}:${item.id}`;
}

function itemHref(item: LiveMediaItem) {
  if (item.type === 'url') {
    return item.url || undefined;
  }
  return item.shortCode ? shortPath(item) : undefined;
}

function copyValue(item: LiveMediaItem) {
  if (item.type === 'url') {
    return item.url || undefined;
  }
  return item.shortCode ? `${window.location.origin}${shortPath(item)}` : undefined;
}

function shortPath(item: LiveMediaItem) {
  return `/m/${item.shortCode}`;
}

function formatBytes(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value) || value < 0) {
    return '-';
  }
  if (value < 1024) {
    return `${value} B`;
  }
  const units = ['KB', 'MB', 'GB'];
  let size = value / 1024;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }
  return `${size.toLocaleString('fi-FI', { maximumFractionDigits: 1 })} ${units[unitIndex]}`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString('fi-FI', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function formatDuration(value: string) {
  const match = value.match(/^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/);
  if (!match) {
    return value;
  }
  const hours = Number(match[1] ?? 0);
  const minutes = Number(match[2] ?? 0);
  const seconds = Number(match[3] ?? 0);
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  }
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}
