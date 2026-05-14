import {
  ActionIcon,
  Alert,
  Button,
  Card,
  Group,
  Loader,
  NumberInput,
  Select,
  SimpleGrid,
  Stack,
  Switch,
  Tabs,
  Text,
  TextInput,
  Title,
  Tooltip,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Plus, Save, Trash2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AdminConfigChannel,
  AdminBotConfig,
  AdminConnectionConfigPayload,
  AdminDiscordConfig,
  AdminIrcServerConfig,
  AdminTelegramConfig,
  AdminWhatsAppConfig,
  getAdminConnectionConfig,
  PromoteChannelState,
  saveAndApplyAdminConnectionConfig,
  saveAdminConnectionConfig,
} from '../api/adminConnectionConfig';
import { ApiError } from '../api/client';

const emptyChannel: AdminConfigChannel = {
  id: null,
  description: null,
  name: null,
  type: null,
  echoToAlias: null,
  echoToAliases: [],
  joinOnStart: false,
};

const emptyDiscord: AdminDiscordConfig = {
  connectStartup: false,
  theBotUserId: null,
  channelList: [],
};

const emptyBotConfig: AdminBotConfig = {
  botName: null,
  ircRealName: null,
};

const emptyTelegram: AdminTelegramConfig = {
  telegramName: null,
  connectStartup: false,
  channelList: [],
};

const emptyWhatsApp: AdminWhatsAppConfig = {
  network: null,
  sendBaseUrl: null,
  connectStartup: false,
  channelList: [],
};

export function AdminConnectionConfigPage() {
  const queryClient = useQueryClient();
  const location = useLocation();
  const navigate = useNavigate();
  const promotedRef = useRef(false);
  const [config, setConfig] = useState<AdminConnectionConfigPayload | null>(null);
  const [activeTab, setActiveTab] = useState<string | null>('irc');

  const configQuery = useQuery({
    queryKey: ['admin-connection-config'],
    queryFn: getAdminConnectionConfig,
  });

  const saveMutation = useMutation({
    mutationFn: saveAdminConnectionConfig,
    onSuccess: (response) => {
      setConfig(normalizePayload(response.config));
      queryClient.setQueryData(['admin-connection-config'], response);
    },
  });

  const applyMutation = useMutation({
    mutationFn: saveAndApplyAdminConnectionConfig,
    onSuccess: (response) => {
      setConfig(normalizePayload(response.savedConfig.config));
      queryClient.setQueryData(['admin-connection-config'], response.savedConfig);
    },
  });

  useEffect(() => {
    if (configQuery.data?.config && !config) {
      setConfig(normalizePayload(configQuery.data.config));
    }
  }, [configQuery.data, config]);

  useEffect(() => {
    const promote = (location.state as { promote?: PromoteChannelState } | null)?.promote;
    if (!promote || !config || promotedRef.current) {
      return;
    }
    promotedRef.current = true;
    setConfig(addPromotedChannel(config, promote));
    setActiveTab(tabFor(promote.connectionType));
    navigate(location.pathname, { replace: true, state: null });
  }, [config, location.pathname, location.state, navigate]);

  const handleSave = () => {
    if (config) {
      saveMutation.mutate(config);
    }
  };

  const handleSaveAndApply = () => {
    if (config) {
      applyMutation.mutate(config);
    }
  };

  if (configQuery.isLoading || !config) {
    return <Loader />;
  }

  if (configQuery.isError) {
    return <ConfigError error={configQuery.error} />;
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Connection Config</Title>
          <Text c="dimmed">
            Profile {configQuery.data?.profile || '-'} from {configQuery.data?.configFile}
          </Text>
        </div>
        <Group gap="sm">
          <Button
            variant="light"
            leftSection={<Save size={16} />}
            onClick={handleSave}
            loading={saveMutation.isPending}
          >
            Save
          </Button>
          <Button
            leftSection={<Save size={16} />}
            onClick={handleSaveAndApply}
            loading={applyMutation.isPending}
          >
            Save and apply
          </Button>
        </Group>
      </Group>

      <Alert icon={<AlertTriangle size={18} />} color="yellow" variant="light">
        Saving writes the runtime config only. Running bot services need restart/apply before connection changes are in use.
        Secret values are preserved but not shown here.
      </Alert>

      {saveMutation.isSuccess && (
        <Alert color="green" variant="light">Connection config saved.</Alert>
      )}
      {saveMutation.isError && <ConfigError error={saveMutation.error} />}
      {applyMutation.isSuccess && (
        <Alert color={applyMutation.data.status === 'OK' ? 'green' : 'yellow'} variant="light">
          <Stack gap="xs">
            <Text fw={700}>Save and apply finished: {applyMutation.data.status}</Text>
            {applyMutation.data.targets.map((target) => (
              <Text key={target.target} size="sm">
                {target.target}: {target.status}{target.message ? ` - ${target.message}` : ''}
              </Text>
            ))}
          </Stack>
        </Alert>
      )}
      {applyMutation.isError && <ConfigError error={applyMutation.error} />}

      <Tabs value={activeTab} onChange={setActiveTab} keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="irc">IRC</Tabs.Tab>
          <Tabs.Tab value="discord">Discord</Tabs.Tab>
          <Tabs.Tab value="telegram">Telegram</Tabs.Tab>
          <Tabs.Tab value="whatsapp">WhatsApp</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="irc" pt="md">
          <IrcConfigsEditor
            botConfig={config.botConfig ?? emptyBotConfig}
            configs={config.ircServerConfigs ?? []}
            onBotConfigChange={(botConfig) => setConfig({ ...config, botConfig })}
            onChange={(ircServerConfigs) => setConfig({ ...config, ircServerConfigs })}
          />
        </Tabs.Panel>

        <Tabs.Panel value="discord" pt="md">
          <DiscordEditor
            config={config.discordConfig ?? emptyDiscord}
            onChange={(discordConfig) => setConfig({ ...config, discordConfig })}
          />
        </Tabs.Panel>

        <Tabs.Panel value="telegram" pt="md">
          <TelegramEditor
            config={config.telegramConfig ?? emptyTelegram}
            onChange={(telegramConfig) => setConfig({ ...config, telegramConfig })}
          />
        </Tabs.Panel>

        <Tabs.Panel value="whatsapp" pt="md">
          <WhatsAppEditor
            config={config.whatsappConfig ?? emptyWhatsApp}
            onChange={(whatsappConfig) => setConfig({ ...config, whatsappConfig })}
          />
        </Tabs.Panel>
      </Tabs>
    </Stack>
  );
}

function IrcConfigsEditor({
  botConfig,
  configs,
  onBotConfigChange,
  onChange,
}: {
  botConfig: AdminBotConfig;
  configs: AdminIrcServerConfig[];
  onBotConfigChange: (config: AdminBotConfig) => void;
  onChange: (configs: AdminIrcServerConfig[]) => void;
}) {
  const addConfig = () => onChange([
    ...configs,
    { name: 'IRC_CONNECTION', connectStartup: false, networkName: null, host: null, port: 6667, channelList: [] },
  ]);

  return (
    <Stack gap="md">
      <Card withBorder radius="sm">
        <Stack gap="md">
          <Title order={3}>IRC identity</Title>
          <SimpleGrid cols={{ base: 1, sm: 2 }}>
            <TextInput
              label="Bot nick"
              value={botConfig.botName ?? ''}
              onChange={(event) => onBotConfigChange({ ...botConfig, botName: event.currentTarget.value })}
            />
            <TextInput
              label="IRC real name"
              description="Shown as ircname in IRC WHOIS replies"
              value={botConfig.ircRealName ?? ''}
              onChange={(event) => onBotConfigChange({ ...botConfig, ircRealName: event.currentTarget.value })}
            />
          </SimpleGrid>
        </Stack>
      </Card>
      <Group justify="flex-end">
        <Button leftSection={<Plus size={16} />} variant="light" onClick={addConfig}>
          Add IRC server
        </Button>
      </Group>
      {configs.length === 0 ? <EmptyState text="No IRC configs." /> : null}
      {configs.map((config, index) => (
        <Card withBorder radius="sm" key={`irc-config-${index}`}>
          <Stack gap="md">
            <Group justify="space-between" gap="sm">
              <Title order={3}>{config.name || 'IRC config'}</Title>
              <Tooltip label="Remove IRC server">
                <ActionIcon
                  color="red"
                  variant="subtle"
                  aria-label="Remove IRC server"
                  onClick={() => onChange(configs.filter((_, i) => i !== index))}
                >
                  <Trash2 size={18} />
                </ActionIcon>
              </Tooltip>
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
              <TextInput
                label="Config name"
                value={config.name ?? ''}
                onChange={(event) => updateIrc(configs, index, { name: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Network"
                value={config.networkName ?? ''}
                onChange={(event) => updateIrc(configs, index, { networkName: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Host"
                value={config.host ?? ''}
                onChange={(event) => updateIrc(configs, index, { host: event.currentTarget.value }, onChange)}
              />
              <NumberInput
                label="Port"
                value={config.port || 6667}
                min={1}
                max={65535}
                onChange={(value) => updateIrc(configs, index, { port: numericValue(value, 6667) }, onChange)}
              />
            </SimpleGrid>
            <Switch
              label="Connect on startup"
              checked={config.connectStartup}
              onChange={(event) => updateIrc(configs, index, { connectStartup: event.currentTarget.checked }, onChange)}
            />
            <ChannelsEditor
              channels={config.channelList ?? []}
              onChange={(channelList) => updateIrc(configs, index, { channelList }, onChange)}
            />
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function DiscordEditor({
  config,
  onChange,
}: {
  config: AdminDiscordConfig;
  onChange: (config: AdminDiscordConfig) => void;
}) {
  return (
    <Stack gap="md">
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <NumberInput
          label="Bot user ID"
          value={config.theBotUserId ?? ''}
          onChange={(value) => onChange({ ...config, theBotUserId: nullableNumber(value) })}
        />
        <Switch
          label="Connect on startup"
          checked={config.connectStartup}
          onChange={(event) => onChange({ ...config, connectStartup: event.currentTarget.checked })}
        />
      </SimpleGrid>
      <SecretNotice service="Discord" />
      <ChannelsEditor
        channels={config.channelList ?? []}
        onChange={(channelList) => onChange({ ...config, channelList })}
      />
    </Stack>
  );
}

function TelegramEditor({
  config,
  onChange,
}: {
  config: AdminTelegramConfig;
  onChange: (config: AdminTelegramConfig) => void;
}) {
  return (
    <Stack gap="md">
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <TextInput
          label="Telegram name"
          value={config.telegramName ?? ''}
          onChange={(event) => onChange({ ...config, telegramName: event.currentTarget.value })}
        />
        <Switch
          label="Connect on startup"
          checked={config.connectStartup}
          onChange={(event) => onChange({ ...config, connectStartup: event.currentTarget.checked })}
        />
      </SimpleGrid>
      <SecretNotice service="Telegram" />
      <ChannelsEditor
        channels={config.channelList ?? []}
        onChange={(channelList) => onChange({ ...config, channelList })}
      />
    </Stack>
  );
}

function WhatsAppEditor({
  config,
  onChange,
}: {
  config: AdminWhatsAppConfig;
  onChange: (config: AdminWhatsAppConfig) => void;
}) {
  return (
    <Stack gap="md">
      <SimpleGrid cols={{ base: 1, sm: 3 }}>
        <TextInput
          label="Network"
          value={config.network ?? ''}
          onChange={(event) => onChange({ ...config, network: event.currentTarget.value })}
        />
        <TextInput
          label="Send base URL"
          value={config.sendBaseUrl ?? ''}
          onChange={(event) => onChange({ ...config, sendBaseUrl: event.currentTarget.value })}
        />
        <Switch
          label="Connect on startup"
          checked={config.connectStartup}
          onChange={(event) => onChange({ ...config, connectStartup: event.currentTarget.checked })}
        />
      </SimpleGrid>
      <SecretNotice service="WhatsApp" />
      <ChannelsEditor
        channels={config.channelList ?? []}
        onChange={(channelList) => onChange({ ...config, channelList })}
      />
    </Stack>
  );
}

function ChannelsEditor({
  channels,
  onChange,
}: {
  channels: AdminConfigChannel[];
  onChange: (channels: AdminConfigChannel[]) => void;
}) {
  const addChannel = () => onChange([...channels, { ...emptyChannel, echoToAliases: [] }]);

  return (
    <Stack gap="sm">
      <Group justify="space-between" gap="sm">
        <Title order={4}>Channels</Title>
        <Button leftSection={<Plus size={16} />} variant="light" size="xs" onClick={addChannel}>
          Add channel
        </Button>
      </Group>
      {channels.length === 0 ? <EmptyState text="No configured channels." /> : null}
      {channels.map((channel, index) => (
        <Card withBorder radius="sm" key={`channel-${index}`}>
          <Stack gap="sm">
            <Group justify="space-between" gap="sm">
              <Text fw={700}>{channel.name || channel.echoToAlias || channel.id || 'Channel'}</Text>
              <Tooltip label="Remove channel">
                <ActionIcon
                  color="red"
                  variant="subtle"
                  aria-label="Remove channel"
                  onClick={() => onChange(channels.filter((_, i) => i !== index))}
                >
                  <Trash2 size={18} />
                </ActionIcon>
              </Tooltip>
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2, lg: 3 }}>
              <TextInput
                label="ID"
                value={channel.id ?? ''}
                onChange={(event) => updateChannel(channels, index, { id: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Name"
                value={channel.name ?? ''}
                onChange={(event) => updateChannel(channels, index, { name: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Type"
                value={channel.type ?? ''}
                onChange={(event) => updateChannel(channels, index, { type: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Alias"
                value={channel.echoToAlias ?? ''}
                onChange={(event) => updateChannel(channels, index, { echoToAlias: event.currentTarget.value }, onChange)}
              />
              <TextInput
                label="Echo to aliases"
                description="Comma separated target channel aliases"
                value={(channel.echoToAliases ?? []).join(', ')}
                onChange={(event) => updateChannel(
                  channels,
                  index,
                  { echoToAliases: splitAliases(event.currentTarget.value) },
                  onChange,
                )}
              />
              <TextInput
                label="Description"
                value={channel.description ?? ''}
                onChange={(event) => updateChannel(channels, index, { description: event.currentTarget.value }, onChange)}
              />
            </SimpleGrid>
            <Switch
              label="Join on start"
              checked={channel.joinOnStart}
              onChange={(event) => updateChannel(channels, index, { joinOnStart: event.currentTarget.checked }, onChange)}
            />
          </Stack>
        </Card>
      ))}
    </Stack>
  );
}

function SecretNotice({ service }: { service: string }) {
  return (
    <Text size="sm" c="dimmed">
      {service} secrets are preserved from the current config file and are not editable here.
    </Text>
  );
}

function ConfigError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;

  return (
    <Alert color="red" variant="light">
      <Stack gap={2}>
        <Text fw={700}>{apiError?.message || 'Could not load connection config.'}</Text>
        {apiError?.detail && <Text size="sm">{apiError.detail}</Text>}
      </Stack>
    </Alert>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <Card withBorder radius="sm">
      <Text c="dimmed">{text}</Text>
    </Card>
  );
}

function updateIrc(
  configs: AdminIrcServerConfig[],
  index: number,
  patch: Partial<AdminIrcServerConfig>,
  onChange: (configs: AdminIrcServerConfig[]) => void,
) {
  onChange(configs.map((config, i) => (i === index ? { ...config, ...patch } : config)));
}

function updateChannel(
  channels: AdminConfigChannel[],
  index: number,
  patch: Partial<AdminConfigChannel>,
  onChange: (channels: AdminConfigChannel[]) => void,
) {
  onChange(channels.map((channel, i) => (i === index ? { ...channel, ...patch } : channel)));
}

function normalizePayload(payload: AdminConnectionConfigPayload): AdminConnectionConfigPayload {
  return {
    botConfig: payload.botConfig ?? emptyBotConfig,
    ircServerConfigs: payload.ircServerConfigs ?? [],
    discordConfig: payload.discordConfig ?? emptyDiscord,
    telegramConfig: payload.telegramConfig ?? emptyTelegram,
    whatsappConfig: payload.whatsappConfig ?? emptyWhatsApp,
  };
}

function addPromotedChannel(
  payload: AdminConnectionConfigPayload,
  promote: PromoteChannelState,
): AdminConnectionConfigPayload {
  const channel = {
    ...emptyChannel,
    ...promote.channel,
    echoToAliases: promote.channel.echoToAliases ?? [],
  };
  const type = promote.connectionType || '';

  if (type.includes('IRC')) {
    const ircServerConfigs = [...(payload.ircServerConfigs ?? [])];
    const targetIndex = Math.max(0, ircServerConfigs.findIndex((config) => config.networkName === promote.network));
    if (ircServerConfigs.length === 0) {
      ircServerConfigs.push({
        name: promote.network || 'IRC_CONNECTION',
        connectStartup: false,
        networkName: promote.network,
        host: null,
        port: 6667,
        channelList: [channel],
      });
    } else {
      ircServerConfigs[targetIndex] = {
        ...ircServerConfigs[targetIndex],
        channelList: [...(ircServerConfigs[targetIndex].channelList ?? []), channel],
      };
    }
    return { ...payload, ircServerConfigs };
  }

  if (type.includes('DISCORD')) {
    const discordConfig = payload.discordConfig ?? emptyDiscord;
    return { ...payload, discordConfig: appendChannel(discordConfig, channel) };
  }
  if (type.includes('TELEGRAM')) {
    const telegramConfig = payload.telegramConfig ?? emptyTelegram;
    return { ...payload, telegramConfig: appendChannel(telegramConfig, channel) };
  }
  if (type.includes('WHATSAPP')) {
    const whatsappConfig = payload.whatsappConfig ?? emptyWhatsApp;
    return { ...payload, whatsappConfig: appendChannel(whatsappConfig, channel) };
  }
  return payload;
}

function appendChannel<T extends { channelList: AdminConfigChannel[] | null }>(config: T, channel: AdminConfigChannel): T {
  return {
    ...config,
    channelList: [...(config.channelList ?? []), channel],
  };
}

function tabFor(connectionType: string | null) {
  const type = connectionType || '';
  if (type.includes('DISCORD')) {
    return 'discord';
  }
  if (type.includes('TELEGRAM')) {
    return 'telegram';
  }
  if (type.includes('WHATSAPP')) {
    return 'whatsapp';
  }
  return 'irc';
}

function splitAliases(value: string) {
  return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
}

function numericValue(value: string | number, fallback: number) {
  return typeof value === 'number' ? value : fallback;
}

function nullableNumber(value: string | number) {
  if (value === '' || value == null) {
    return null;
  }
  return typeof value === 'number' ? value : Number(value);
}
