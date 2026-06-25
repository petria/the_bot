import {
  Alert,
  Button,
  Card,
  Group,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useMutation } from '@tanstack/react-query';
import { AlertTriangle, CornerDownLeft, Terminal, Trash2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { ApiError } from '../api/client';
import { executeConsoleCommand } from '../api/console';

type ConsoleLine = {
  id: number;
  kind: 'input' | 'reply' | 'error' | 'system';
  text: string;
};

export function ConsolePage() {
  const [command, setCommand] = useState('');
  const [lines, setLines] = useState<ConsoleLine[]>([
    {
      id: 1,
      kind: 'system',
      text: 'Console ready. Commands execute as your logged-in bot user.',
    },
  ]);
  const outputRef = useRef<HTMLTextAreaElement | null>(null);
  const nextIdRef = useRef(2);
  const trimmedCommand = command.trim();

  const commandMutation = useMutation({
    mutationFn: executeConsoleCommand,
    onSuccess: (response) => {
      appendLine('reply', response.reply?.trim() || 'Command accepted; no immediate reply.');
    },
    onError: (error) => {
      const apiError = error instanceof ApiError ? error : null;
      appendLine('error', apiError?.detail || apiError?.message || error.message);
    },
  });

  useEffect(() => {
    const output = outputRef.current;
    if (output) {
      output.scrollTop = output.scrollHeight;
    }
  }, [lines]);

  const canSubmit = trimmedCommand.length > 0 && !commandMutation.isPending;

  const submit = () => {
    if (!canSubmit) {
      return;
    }
    appendLine('input', trimmedCommand);
    commandMutation.mutate(trimmedCommand);
    setCommand('');
  };

  const clear = () => {
    setLines([
      {
        id: nextId(),
        kind: 'system',
        text: 'Console cleared.',
      },
    ]);
    commandMutation.reset();
  };

  const transcript = lines.map(formatLine).join('\n');

  function appendLine(kind: ConsoleLine['kind'], text: string) {
    setLines((current) => [
      ...current,
      {
        id: nextId(),
        kind,
        text,
      },
    ]);
  }

  function nextId() {
    const value = nextIdRef.current;
    nextIdRef.current += 1;
    return value;
  }

  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-start" gap="sm">
        <div>
          <Title order={2}>Console</Title>
          <Text c="dimmed">Execute bot commands as your logged-in user.</Text>
        </div>
        <Button
          variant="light"
          color="gray"
          leftSection={<Trash2 size={18} />}
          onClick={clear}
        >
          Clear
        </Button>
      </Group>

      <Card withBorder radius="sm" className="console-card">
        <Stack gap="md">
          <Textarea
            ref={outputRef}
            aria-label="Console output"
            className="console-output"
            value={transcript}
            readOnly
            autosize={false}
            minRows={18}
          />

          <Group gap="sm" align="flex-end" wrap="nowrap" className="console-input-row">
            <TextInput
              className="console-command-input"
              label="Command"
              placeholder="!ping"
              value={command}
              leftSection={<Terminal size={18} />}
              onChange={(event) => {
                setCommand(event.currentTarget.value);
                commandMutation.reset();
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault();
                  submit();
                }
              }}
              disabled={commandMutation.isPending}
            />
            <Button
              leftSection={<CornerDownLeft size={18} />}
              loading={commandMutation.isPending}
              disabled={!canSubmit}
              onClick={submit}
            >
              Execute
            </Button>
          </Group>
        </Stack>
      </Card>

      {commandMutation.isError ? (
        <Alert color="red" variant="light" icon={<AlertTriangle size={18} />} title="Command failed">
          <Text>{commandMutation.error instanceof ApiError ? commandMutation.error.message : commandMutation.error.message}</Text>
        </Alert>
      ) : null}
    </Stack>
  );
}

function formatLine(line: ConsoleLine) {
  switch (line.kind) {
    case 'input':
      return `you> ${line.text}`;
    case 'reply':
      return `bot> ${line.text}`;
    case 'error':
      return `error> ${line.text}`;
    case 'system':
    default:
      return `system> ${line.text}`;
  }
}
