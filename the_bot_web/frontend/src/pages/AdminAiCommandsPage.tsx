import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Card,
  Group,
  Loader,
  MultiSelect,
  NumberInput,
  Select,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Plus, Save, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError } from '../api/client';
import {
  type AiCommandConfig,
  type AiCommandDefinition,
  getAiCommands,
  saveAiCommands,
} from '../api/adminAiCommands';
import { getCommands } from '../api/commands';

const ADMIN_ONLY_PERMISSION = 'commands.admin';
const TOOL_RESULT_MODES = [
  { value: 'FORMATTED_TEXT', label: 'Formatted text' },
  { value: 'MODEL_FINAL', label: 'Model final' },
];

export function AdminAiCommandsPage() {
  const queryClient = useQueryClient();
  const aiCommandsQuery = useQuery({
    queryKey: ['admin-ai-commands'],
    queryFn: getAiCommands,
  });
  const commandsQuery = useQuery({
    queryKey: ['commands'],
    queryFn: getCommands,
  });
  const [config, setConfig] = useState<AiCommandConfig>({ commands: [] });

  useEffect(() => {
    if (aiCommandsQuery.data?.config) {
      setConfig({
        commands: (aiCommandsQuery.data.config.commands || []).map((command) => ({
          ...command,
          aliases: command.aliases || [],
          allowedTools: command.allowedTools || [],
          toolResultMode: command.toolResultMode || 'FORMATTED_TEXT',
        })),
      });
    }
  }, [aiCommandsQuery.data?.config]);

  const saveMutation = useMutation({
    mutationFn: saveAiCommands,
    onSuccess: (response) => {
      queryClient.setQueryData(['admin-ai-commands'], response);
      queryClient.invalidateQueries({ queryKey: ['commands'] });
    },
  });

  const staticMainCommands = useMemo(() => {
    const main = commandsQuery.data?.providers?.find((provider) => provider.namespace === 'main');
    return new Set((main?.commands || []).map((command) => (command.commandName || '').toLowerCase()));
  }, [commandsQuery.data]);

  const updateCommand = (index: number, patch: Partial<AiCommandDefinition>) => {
    setConfig((current) => ({
      commands: current.commands.map((command, i) => (i === index ? { ...command, ...patch } : command)),
    }));
  };

  const addCommand = () => {
    setConfig((current) => ({
      commands: [
        ...current.commands,
        {
          name: '',
          enabled: true,
          description: '',
          usage: '',
          aliases: [],
          requiredPermission: null,
          instructions: '',
          allowedTools: [],
          maxToolIterations: 3,
          toolResultMode: 'FORMATTED_TEXT',
        },
      ],
    }));
  };

  const removeCommand = (index: number) => {
    setConfig((current) => ({
      commands: current.commands.filter((_, i) => i !== index),
    }));
  };

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Manage AI Commands</Title>
          <Text c="dimmed">Runtime Hermes-backed commands stored in ai-commands.json.</Text>
          {aiCommandsQuery.data?.path ? <Text size="sm" c="dimmed">{aiCommandsQuery.data.path}</Text> : null}
        </div>
        <Group gap="sm">
          <Button variant="light" leftSection={<Plus size={18} />} onClick={addCommand}>
            Add command
          </Button>
          <Button
            leftSection={<Save size={18} />}
            loading={saveMutation.isPending}
            onClick={() => saveMutation.mutate(config)}
          >
            Save and apply
          </Button>
        </Group>
      </Group>

      {aiCommandsQuery.isLoading ? <Loader /> : null}
      {aiCommandsQuery.isError ? <PageError error={aiCommandsQuery.error} /> : null}
      {saveMutation.isError ? <PageError error={saveMutation.error} /> : null}

      <Alert color="yellow" variant="light" icon={<AlertCircle size={18} />}>
        Dynamic AI commands override un-namespaced Java commands. The Java command remains callable with its provider namespace.
      </Alert>

      {config.commands.map((command, index) => {
        const normalizedName = command.name.trim().toLowerCase();
        const overridesStaticCommand = normalizedName !== '' && staticMainCommands.has(normalizedName);
        return (
          <Card key={`ai-command-${index}`} withBorder radius="sm">
            <Stack gap="md">
              <Group justify="space-between" align="flex-start" gap="sm">
                <div>
                  <Group gap="xs">
                    <Title order={3}>!{command.name || 'new-command'}</Title>
                    {command.enabled ? <Badge color="green">enabled</Badge> : <Badge color="gray">disabled</Badge>}
                    {isAdminOnly(command) ? <Badge color="red">admin only</Badge> : null}
                    {overridesStaticCommand ? <Badge color="yellow">overrides main::{normalizedName}</Badge> : null}
                  </Group>
                  <Text size="sm" c="dimmed">{command.description || 'No description'}</Text>
                </div>
                <ActionIcon color="red" variant="subtle" aria-label="Delete AI command" onClick={() => removeCommand(index)}>
                  <Trash2 size={18} />
                </ActionIcon>
              </Group>

              <Group grow align="flex-start">
                <TextInput
                  label="Command name"
                  value={command.name}
                  onChange={(event) => updateCommand(index, { name: event.currentTarget.value })}
                />
                <NumberInput
                  label="Max tool iterations"
                  min={1}
                  max={10}
                  value={command.maxToolIterations || 3}
                  onChange={(value) => updateCommand(index, { maxToolIterations: Number(value) || 3 })}
                />
                <Select
                  label="Tool result output"
                  data={TOOL_RESULT_MODES}
                  value={command.toolResultMode || 'FORMATTED_TEXT'}
                  onChange={(value) => updateCommand(index, { toolResultMode: value || 'FORMATTED_TEXT' })}
                  allowDeselect={false}
                />
              </Group>

              <Switch
                label="Enabled"
                checked={command.enabled}
                onChange={(event) => updateCommand(index, { enabled: event.currentTarget.checked })}
              />

              <Switch
                label="Admin only"
                checked={isAdminOnly(command)}
                onChange={(event) => updateCommand(index, {
                  requiredPermission: event.currentTarget.checked ? ADMIN_ONLY_PERMISSION : null,
                })}
              />

              <TextInput
                label="Description"
                value={command.description || ''}
                onChange={(event) => updateCommand(index, { description: event.currentTarget.value })}
              />

              <TextInput
                label="Usage"
                description="Shown by !command ? and !help command"
                value={command.usage || ''}
                onChange={(event) => updateCommand(index, { usage: event.currentTarget.value })}
              />

              <TextInput
                label="Aliases"
                description="Comma separated aliases, for example saa, sää, foreca"
                value={(command.aliases || []).join(', ')}
                onChange={(event) => updateCommand(index, { aliases: splitList(event.currentTarget.value) })}
              />

              <MultiSelect
                label="Allowed tools"
                data={aiCommandsQuery.data?.availableTools || []}
                value={command.allowedTools || []}
                onChange={(allowedTools) => updateCommand(index, { allowedTools })}
                searchable
                clearable
              />

              <Textarea
                label="Instructions"
                minRows={6}
                autosize
                value={command.instructions || ''}
                onChange={(event) => updateCommand(index, { instructions: event.currentTarget.value })}
              />
            </Stack>
          </Card>
        );
      })}
    </Stack>
  );
}

function splitList(value: string): string[] {
  return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
}

function isAdminOnly(command: AiCommandDefinition): boolean {
  return (command.requiredPermission || '').trim().toLowerCase() === ADMIN_ONLY_PERMISSION;
}

function PageError({ error }: { error: Error }) {
  const apiError = error instanceof ApiError ? error : null;
  return (
    <Alert color="red" icon={<AlertCircle size={18} />}>
      <Text fw={700}>{apiError?.message || error.message}</Text>
      {apiError?.detail ? <Text size="sm" mt={4}>{apiError.detail}</Text> : null}
    </Alert>
  );
}
