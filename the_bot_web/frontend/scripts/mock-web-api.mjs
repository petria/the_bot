/**
 * Dev-only mock of the bot web API for the phone-width screenshot pass.
 *
 * It answers on 127.0.0.1:8091, which is already the Vite dev proxy target, so
 * `npm run dev` needs no extra configuration. Fixtures are deliberately small and
 * mirror the payloads used by the page tests.
 *
 * Usage: node scripts/mock-web-api.mjs
 */
import { createServer } from 'node:http';

const port = Number(process.env.MOCK_WEB_API_PORT ?? 8091);
// The Vite proxy target is http://localhost:8091, which resolves to ::1 on this host,
// so the mock answers on both loopback families.
const hosts = (process.env.MOCK_WEB_API_HOST ?? '127.0.0.1,::1').split(',').map((host) => host.trim()).filter(Boolean);

const isoDate = (offsetMinutes) => new Date(Date.now() + offsetMinutes * 60_000).toISOString();

const me = {
  id: 1,
  username: 'petria',
  name: 'Petri Airio',
  email: 'petri@example.test',
  ircNick: '_Pete_',
  telegramId: null,
  discordId: null,
  whatsappId: null,
  homeChannel: {
    connectionType: 'IRC_CONNECTION',
    network: 'IRCNet',
    echoToAlias: 'IRC-TEST',
    label: '#bot-test',
  },
  permissions: ['*'],
  roles: ['user'],
};

const notifyRules = {
  rules: [{
    id: 'rule-1',
    username: 'petria',
    enabled: true,
    autoReplyEnabled: false,
    sourceEchoToAlias: 'IRC-TEST',
    sourceDisplayName: '#bot-test',
    patternType: 'PRESET_MENTION',
    pattern: null,
    destinationConnectionType: 'SIGNAL',
    cooldownSeconds: 60,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  }],
};

const systemStatus = {
  checkedAt: isoDate(0),
  components: [
    {
      name: 'bot-io',
      status: 'UP',
      componentType: 'SPRING_BOOT',
      runtimeMode: 'docker',
      healthUrl: 'http://bot-io:8091/actuator/health',
      healthStatus: 'UP',
      baseUrl: 'http://bot-io:8091',
      profiles: 'prod',
      version: '0.9.4',
      artifact: 'bot-io.jar',
      uptimeSeconds: 94_000,
      startedAt: isoDate(-1_560),
      receivedCalls: 128_400,
      requestedCalls: 64_200,
      responseTimeMs: 12,
      checkedAt: isoDate(0),
      containerName: 'the_bot_bot_io',
      containerState: 'running',
      containerStatusText: 'Up 26 hours',
      image: 'the_bot/bot-io:latest',
      containerStartedAt: isoDate(-1_560),
      restartCount: 0,
      containerError: null,
      error: null,
    },
    {
      name: 'hermes-gateway',
      status: 'DEGRADED',
      componentType: 'HERMES_GATEWAY',
      runtimeMode: 'docker',
      healthUrl: 'http://hermes-gateway:8000/health',
      healthStatus: 'DEGRADED',
      baseUrl: 'http://hermes-gateway:8000',
      profiles: 'gpt-6.1-sol',
      version: '30',
      artifact: 'hermes-gateway',
      uptimeSeconds: 8_400,
      startedAt: isoDate(-140),
      receivedCalls: null,
      requestedCalls: null,
      responseTimeMs: 240,
      checkedAt: isoDate(0),
      containerName: 'the_bot_hermes_gateway',
      containerState: 'running',
      containerStatusText: 'Up 2 hours',
      image: 'the_bot/hermes-gateway:latest',
      containerStartedAt: isoDate(-140),
      restartCount: 1,
      containerError: null,
      error: 'Model list timed out once in the last minute.',
    },
    {
      name: 'bot-clean',
      status: 'DOWN',
      componentType: 'SIDECAR',
      runtimeMode: 'host',
      healthUrl: null,
      healthStatus: null,
      baseUrl: null,
      profiles: null,
      version: null,
      artifact: null,
      uptimeSeconds: null,
      startedAt: null,
      receivedCalls: null,
      requestedCalls: null,
      responseTimeMs: null,
      checkedAt: isoDate(0),
      containerName: null,
      containerState: null,
      containerStatusText: null,
      image: null,
      containerStartedAt: null,
      restartCount: null,
      containerError: 'Sidecar process is not running on the host.',
      error: 'Sidecar process is not running on the host.',
    },
  ],
};

const systemSettings = { waterTemperatureToWatch: true };

const mediaStorage = {
  enabled: true,
  storageDir: '/runtime/media',
  publicUrlPrefix: 'http://localhost:8091/media',
  maxFileSizeMb: 25,
  retentionDays: 30,
  directoryExists: true,
  writable: true,
  detail: null,
};

const mediaContent = {
  enabled: true,
  storageDir: '/runtime/media',
  publicUrlPrefix: 'http://localhost:8091/media',
  detail: null,
  items: [
    {
      id: 'media-1',
      shortCode: 'ab12cd',
      contentType: 'image/png',
      mediaType: 'image',
      originalFileName: 'water-level-trend.png',
      sizeBytes: 2_411_520,
      createdAt: isoDate(-120),
      expiresAt: isoDate(60 * 24 * 20),
      sourceProtocol: 'TELEGRAM',
      sourceNetwork: 'TelegramNetwork',
      sourceChannelAlias: 'TELEGRAM-HOME',
      sourceChannelName: 'Home group',
      sourceSender: 'Petri',
    },
    {
      id: 'media-2',
      shortCode: 'pdf99z',
      contentType: 'application/pdf',
      mediaType: 'document',
      originalFileName: 'siike-report.pdf',
      sizeBytes: 148_480,
      createdAt: isoDate(-600),
      expiresAt: null,
      sourceProtocol: 'WhatsApp',
      sourceNetwork: 'WhatsApp',
      sourceChannelAlias: 'WA-TEST',
      sourceChannelName: 'WhatsApp',
      sourceSender: 'Matti Meikäläinen',
    },
  ],
};

const collectedUrls = {
  enabled: true,
  storageDir: '/runtime/media',
  detail: null,
  items: [
    {
      id: 'url-1',
      shortCode: 'vt88aa',
      url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      provider: 'YouTube',
      title: 'Classic video with a very long descriptive title that keeps going',
      author: 'Some Channel',
      description: 'Captured from the public channel while the bot was watching.',
      duration: 'PT3H37M12S',
      publishedAt: isoDate(-5_000),
      viewCount: 1_204_500,
      createdAt: isoDate(-45),
      expiresAt: isoDate(60 * 24 * 30),
      sourceProtocol: 'IRC',
      sourceNetwork: 'IRCNet',
      sourceChannelAlias: 'IRC-TEST',
      sourceChannelName: '#bot-test',
      sourceSender: 'petria',
    },
    {
      id: 'url-2',
      shortCode: 'nk71bb',
      url: 'https://example.test/artikkeli/lyhyt',
      provider: null,
      title: 'Lyhyt artikkeli',
      author: null,
      description: null,
      duration: null,
      publishedAt: null,
      viewCount: null,
      createdAt: isoDate(-90),
      expiresAt: null,
      sourceProtocol: 'DISCORD',
      sourceNetwork: 'DiscordNetwork',
      sourceChannelAlias: 'DISCORD-TEST',
      sourceChannelName: '#test',
      sourceSender: 'discodialue',
    },
  ],
};

const liveMedia = {
  enabled: true,
  storageDir: '/runtime/media',
  publicUrlPrefix: 'http://localhost:8091/media',
  detail: null,
  items: [
    {
      type: 'media',
      id: 'media-1',
      shortCode: 'ab12cd',
      createdAt: isoDate(-15),
      expiresAt: isoDate(60 * 24 * 20),
      sourceProtocol: 'discord',
      sourceNetwork: 'Test',
      sourceChannelAlias: 'DISCORD-TEST',
      sourceChannelName: '#test',
      sourceSender: 'Petri',
      contentType: 'image/png',
      mediaType: 'image',
      originalFileName: 'water-level-trend.png',
      sizeBytes: 2_411_520,
      url: '/m/ab12cd',
      provider: null,
      title: null,
      author: null,
      description: null,
      duration: null,
      publishedAt: null,
      viewCount: null,
    },
    {
      type: 'url',
      id: 'url-1',
      shortCode: null,
      createdAt: isoDate(-30),
      expiresAt: isoDate(60 * 24 * 30),
      sourceProtocol: 'IRC',
      sourceNetwork: 'IRCNet',
      sourceChannelAlias: 'IRC-TEST',
      sourceChannelName: '#bot-test',
      sourceSender: 'petria',
      contentType: null,
      mediaType: null,
      originalFileName: null,
      sizeBytes: null,
      url: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
      provider: 'YouTube',
      title: 'Classic video with a very long descriptive title that keeps going',
      author: 'Some Channel',
      description: 'Captured from the public channel while the bot was watching.',
      duration: 'PT1H2M3S',
      publishedAt: isoDate(-9_000),
      viewCount: 999_000,
    },
  ],
};

const liveChannelAliasList = [
  'IRC-TEST',
  'IRC-BOT-CHAT',
  'DISCORD-TEST',
  'TELEGRAM-HOME',
  'WA-TEST',
  'IRC-JYVSKYLA',
  'TELEGRAM-BOT-DEV',
];

const liveChannels = {
  channels: liveChannelAliasList.map((alias) => ({
    echoToAlias: alias,
    label: alias === 'IRC-TEST' ? '#bot-test' : alias.toLowerCase(),
    connectionType: alias.startsWith('IRC') ? 'IRC_CONNECTION'
      : alias.startsWith('DISCORD') ? 'DISCORD_CONNECTION'
        : alias.startsWith('WA') ? 'WHATSAPP_CONNECTION' : 'TELEGRAM_CONNECTION',
    network: alias.startsWith('IRC') ? 'IRCNet' : alias.split('-')[0],
    channelType: 'CHANNEL',
    sendAllowed: true,
    adminAllowed: true,
    modeAllowed: true,
  })),
};

const liveChannelUsers = {
  channelUsers: [
    {
      account: 'petria', awayMessage: null, host: 'example.test', nick: 'petria',
      operatorInformation: 'is channel operator', realName: 'Petri Airio', server: 'irc.example.test',
      userString: 'petria!petria@example.test', displayPrefix: '@', channelModes: ['o'], channelRoles: ['op'],
      away: false,
    },
    {
      account: 'matti', awayMessage: null, host: 'example.test', nick: 'matti',
      operatorInformation: null, realName: 'Matti Meikäläinen', server: 'irc.example.test',
      userString: 'matti!matti@example.test', displayPrefix: null, channelModes: [], channelRoles: [],
      away: true,
    },
  ],
};

const liveChannelSettings = {
  publicAiEnabled: true,
  allowAnonymousAiCommands: false,
  resolveUrls: true,
  captureResolvedUrls: true,
  captureImages: true,
  showCapturedUrl: false,
};

const liveChannelTopic = {
  echoToAlias: 'IRC-TEST',
  channelName: '#bot-test',
  configuredTopic: 'Bot test channel - please be kind',
  currentTopic: 'Bot test channel - please be kind',
  manageTopic: true,
  connected: true,
  joined: true,
  mismatch: false,
  editable: true,
};

const liveChannelMode = {
  echoToAlias: 'IRC-TEST',
  channelName: '#bot-test',
  configuredModes: '+nt',
  currentModes: '+nt',
  manageMode: true,
  connected: true,
  joined: true,
  mismatch: false,
  editable: true,
};

const adminUsers = {
  users: [
    {
      id: 1, username: 'petria', name: 'Petri Airio', email: 'petri@example.test', ircNick: '_Pete_',
      telegramId: null, discordId: null, whatsappId: null,
      homeChannel: { connectionType: 'IRC_CONNECTION', network: 'IRCNet', echoToAlias: 'IRC-TEST', label: '#bot-test' },
      chatIdentities: [], permissions: ['web.user', 'web.admin', 'channels.view.all'], reserved: false,
    },
    {
      id: 2, username: 'matti', name: 'Matti Meikäläinen', email: null, ircNick: 'matti',
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      chatIdentities: [], permissions: ['web.user'], reserved: false,
    },
  ],
  availablePermissions: ['web.user', 'web.admin', 'channels.view.all', 'config.edit'],
  availableHomeChannels: [
    { connectionType: 'IRC_CONNECTION', network: 'IRCNet', echoToAlias: 'IRC-TEST', label: '#bot-test' },
    { connectionType: 'DISCORD_CONNECTION', network: 'DiscordNetwork', echoToAlias: 'DISCORD-TEST', label: '#test' },
  ],
};

const knownUserTargets = {
  targets: [
    {
      logicalUserKey: 'irc:petria', configuredUserId: 1, configuredUsername: 'petria', configuredName: 'Petri Airio',
      matchedConfiguredUser: true, matchSource: 'IRC_NICK', observedUserKey: 'petria', observedUserId: 'petria',
      observedUsername: 'petria', observedDisplayName: 'petria', connectionId: 1,
      connectionType: 'IRC_CONNECTION', network: 'IRCNet', channelId: '1', channelName: '#bot-test',
      echoToAlias: 'IRC-TEST', targetType: 'PUBLIC', lastSeenAt: Date.now(), lastSeenSource: 'IRC',
    },
    {
      logicalUserKey: 'discord:discodialue', configuredUserId: null, configuredUsername: null, configuredName: null,
      matchedConfiguredUser: false, matchSource: null, observedUserKey: 'discord-user',
      observedUserId: '123456789012345678', observedUsername: 'discodialue', observedDisplayName: 'Discora Lue',
      connectionId: 2, connectionType: 'DISCORD_CONNECTION', network: 'DiscordNetwork', channelId: '2',
      channelName: '#test', echoToAlias: 'DISCORD-TEST', targetType: 'PUBLIC', lastSeenAt: Date.now(),
      lastSeenSource: 'DISCORD',
    },
  ],
};

const commands = {
  providers: [
    {
      namespace: 'main',
      displayName: 'Bot commands',
      description: 'Commands registered by the bot itself.',
      commandCount: 3,
      invocationCount: 4_212,
      commands: [
        {
          commandName: 'water', displayName: 'water', trigger: '!water', className: 'fi.freakz.bot.WaterCommand',
          requiredPermission: null, help: 'Latest water level and temperature for a station',
          invocationCount: 1_204, aliases: [{ alias: 'vesi', target: 'water', withArgs: false }],
        },
        {
          commandName: 'help', displayName: 'help', trigger: '!help', className: 'fi.freakz.bot.HelpCommand',
          requiredPermission: null, help: 'Lists commands and shows usage for one command. This help text is long enough to wrap in a table cell.',
          invocationCount: 980, aliases: [],
        },
      ],
    },
    {
      namespace: 'hermes',
      displayName: 'AI commands',
      description: 'Hermes backed commands.',
      commandCount: 1,
      invocationCount: 61,
      commands: [
        {
          commandName: 'summary', displayName: 'summary', trigger: '!summary', className: null,
          requiredPermission: 'commands.admin', help: 'Summarise the last messages of the channel',
          invocationCount: 61, aliases: [{ alias: 'yhteenveto', target: 'summary', withArgs: true }],
        },
      ],
    },
  ],
};

const aiCommands = {
  path: '/runtime/config/ai-commands.json',
  availableTools: ['weather.current', 'water.level', 'channel.recent_messages'],
  config: {
    commands: [
      {
        name: 'saa',
        enabled: true,
        description: 'Sää Jyväskylässä',
        usage: '!saa [paikkakunta]',
        aliases: ['saa', 'sää', 'foreca'],
        requiredPermission: null,
        instructions: 'Vastaa lyhyesti suomeksi ja käytä Forecan dataa.',
        allowedTools: ['weather.current'],
        maxToolIterations: 3,
        toolResultMode: 'FORMATTED_TEXT',
      },
    ],
  },
};

const connectionsMap = {
  connectionMap: {
    'IRC_CONNECTION:IRCNet': {
      id: 1,
      type: 'IRC_CONNECTION',
      network: 'IRCNet',
      channels: [
        { id: '1', type: 'CHANNEL', network: 'IRCNet', name: '#bot-test', echoToAlias: 'IRC-TEST', configured: true },
        { id: '2', type: 'CHANNEL', network: 'IRCNet', name: '#bot-chat', echoToAlias: 'IRC-BOT-CHAT', configured: true },
      ],
    },
    'DISCORD_CONNECTION:DiscordNetwork': {
      id: 2,
      type: 'DISCORD_CONNECTION',
      network: 'DiscordNetwork',
      channels: [
        { id: '17', type: 'GUILD_CHANNEL', network: 'DiscordNetwork', name: '#test', echoToAlias: 'DISCORD-TEST', observedOnly: true },
      ],
    },
  },
};

const channelActivity = {
  channels: [
    {
      echoToAlias: 'IRC-TEST', type: 'CHANNEL', network: 'IRCNet', name: '#bot-test',
      lastReceivedMessageAt: Date.now() - 120_000, lastReceivedMessageBy: 'petria',
      lastReceivedMessageSource: 'petria!petria@example.test',
    },
    {
      echoToAlias: 'DISCORD-TEST', type: 'GUILD_CHANNEL', network: 'DiscordNetwork', name: '#test',
      lastReceivedMessageAt: null, lastReceivedMessageBy: null, lastReceivedMessageSource: null,
    },
  ],
};

const configChannel = (alias, name) => ({
  id: `${alias.toLowerCase()}-1`,
  description: null,
  name,
  type: 'CHANNEL',
  echoToAlias: alias,
  echoToAliases: [alias],
  echoIrcActivity: false,
  joinOnStart: true,
  commandsEnabled: true,
  publicAiEnabled: true,
  allowAnonymousAiCommands: false,
  resolveUrls: true,
  alertMessages: false,
  captureResolvedUrls: true,
  captureImages: true,
  showCapturedUrl: false,
  captureImageToAliases: [],
  manageOperators: true,
  manageTopic: true,
  topic: 'Bot test channel - please be kind',
  manageMode: true,
  modes: '+nt',
});

const connectionConfig = {
  profile: 'prod',
  configFile: '/runtime/config/bot-io.yaml',
  lastModifiedAt: isoDate(-300),
  config: {
    botConfig: { botName: 'the_bot', ircRealName: 'the_bot' },
    ircServerConfigs: [{
      name: 'IRCNet',
      connectStartup: true,
      networkName: 'IRCNet',
      host: 'irc.example.test',
      port: 6697,
      channelList: [configChannel('IRC-TEST', '#bot-test'), configChannel('IRC-BOT-CHAT', '#bot-chat')],
    }],
    discordConfig: {
      connectStartup: true,
      theBotUserId: '123456789012345678',
      channelList: [configChannel('DISCORD-TEST', '#test')],
    },
    telegramConfig: {
      telegramName: 'the_bot',
      connectStartup: false,
      channelList: [configChannel('TELEGRAM-HOME', 'Home group')],
    },
    whatsappConfig: {
      network: 'WhatsApp',
      sendBaseUrl: 'http://whatsapp-bridge:8080',
      connectStartup: true,
      channelList: [configChannel('WA-TEST', 'WhatsApp')],
    },
  },
  topicStates: [{
    echoToAlias: 'IRC-TEST', channelName: '#bot-test', manageTopic: true,
    configuredTopic: 'Bot test channel - please be kind', currentTopic: 'Bot test channel - please be kind',
    connected: true, joined: true, mismatch: false,
  }],
  modeStates: [{
    echoToAlias: 'IRC-TEST', channelName: '#bot-test', manageMode: true,
    configuredModes: '+nt', currentModes: '+nt', connected: true, joined: true, mismatch: false,
  }],
};

const whatsappAuth = {
  state: 'AUTHENTICATED',
  authenticated: true,
  syncRunning: false,
  authRunning: false,
  method: 'QR',
  qrUrl: null,
  linkExpiresAt: null,
  pairingCode: null,
  message: 'Linked device is active.',
  error: null,
};

const hermesBackends = {
  systemMode: 'enabled',
  backends: [
    {
      id: 'openai', label: 'OpenAI', provider: 'openai', baseUrl: null, model: 'gpt-6.1-sol',
      apiMode: 'responses', timeoutSeconds: 30, contextWindow: 100_000, healthy: true, toolCapable: true,
      detail: null, lastValidatedAt: null, validationStatus: 'OK', apiKeyConfigured: true, reasoningDisabled: false,
    },
    {
      id: 'local-0', label: 'Local 3090', provider: 'llamacpp', baseUrl: 'http://192.168.0.55:8080/v1',
      model: 'flash-next', apiMode: 'chat-completions', timeoutSeconds: 120, contextWindow: 65_536,
      healthy: true, toolCapable: true, detail: null, lastValidatedAt: null, validationStatus: 'OK',
      apiKeyConfigured: false, reasoningDisabled: true,
    },
    {
      id: 'local-1', label: 'Remote 5090', provider: 'llamacpp', baseUrl: 'http://192.168.0.60:8080/v1',
      model: 'flash-next', apiMode: 'chat-completions', timeoutSeconds: 120, contextWindow: 131_072,
      healthy: false, toolCapable: null, detail: 'No slots loaded', lastValidatedAt: null,
      validationStatus: 'NOT_VALIDATED', apiKeyConfigured: false, reasoningDisabled: true,
    },
  ],
  routes: [
    {
      id: 'chat', label: 'Chat', backendId: 'openai', provider: 'openai', baseUrl: null, model: 'gpt-6.1-sol',
      apiMode: 'responses', timeoutSeconds: 30, contextWindow: 100_000, healthy: true, toolCapable: true, detail: null,
    },
    {
      id: 'tools', label: 'Tool calling', backendId: 'local-0', provider: 'llamacpp', baseUrl: 'http://192.168.0.55:8080/v1',
      model: 'flash-next', apiMode: 'chat-completions', timeoutSeconds: 120, contextWindow: 65_536,
      healthy: true, toolCapable: true, detail: null,
    },
  ],
  profiles: [],
  fallback: null,
  globalOverride: null,
};

const generatedPage = {
  id: 'glugga-1',
  componentType: 'GluggaCountsPage',
  title: 'GLUGGA top nicks',
  createdAt: isoDate(-60),
  expiresAt: isoDate(60 * 24),
  props: {
    channel: '#bot-test',
    network: 'IRCNet',
    counterKey: 'GLUGGA_COUNT',
    counterName: 'GLUGGA',
    generatedAt: isoDate(-60),
    rowCount: 4,
    rows: [
      { rank: 1, nick: 'petria', value: 4_812 },
      { rank: 2, nick: 'matti', value: 3_910 },
      { rank: 3, nick: 'discodialue', value: 2_450 },
      { rank: 4, nick: 'botwatcher', value: 1_208 },
    ],
  },
};

const jsonFixtures = new Map([
  ['/api/web/me', me],
  ['/api/web/me/notify-rules', notifyRules],
  ['/api/web/system/status', systemStatus],
  ['/api/web/admin/system/settings', systemSettings],
  ['/api/web/admin/system/media-storage', mediaStorage],
  ['/api/web/admin/media-content', mediaContent],
  ['/api/web/admin/collected-urls', collectedUrls],
  ['/api/web/live-media', liveMedia],
  ['/api/web/live-channels/channels', liveChannels],
  ['/api/web/live-channels/users', liveChannelUsers],
  ['/api/web/live-channels/settings', liveChannelSettings],
  ['/api/web/live-channels/topic', liveChannelTopic],
  ['/api/web/live-channels/mode', liveChannelMode],
  ['/api/web/live-channels/events', { events: [] }],
  ['/api/web/admin/users', adminUsers],
  ['/api/web/admin/config/connections', connectionConfig],
  ['/api/web/admin/whatsapp/auth', whatsappAuth],
  ['/api/web/admin/system/hermes/backends', hermesBackends],
  ['/api/web/admin/ai-commands', aiCommands],
  ['/api/web/commands', commands],
  ['/api/web/connections/map', connectionsMap],
  ['/api/web/connections/activity', channelActivity],
  ['/api/web/console/events', { events: [] }],
  ['/api/web/known-users/targets', knownUserTargets],
]);

const streamEvents = [
  {
    match: '/api/web/system/status/stream',
    frames: [`event: status\ndata: ${JSON.stringify(systemStatus)}\n\n`],
  },
  {
    match: '/api/web/console/stream',
    frames: [
      `data: ${JSON.stringify({ id: 1, requestId: 0, createdAt: Date.now(), message: 'the-bot> Console session ready. Try !water.' })}\n\n`,
    ],
  },
  {
    match: '/api/web/live-channels/stream',
    frames: [
      `data: ${JSON.stringify({
        id: 1, requestId: 0, createdAt: Date.now() - 90_000, echoToAlias: 'IRC-TEST', sender: 'petria',
        senderId: 'petria', message: 'vesitiedot päivittyi', protocol: 'IRC', network: 'IRCNet',
        chatType: 'CHANNEL', chatId: '#bot-test', direction: 'INBOUND',
      })}\n\n`,
      `data: ${JSON.stringify({
        id: 2, requestId: 0, createdAt: Date.now() - 30_000, echoToAlias: 'IRC-TEST', sender: 'matti',
        senderId: 'matti', message: 'KIITOS! cat /etc/passwd on edelleen turvassa ja sää on hyvä',
        protocol: 'IRC', network: 'IRCNet', chatType: 'CHANNEL', chatId: '#bot-test', direction: 'INBOUND',
      })}\n\n`,
    ],
  },
];

function sendJson(response, status, body) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(payload),
    'Cache-Control': 'no-store',
  });
  response.end(payload);
}

function openStream(response, frames) {
  response.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no',
  });
  response.write(': open\n\n');
  frames.forEach((frame) => response.write(frame));

  const keepAlive = setInterval(() => response.write(': ping\n\n'), 15_000);
  const stop = () => clearInterval(keepAlive);
  response.on('close', stop);
  response.on('error', stop);
}

function generatedPageFixture(pathname) {
  if (!pathname.startsWith('/api/web/generated-pages/')) {
    return null;
  }
  return generatedPage;
}

function handle(request, response) {
  const url = new URL(request.url, `http://${request.headers.host ?? 'localhost'}`);
  const pathname = url.pathname;

  if (pathname === '/default-ui.css') {
    response.writeHead(200, { 'Content-Type': 'text/css', 'Content-Length': '0', 'Cache-Control': 'no-store' });
    response.end();
    return;
  }

  const stream = streamEvents.find((candidate) => pathname === candidate.match);
  if (stream) {
    openStream(response, stream.frames);
    return;
  }

  if (!pathname.startsWith('/api/web/')) {
    response.writeHead(404, { 'Content-Type': 'text/plain' });
    response.end('mock-web-api: unsupported path');
    return;
  }

  if (request.method !== 'GET' && request.method !== 'HEAD') {
    sendJson(response, 200, {});
    return;
  }

  const fixture = jsonFixtures.get(pathname) ?? generatedPageFixture(pathname);
  if (fixture) {
    sendJson(response, 200, fixture);
    return;
  }

  sendJson(response, 200, {});
}

const listening = [];
for (const host of hosts) {
  const server = createServer(handle);
  await new Promise((resolvePromise, rejectPromise) => {
    server.once('error', (error) => {
      console.log(`mock web api skipped ${host}:${port} (${error.code ?? error.message})`);
      resolvePromise();
    });
    server.listen(port, host, () => {
      listening.push(`http://${host.includes(':') ? `[${host}]` : host}:${port}`);
      server.removeAllListeners('error');
      resolvePromise();
    });
  });
}

if (listening.length === 0) {
  console.error('mock web api could not bind any loopback address');
  process.exit(1);
}

console.log(`mock web api on ${listening.join(', ')}`);
