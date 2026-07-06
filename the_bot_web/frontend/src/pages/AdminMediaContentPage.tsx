import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  SegmentedControl,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, Copy, ExternalLink, FileAudio, FileVideo, Image as ImageIcon, Search } from 'lucide-react';
import { ApiError } from '../api/client';
import { getMediaContent, type MediaContentItem } from '../api/adminMediaContent';

type MediaTypeFilter = 'all' | 'image' | 'video' | 'audio';

export function AdminMediaContentPage() {
  const [typeFilter, setTypeFilter] = useState<MediaTypeFilter>('all');
  const [textFilter, setTextFilter] = useState('');
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const mediaQuery = useQuery({
    queryKey: ['admin-media-content'],
    queryFn: getMediaContent,
    refetchInterval: 30000,
  });

  const items = mediaQuery.data?.items ?? [];
  const visibleItems = useMemo(() => {
    const query = textFilter.trim().toLowerCase();
    return items.filter((item) => {
      if (typeFilter !== 'all' && item.mediaType !== typeFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [
        item.originalFileName,
        item.sourceSender,
        item.sourceChannelAlias,
        item.sourceChannelName,
        item.sourceNetwork,
        item.sourceProtocol,
        item.contentType,
      ]
          .filter(Boolean)
          .some((value) => value?.toLowerCase().includes(query));
    });
  }, [items, textFilter, typeFilter]);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>Media Content</Title>
          <Text c="dimmed">Browse media files saved from connected chat channels.</Text>
        </div>
        <Button variant="light" onClick={() => mediaQuery.refetch()} loading={mediaQuery.isFetching}>
          Refresh
        </Button>
      </Group>

      {mediaQuery.isLoading ? <Loader /> : null}
      {mediaQuery.isError ? <MediaContentError error={mediaQuery.error} /> : null}
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
              placeholder="Filename, sender, channel, network"
              leftSection={<Search size={16} />}
              value={textFilter}
              onChange={(event) => setTextFilter(event.currentTarget.value)}
            />
            <SegmentedControl
              value={typeFilter}
              onChange={(value) => setTypeFilter(value as MediaTypeFilter)}
              data={[
                { label: 'All', value: 'all' },
                { label: 'Images', value: 'image' },
                { label: 'Videos', value: 'video' },
                { label: 'Audio', value: 'audio' },
              ]}
            />
          </Group>

          <Group gap="xs">
            <Badge variant="light">{visibleItems.length.toLocaleString('fi-FI')} shown</Badge>
            <Badge variant="light" color="gray">{items.length.toLocaleString('fi-FI')} total</Badge>
            {mediaQuery.data?.storageDir ? <Text size="sm" c="dimmed">{mediaQuery.data.storageDir}</Text> : null}
          </Group>

          <Table.ScrollContainer minWidth={980}>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Type</Table.Th>
                  <Table.Th>File</Table.Th>
                  <Table.Th>Source</Table.Th>
                  <Table.Th>Sender</Table.Th>
                  <Table.Th>Size</Table.Th>
                  <Table.Th>Created</Table.Th>
                  <Table.Th>Expires</Table.Th>
                  <Table.Th>Link</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {visibleItems.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={8}>
                      <Text c="dimmed">No media content found.</Text>
                    </Table.Td>
                  </Table.Tr>
                ) : visibleItems.map((item) => (
                  <Table.Tr key={item.id}>
                    <Table.Td><MediaTypeCell item={item} /></Table.Td>
                    <Table.Td><FileCell item={item} /></Table.Td>
                    <Table.Td><SourceCell item={item} /></Table.Td>
                    <Table.Td>{item.sourceSender || '-'}</Table.Td>
                    <Table.Td>{formatBytes(item.sizeBytes)}</Table.Td>
                    <Table.Td>{formatDateTime(item.createdAt)}</Table.Td>
                    <Table.Td>{formatDateTime(item.expiresAt)}</Table.Td>
                    <Table.Td>
                      <Group gap="xs" wrap="nowrap">
                        <Tooltip label="Open">
                          <ActionIcon
                            component="a"
                            href={shortPath(item)}
                            target="_blank"
                            rel="noreferrer"
                            variant="light"
                            aria-label="Open media"
                          >
                            <ExternalLink size={16} />
                          </ActionIcon>
                        </Tooltip>
                        <Tooltip label={copiedCode === item.shortCode ? 'Copied' : 'Copy link'}>
                          <ActionIcon
                            variant="light"
                            aria-label="Copy media link"
                            onClick={() => copyLink(item)}
                          >
                            <Copy size={16} />
                          </ActionIcon>
                        </Tooltip>
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

  async function copyLink(item: MediaContentItem) {
    await navigator.clipboard.writeText(`${window.location.origin}${shortPath(item)}`);
    setCopiedCode(item.shortCode);
    window.setTimeout(() => setCopiedCode((current) => current === item.shortCode ? null : current), 1500);
  }
}

function MediaTypeCell({ item }: { item: MediaContentItem }) {
  const Icon = item.mediaType === 'image' ? ImageIcon : item.mediaType === 'video' ? FileVideo : FileAudio;
  return (
    <Group gap="xs" wrap="nowrap">
      <Icon size={18} />
      <Badge variant="light">{item.mediaType}</Badge>
    </Group>
  );
}

function FileCell({ item }: { item: MediaContentItem }) {
  return (
    <Group gap="sm" wrap="nowrap">
      {item.mediaType === 'image' ? (
        <img
          src={shortPath(item)}
          alt=""
          loading="lazy"
          style={{ width: 46, height: 46, objectFit: 'cover', borderRadius: 4 }}
        />
      ) : null}
      <Stack gap={1}>
        <Text fw={700}>{item.originalFileName || item.id}</Text>
        <Text size="xs" c="dimmed">{item.contentType}</Text>
        <Text size="xs" c="dimmed">{item.shortCode}</Text>
      </Stack>
    </Group>
  );
}

function SourceCell({ item }: { item: MediaContentItem }) {
  return (
    <Stack gap={1}>
      <Text>{item.sourceChannelAlias || item.sourceChannelName || '-'}</Text>
      <Text size="xs" c="dimmed">{[item.sourceProtocol, item.sourceNetwork].filter(Boolean).join(' / ') || '-'}</Text>
    </Stack>
  );
}

function MediaContentError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
      <Text fw={700}>Could not load media content</Text>
      <Text>{apiError?.message || error.message}</Text>
      {apiError?.detail ? <Text size="sm">{apiError.detail}</Text> : null}
    </Alert>
  );
}

function shortPath(item: MediaContentItem) {
  return `/m/${item.shortCode}`;
}

function formatBytes(value: number) {
  if (!Number.isFinite(value) || value < 0) {
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
