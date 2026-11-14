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
import { AlertCircle, Copy, ExternalLink, Link2, Search } from 'lucide-react';
import { ApiError } from '../api/client';
import { getCollectedUrls, type CollectedUrlItem } from '../api/adminCollectedUrls';

export function AdminCollectedUrlsPage() {
  const [providerFilter, setProviderFilter] = useState<string | null>('all');
  const [textFilter, setTextFilter] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const urlsQuery = useQuery({
    queryKey: ['admin-collected-urls'],
    queryFn: getCollectedUrls,
    refetchInterval: 30000,
  });

  const items = urlsQuery.data?.items ?? [];
  const providers = useMemo(() => {
    const values = Array.from(new Set(items.map((item) => item.provider || 'Web'))).sort();
    return [{ label: 'All providers', value: 'all' }, ...values.map((value) => ({ label: value, value }))];
  }, [items]);
  const visibleItems = useMemo(() => {
    const query = textFilter.trim().toLowerCase();
    return items.filter((item) => {
      if (providerFilter && providerFilter !== 'all' && (item.provider || 'Web') !== providerFilter) {
        return false;
      }
      if (!query) {
        return true;
      }
      return [
        item.url,
        item.title,
        item.description,
        item.author,
        item.sourceSender,
        item.sourceChannelAlias,
        item.sourceChannelName,
        item.sourceNetwork,
        item.sourceProtocol,
        item.provider,
      ]
          .filter(Boolean)
          .some((value) => value?.toLowerCase().includes(query));
    });
  }, [items, providerFilter, textFilter]);

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start">
        <div>
          <Title order={2}>Collected URLs</Title>
          <Text c="dimmed">Browse URLs captured from connected chat channels.</Text>
        </div>
        <Button variant="light" onClick={() => urlsQuery.refetch()} loading={urlsQuery.isFetching}>
          Refresh
        </Button>
      </Group>

      {urlsQuery.isLoading ? <Loader /> : null}
      {urlsQuery.isError ? <CollectedUrlsError error={urlsQuery.error} /> : null}
      {urlsQuery.data && !urlsQuery.data.enabled ? (
        <Alert color="yellow" variant="light" icon={<AlertCircle size={18} />}>
          Media storage is disabled.
        </Alert>
      ) : null}
      {urlsQuery.data?.detail ? (
        <Alert color={urlsQuery.data.enabled ? 'yellow' : 'red'} variant="light" icon={<AlertCircle size={18} />}>
          {urlsQuery.data.detail}
        </Alert>
      ) : null}

      <Card withBorder radius="sm">
        <Stack gap="md">
          <Group grow align="flex-end" className="field-row">
            <TextInput
              label="Filter"
              placeholder="Title, URL, sender, channel, network"
              leftSection={<Search size={16} />}
              value={textFilter}
              onChange={(event) => setTextFilter(event.currentTarget.value)}
            />
            <Select
              label="Provider"
              value={providerFilter}
              data={providers}
              onChange={setProviderFilter}
              allowDeselect={false}
            />
          </Group>

          <Group gap="xs">
            <Badge variant="light">{visibleItems.length.toLocaleString('fi-FI')} shown</Badge>
            <Badge variant="light" color="gray">{items.length.toLocaleString('fi-FI')} total</Badge>
            {urlsQuery.data?.storageDir ? <Text size="sm" c="dimmed">{urlsQuery.data.storageDir}</Text> : null}
          </Group>

          <Table.ScrollContainer minWidth={1050} className="data-table">
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Provider</Table.Th>
                  <Table.Th>Title</Table.Th>
                  <Table.Th>Source</Table.Th>
                  <Table.Th>Sender</Table.Th>
                  <Table.Th>Created</Table.Th>
                  <Table.Th>Expires</Table.Th>
                  <Table.Th>URL</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {visibleItems.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={7}>
                      <Text c="dimmed">No collected URLs found.</Text>
                    </Table.Td>
                  </Table.Tr>
                ) : visibleItems.map((item) => (
                  <Table.Tr key={item.id}>
                    <Table.Td><ProviderCell item={item} /></Table.Td>
                    <Table.Td><TitleCell item={item} /></Table.Td>
                    <Table.Td><SourceCell item={item} /></Table.Td>
                    <Table.Td>{item.sourceSender || '-'}</Table.Td>
                    <Table.Td>{formatDateTime(item.createdAt)}</Table.Td>
                    <Table.Td>{formatDateTime(item.expiresAt)}</Table.Td>
                    <Table.Td>
                      <UrlActions
                        item={item}
                        copied={copiedId === item.id}
                        onCopy={() => copyUrl(item)}
                      />
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>

          <CollectedUrlCards
            items={visibleItems}
            copiedId={copiedId}
            onCopy={copyUrl}
          />
        </Stack>
      </Card>
    </Stack>
  );

  async function copyUrl(item: CollectedUrlItem) {
    await navigator.clipboard.writeText(item.url);
    setCopiedId(item.id);
    window.setTimeout(() => setCopiedId((current) => current === item.id ? null : current), 1500);
  }
}

function ProviderCell({ item }: { item: CollectedUrlItem }) {
  return (
    <Group gap="xs" wrap="nowrap">
      <Link2 size={18} />
      <Badge variant="light">{item.provider || 'Web'}</Badge>
    </Group>
  );
}

function TitleCell({ item }: { item: CollectedUrlItem }) {
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

function SourceCell({ item }: { item: CollectedUrlItem }) {
  return (
    <Stack gap={1}>
      <Text>{item.sourceChannelAlias || item.sourceChannelName || '-'}</Text>
      <Text size="xs" c="dimmed">{[item.sourceProtocol, item.sourceNetwork].filter(Boolean).join(' / ') || '-'}</Text>
    </Stack>
  );
}

function CollectedUrlCards({
  items,
  copiedId,
  onCopy,
}: {
  items: CollectedUrlItem[];
  copiedId: string | null;
  onCopy: (item: CollectedUrlItem) => void;
}) {
  if (items.length === 0) {
    return (
      <Stack gap="sm" className="data-cards">
        <Card withBorder radius="sm">
          <Text c="dimmed">No collected URLs found.</Text>
        </Card>
      </Stack>
    );
  }

  return (
    <Stack gap="sm" className="data-cards">
      {items.map((item) => (
        <Card withBorder radius="sm" key={item.id}>
          <Stack gap="xs">
            <ProviderCell item={item} />
            <Stack gap={2} className="info-row">
              <Text size="xs" c="dimmed" fw={600}>Title</Text>
              <TitleCell item={item} />
            </Stack>
            <Stack gap={2} className="info-row">
              <Text size="xs" c="dimmed" fw={600}>Source</Text>
              <SourceCell item={item} />
            </Stack>
            <InfoLine label="Sender" value={item.sourceSender || '-'} />
            <InfoLine label="Created" value={formatDateTime(item.createdAt)} />
            <InfoLine label="Expires" value={formatDateTime(item.expiresAt)} />
            <InfoLine label="URL" value={item.url} />
            <UrlActions
              item={item}
              touch
              copied={copiedId === item.id}
              onCopy={() => onCopy(item)}
            />
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function UrlActions({
  item,
  copied,
  onCopy,
  touch,
}: {
  item: CollectedUrlItem;
  copied: boolean;
  onCopy: () => void;
  touch?: boolean;
}) {
  return (
    <Group gap="xs" wrap="nowrap" className={touch ? 'action-row' : undefined}>
      <Tooltip label="Open original">
        <ActionIcon
          component="a"
          href={item.url}
          target="_blank"
          rel="noreferrer"
          variant="light"
          size={touch ? 'lg' : undefined}
          aria-label="Open original URL"
        >
          <ExternalLink size={touch ? 20 : 16} />
        </ActionIcon>
      </Tooltip>
      <Tooltip label={copied ? 'Copied' : 'Copy original URL'}>
        <ActionIcon
          variant="light"
          size={touch ? 'lg' : undefined}
          aria-label="Copy original URL"
          onClick={onCopy}
        >
          <Copy size={touch ? 20 : 16} />
        </ActionIcon>
      </Tooltip>
    </Group>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <Group justify="space-between" gap="sm" wrap="nowrap" className="info-row">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text size="sm" ta="right" className="info-value">{value || '-'}</Text>
    </Group>
  );
}

function CollectedUrlsError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" variant="light" icon={<AlertCircle size={18} />}>
      <Text fw={700}>Could not load collected URLs</Text>
      <Text>{apiError?.message || error.message}</Text>
      {apiError?.detail ? <Text size="sm">{apiError.detail}</Text> : null}
    </Alert>
  );
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
