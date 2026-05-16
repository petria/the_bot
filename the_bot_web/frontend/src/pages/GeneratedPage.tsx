import { Alert, Badge, Box, Card, Group, Loader, Stack, Table, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, Bot } from 'lucide-react';
import { useMemo } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { getGeneratedPage, type GeneratedPageResponse } from '../api/generatedPages';

interface GluggaCountRow {
  rank: number;
  nick: string;
  value: number;
}

interface GluggaCountsProps {
  channel: string;
  network: string;
  counterKey: string;
  counterName: string;
  generatedAt: string;
  rowCount: number;
  rows: GluggaCountRow[];
}

interface GluggaWeekdaySummaryRow {
  day: string;
  count: number;
  percent: number;
}

interface GluggaWeekdayNickRow {
  nick: string;
  bestDay: string;
  bestDayCount: number;
  bestDayPercent: number;
  totalCount: number;
}

interface GluggaWeekdayProps {
  channel: string;
  network: string;
  counterKey: string;
  counterName: string;
  generatedAt: string;
  totalCount: number;
  rawRowCount: number;
  skippedRowCount: number;
  nickCount: number;
  daySummary: GluggaWeekdaySummaryRow[];
  nickRows: GluggaWeekdayNickRow[];
}

export function GeneratedPage() {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  const pageQuery = useQuery({
    queryKey: ['generated-page', id, token],
    queryFn: () => getGeneratedPage(id || '', token),
    retry: false,
    enabled: Boolean(id && token),
  });

  return (
    <Box className="generated-page-shell">
      <Stack gap="lg">
        <Group justify="space-between" align="center" gap="sm">
          <Group gap="sm">
            <Bot size={24} />
            <Title order={2}>the_bot</Title>
          </Group>
          <Badge variant="light">Generated page</Badge>
        </Group>

        {!id || !token ? (
          <GeneratedPageError message="Generated page token is missing." />
        ) : null}
        {pageQuery.isLoading ? <Loader /> : null}
        {pageQuery.isError ? <GeneratedPageError error={pageQuery.error} /> : null}
        {pageQuery.data ? <GeneratedPageRenderer page={pageQuery.data} /> : null}
      </Stack>
    </Box>
  );
}

function GeneratedPageRenderer({ page }: { page: GeneratedPageResponse }) {
  if (page.componentType === 'GluggaCountsPage') {
    return <GluggaCountsPage page={page} />;
  }
  if (page.componentType === 'GluggaWeekdayPage') {
    return <GluggaWeekdayPage page={page} />;
  }

  return <GeneratedPageError message={`Unsupported generated page type: ${page.componentType}`} />;
}

function GluggaCountsPage({ page }: { page: GeneratedPageResponse }) {
  const props = normalizeGluggaCountsProps(page.props);
  const rows = useMemo(
    () => [...props.rows].sort((left, right) => right.value - left.value),
    [props.rows],
  );

  return (
    <Stack gap="md">
      <div>
        <Title order={1}>{page.title}</Title>
        <Text c="dimmed">
          {props.network} / {props.channel} / {props.counterKey}
        </Text>
      </div>

      <Card withBorder radius="sm">
        <Group gap="xl">
          <InfoValue label="Nicks" value={props.rowCount.toLocaleString('fi-FI')} />
          <InfoValue label="Generated" value={formatDateTime(page.createdAt || props.generatedAt)} />
          <InfoValue label="Expires" value={formatDateTime(page.expiresAt)} />
        </Group>
      </Card>

      {rows.length === 0 ? (
        <Alert color="blue">No {props.counterName} counts recorded for this channel.</Alert>
      ) : (
        <>
          <Table.ScrollContainer minWidth={520}>
            <Table striped highlightOnHover className="generated-counts-table">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th className="generated-rank-cell">Rank</Table.Th>
                  <Table.Th>Nick</Table.Th>
                  <Table.Th className="generated-count-cell">Count</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {rows.map((row, index) => (
                  <Table.Tr key={`${row.nick}-${index}`}>
                    <Table.Td className="generated-rank-cell">{index + 1}</Table.Td>
                    <Table.Td>
                      <Text fw={600} className="generated-nick">{row.nick}</Text>
                    </Table.Td>
                    <Table.Td className="generated-count-cell">
                      {row.value.toLocaleString('fi-FI')}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>

          <Stack gap="xs" className="generated-counts-cards">
            {rows.map((row, index) => (
              <Card withBorder radius="sm" key={`${row.nick}-${index}`}>
                <Group justify="space-between" gap="sm" wrap="nowrap">
                  <Group gap="sm" wrap="nowrap">
                    <Badge variant="light">#{index + 1}</Badge>
                    <Text fw={700} className="generated-nick">{row.nick}</Text>
                  </Group>
                  <Text fw={700}>{row.value.toLocaleString('fi-FI')}</Text>
                </Group>
              </Card>
            ))}
          </Stack>
        </>
      )}
    </Stack>
  );
}

function GluggaWeekdayPage({ page }: { page: GeneratedPageResponse }) {
  const props = normalizeGluggaWeekdayProps(page.props);
  const nickRows = useMemo(
    () => [...props.nickRows].sort((left, right) => right.totalCount - left.totalCount),
    [props.nickRows],
  );

  return (
    <Stack gap="md">
      <div>
        <Title order={1}>{page.title}</Title>
        <Text c="dimmed">
          {props.network} / {props.channel} / {props.counterKey}
        </Text>
      </div>

      <Card withBorder radius="sm">
        <Group gap="xl">
          <InfoValue label="Total count" value={props.totalCount.toLocaleString('fi-FI')} />
          <InfoValue label="Nicks" value={props.nickCount.toLocaleString('fi-FI')} />
          <InfoValue label="Rows" value={props.rawRowCount.toLocaleString('fi-FI')} />
          <InfoValue label="Generated" value={formatDateTime(page.createdAt || props.generatedAt)} />
          <InfoValue label="Expires" value={formatDateTime(page.expiresAt)} />
        </Group>
      </Card>

      {props.totalCount === 0 ? (
        <Alert color="blue">No {props.counterName} counts recorded for this channel.</Alert>
      ) : (
        <>
          <Card withBorder radius="sm">
            <Stack gap="sm">
              <Title order={3}>Weekdays</Title>
              <Table.ScrollContainer minWidth={520}>
                <Table striped highlightOnHover>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Day</Table.Th>
                      <Table.Th className="generated-count-cell">Count</Table.Th>
                      <Table.Th className="generated-percent-cell">Percent</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {props.daySummary.map((row) => (
                      <Table.Tr key={row.day}>
                        <Table.Td>
                          <Text fw={700}>{row.day}</Text>
                        </Table.Td>
                        <Table.Td className="generated-count-cell">
                          {row.count.toLocaleString('fi-FI')}
                        </Table.Td>
                        <Table.Td className="generated-percent-cell">
                          {formatPercent(row.percent)}
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Stack>
          </Card>

          <Stack gap="sm">
            <Title order={3}>Nick strongest weekdays</Title>
            <Table.ScrollContainer minWidth={720}>
              <Table striped highlightOnHover className="generated-weekday-nicks-table">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Nick</Table.Th>
                    <Table.Th>Best day</Table.Th>
                    <Table.Th className="generated-count-cell">Best count</Table.Th>
                    <Table.Th className="generated-percent-cell">Nick percent</Table.Th>
                    <Table.Th className="generated-count-cell">Total</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {nickRows.map((row) => (
                    <Table.Tr key={row.nick}>
                      <Table.Td>
                        <Text fw={600} className="generated-nick">{row.nick}</Text>
                      </Table.Td>
                      <Table.Td>{row.bestDay}</Table.Td>
                      <Table.Td className="generated-count-cell">
                        {row.bestDayCount.toLocaleString('fi-FI')}
                      </Table.Td>
                      <Table.Td className="generated-percent-cell">
                        {formatPercent(row.bestDayPercent)}
                      </Table.Td>
                      <Table.Td className="generated-count-cell">
                        {row.totalCount.toLocaleString('fi-FI')}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>

            <Stack gap="xs" className="generated-weekday-nicks-cards">
              {nickRows.map((row) => (
                <Card withBorder radius="sm" key={row.nick}>
                  <Stack gap={4}>
                    <Group justify="space-between" gap="sm" wrap="nowrap">
                      <Text fw={700} className="generated-nick">{row.nick}</Text>
                      <Badge variant="light">{row.bestDay}</Badge>
                    </Group>
                    <Text size="sm" c="dimmed">
                      {row.bestDayCount.toLocaleString('fi-FI')} of {row.totalCount.toLocaleString('fi-FI')} ({formatPercent(row.bestDayPercent)})
                    </Text>
                  </Stack>
                </Card>
              ))}
            </Stack>
          </Stack>
        </>
      )}

      {props.skippedRowCount > 0 ? (
        <Text size="sm" c="dimmed">
          Skipped {props.skippedRowCount.toLocaleString('fi-FI')} rows with invalid date or count values.
        </Text>
      ) : null}
    </Stack>
  );
}

function normalizeGluggaCountsProps(props: Record<string, unknown>): GluggaCountsProps {
  const rows = Array.isArray(props.rows) ? props.rows : [];
  return {
    channel: stringValue(props.channel),
    network: stringValue(props.network),
    counterKey: stringValue(props.counterKey),
    counterName: stringValue(props.counterName),
    generatedAt: stringValue(props.generatedAt),
    rowCount: numberValue(props.rowCount),
    rows: rows.map((row) => {
      const item = row as Record<string, unknown>;
      return {
        rank: numberValue(item.rank),
        nick: stringValue(item.nick),
        value: numberValue(item.value),
      };
    }),
  };
}

function normalizeGluggaWeekdayProps(props: Record<string, unknown>): GluggaWeekdayProps {
  const daySummary = Array.isArray(props.daySummary) ? props.daySummary : [];
  const nickRows = Array.isArray(props.nickRows) ? props.nickRows : [];
  return {
    channel: stringValue(props.channel),
    network: stringValue(props.network),
    counterKey: stringValue(props.counterKey),
    counterName: stringValue(props.counterName),
    generatedAt: stringValue(props.generatedAt),
    totalCount: numberValue(props.totalCount),
    rawRowCount: numberValue(props.rawRowCount),
    skippedRowCount: numberValue(props.skippedRowCount),
    nickCount: numberValue(props.nickCount),
    daySummary: daySummary.map((row) => {
      const item = row as Record<string, unknown>;
      return {
        day: stringValue(item.day),
        count: numberValue(item.count),
        percent: numberValue(item.percent),
      };
    }),
    nickRows: nickRows.map((row) => {
      const item = row as Record<string, unknown>;
      return {
        nick: stringValue(item.nick),
        bestDay: stringValue(item.bestDay),
        bestDayCount: numberValue(item.bestDayCount),
        bestDayPercent: numberValue(item.bestDayPercent),
        totalCount: numberValue(item.totalCount),
      };
    }),
  };
}

function InfoValue({ label, value }: { label: string; value: string }) {
  return (
    <div className="info-item">
      <Text size="xs" c="dimmed" fw={600}>{label}</Text>
      <Text fw={700} className="info-value">{value}</Text>
    </div>
  );
}

function GeneratedPageError({ error, message }: { error?: Error; message?: string }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{message || apiError?.message || 'Could not load generated page.'}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}

function stringValue(value: unknown) {
  return typeof value === 'string' ? value : '';
}

function numberValue(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0;
}

function formatPercent(value: number) {
  return `${value.toLocaleString('fi-FI', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })} %`;
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  return new Intl.DateTimeFormat('fi-FI', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date);
}
