import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import * as aiApi from '../api/adminAiCommands';
import * as commandsApi from '../api/commands';
import { renderPage } from '../test/pageTestUtils';
import { AdminAiCommandsPage } from './AdminAiCommandsPage';

describe('AdminAiCommandsPage', () => {
  it('loads a dynamic command and saves edits', async () => {
    const config = {
      commands: [{
        name: 'weather', enabled: true, description: 'Weather lookup', usage: '!weather city',
        aliases: ['saa'], requiredPermission: null, instructions: 'Use weather.current.',
        allowedTools: ['weather.current'], maxToolIterations: 3, toolResultMode: 'FORMATTED_TEXT',
      }],
    };
    vi.spyOn(aiApi, 'getAiCommands').mockResolvedValue({ path: 'ai-commands.json', config, availableTools: ['weather.current'] });
    const save = vi.spyOn(aiApi, 'saveAiCommands').mockResolvedValue({
      path: 'ai-commands.json', config, availableTools: ['weather.current'],
    });
    vi.spyOn(commandsApi, 'getCommands').mockResolvedValue({ providers: [] });
    const user = userEvent.setup();
    renderPage(<AdminAiCommandsPage />);

    expect(await screen.findByText('!weather')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /save and apply/i }));
    expect(save.mock.calls[0]?.[0]).toEqual(expect.objectContaining({ commands: expect.any(Array) }));
  });
});
