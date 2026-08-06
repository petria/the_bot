import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as consoleApi from '../api/console';
import * as meApi from '../api/me';
import { installEventSourceMock, renderPage } from '../test/pageTestUtils';
import { ConsolePage } from './ConsolePage';

describe('ConsolePage', () => {
  it('submits a command and keeps the console input available', async () => {
    installEventSourceMock();
    vi.spyOn(consoleApi, 'getConsoleEventStreamUrl').mockReturnValue('/console-stream');
    const execute = vi.spyOn(consoleApi, 'executeConsoleCommand').mockResolvedValue({ requestId: 1, accepted: true });
    vi.spyOn(meApi, 'getMe').mockResolvedValue({
      id: 1, username: 'petria', name: 'Petri', email: null, ircNick: null,
      telegramId: null, discordId: null, whatsappId: null, homeChannel: null,
      permissions: ['web.user'], roles: [],
    });
    const user = userEvent.setup();
    renderPage(<ConsolePage />);

    const input = screen.getByPlaceholderText('!ping');
    await user.type(input, '!ping');
    await user.click(screen.getByRole('button', { name: 'Execute' }));

    expect(execute).toHaveBeenCalledWith(expect.any(String), '!ping');
    expect(input).toHaveValue('');
    expect((screen.getByLabelText('Console output') as HTMLTextAreaElement).value).toContain('you> !ping');
  });
});
