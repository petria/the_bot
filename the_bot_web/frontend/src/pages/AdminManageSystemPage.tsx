import { Alert, Badge, Button, Card, Group, Loader, Stack, Switch, Text, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { getSystemSettings, saveSystemSettings, type SystemSettings } from '../api/adminSystemSettings';
import {
  DEFAULT_DEVELOP_URL,
  DEFAULT_MAIN_URL,
  getWebUiLinks,
  saveWebUiLinks,
  type WebUiLinksSettings,
  type WebUiLinksUpdate,
} from '../api/adminWebUiLinks';

export function AdminManageSystemPage() {
  const client = useQueryClient();
  const query = useQuery({ queryKey: ['admin-system-settings'], queryFn: getSystemSettings });
  const [draft, setDraft] = useState<SystemSettings | null>(null);
  const settings = draft ?? query.data;
  const save = useMutation({
    mutationFn: (settings: SystemSettings) => saveSystemSettings(settings),
    onSuccess: (response) => {
      client.setQueryData(['admin-system-settings'], response);
      setDraft(null);
    },
  });
  const changed = !!settings && !!query.data && settings.waterTemperatureToWatch !== query.data.waterTemperatureToWatch;

  const linksQuery = useQuery({ queryKey: ['admin-webui-links'], queryFn: getWebUiLinks });
  const [linksDraft, setLinksDraft] = useState<WebUiLinksUpdate | null>(null);
  const links = linksDraft ?? (linksQuery.data
    ? { mainUrl: linksQuery.data.main.url, developUrl: linksQuery.data.develop.url }
    : null);
  const linksChanged = !!links && !!linksQuery.data && (
    links.mainUrl !== linksQuery.data.main.url || links.developUrl !== linksQuery.data.develop.url);
  const saveLinks = useMutation({
    mutationFn: (update: WebUiLinksUpdate) => saveWebUiLinks(update),
    onSuccess: (response) => {
      client.setQueryData(['admin-webui-links'], response);
      setLinksDraft(null);
    },
  });

  const linkFields: Array<{ key: keyof WebUiLinksUpdate; label: string; link: () => WebUiLinksSettings['main'] | undefined; fallback: WebUiLinksSettings['main']; defaultUrl: string }> = [
    { key: 'mainUrl', label: 'Main instance URL', link: () => linksQuery.data?.main, fallback: { url: DEFAULT_MAIN_URL, configured: false }, defaultUrl: DEFAULT_MAIN_URL },
    { key: 'developUrl', label: 'Develop instance URL', link: () => linksQuery.data?.develop, fallback: { url: DEFAULT_DEVELOP_URL, configured: false }, defaultUrl: DEFAULT_DEVELOP_URL },
  ];

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" className="field-row">
        <div>
          <Title order={2}>Manage System</Title>
          <Text c="dimmed">Configure system features for this bot instance.</Text>
        </div>
        <Button className="stack-fixed" leftSection={<Save size={18} />} loading={save.isPending}
          disabled={!changed || query.isError || save.isPending} onClick={() => settings && save.mutate(settings)}>Save</Button>
      </Group>
      {query.isLoading && <Loader />}
      {query.isError && <Alert color="red">{query.error.message}</Alert>}
      {save.isError && <Alert color="red">{save.error.message}</Alert>}
      {save.isSuccess && <Alert color="green">System settings saved.</Alert>}
      {settings && !query.isError && (
        <Card withBorder radius="sm">
          <Switch label="Water temperature to watch" onLabel="On" offLabel="Off"
            checked={settings.waterTemperatureToWatch} disabled={save.isPending}
            onChange={(event) => {
              save.reset();
              setDraft({ waterTemperatureToWatch: event.currentTarget.checked });
            }} />
        </Card>
      )}

      <Card withBorder radius="sm">
        <Title order={3}>WebUI links</Title>
        <Text c="dimmed" size="sm">Bot-controlled Web UI links offered to the Android app.</Text>
        {linksQuery.isLoading && <Loader />}
        {linksQuery.isError && <Alert color="red">{linksQuery.error.message}</Alert>}
        {saveLinks.isError && <Alert color="red">{saveLinks.error.message}</Alert>}
        {saveLinks.isSuccess && <Alert color="green">WebUI links saved.</Alert>}
        {links && !linksQuery.isError && (
          <Stack gap="sm" mt="sm">
            {linkFields.map(({ key, label, link, fallback, defaultUrl }) => {
              const current = link() ?? fallback;
              return (
                <TextInput key={key} label={
                  <Group gap="xs">
                    {label}
                    <Badge size="xs" variant="light" color={current.configured ? 'blue' : 'gray'}>
                      {current.configured ? 'custom' : 'default'}
                    </Badge>
                  </Group>
                } value={links[key]} disabled={saveLinks.isPending}
                  onChange={(event) => {
                    saveLinks.reset();
                    setLinksDraft({ ...links, [key]: event.currentTarget.value });
                  }}
                  description={`Default: ${defaultUrl}. Clear the field to restore it.`} />
              );
            })}
            <Button variant="secondary" leftSection={<Save size={18} />} loading={saveLinks.isPending}
              disabled={!linksChanged || linksQuery.isError || saveLinks.isPending}
              onClick={() => links && saveLinks.mutate({ mainUrl: links.mainUrl, developUrl: links.developUrl })}>Save links</Button>
          </Stack>
        )}
      </Card>
    </Stack>
  );
}
