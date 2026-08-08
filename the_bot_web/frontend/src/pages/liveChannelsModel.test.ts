import type { LiveChannelEvent, LiveChannelUser } from '../api/liveChannels';
import { describe, expect, it } from 'vitest';
import {
  compareChannelUsers,
  formatEvent,
  formatTime,
  hasSavedOpenChannels,
  isChannelOperator,
  readActiveAlias,
  readOpenChannels,
  userDetail,
  userDisplayName,
} from './liveChannelsModel';

function user(overrides: Partial<LiveChannelUser>): LiveChannelUser {
  return {
    account: null,
    awayMessage: null,
    host: null,
    nick: null,
    operatorInformation: null,
    realName: null,
    server: null,
    userString: null,
    displayPrefix: null,
    channelModes: null,
    channelRoles: null,
    away: false,
    ...overrides,
  };
}

describe('live channel view model', () => {
  it('sorts operators before other users and sorts each group by nick', () => {
    const users = [
      user({ nick: 'zeta' }),
      user({ nick: 'beta', displayPrefix: '@' }),
      user({ nick: 'Alpha', channelRoles: ['operator'] }),
      user({ nick: 'gamma' }),
    ];

    expect([...users].sort(compareChannelUsers).map((item) => userDisplayName(item)))
      .toEqual(['Alpha', '@beta', 'gamma', 'zeta']);
  });

  it('recognizes operator prefixes, modes, and roles', () => {
    expect(isChannelOperator(user({ displayPrefix: '@' }))).toBe(true);
    expect(isChannelOperator(user({ channelModes: ['o'] }))).toBe(true);
    expect(isChannelOperator(user({ channelRoles: ['OPERATOR'] }))).toBe(true);
    expect(isChannelOperator(user({ nick: 'visitor' }))).toBe(false);
  });

  it('formats user details and event directions for chat output', () => {
    const operator = user({
      nick: 'Pete',
      realName: 'Petri Airio',
      channelRoles: ['operator'],
      away: true,
    });
    expect(userDisplayName(operator)).toBe('Pete');
    expect(userDetail(operator)).toBe('Petri Airio / operator / away');

    const inbound: LiveChannelEvent = {
      id: 1,
      requestId: 1,
      createdAt: new Date(2026, 7, 6, 12, 34, 56).getTime(),
      echoToAlias: 'IRC-TEST',
      sender: 'Pete',
      senderId: 'pete',
      message: 'hello',
      protocol: 'irc',
      network: 'IRCNet',
      chatType: 'channel',
      chatId: '#test',
      direction: 'INBOUND',
    };
    expect(formatEvent(inbound)).toBe('12:34:56 Pete: hello');
    expect(formatEvent({ ...inbound, direction: 'WEB_OUTBOUND', message: 'sent' }))
      .toBe('12:34:56 sent');
    expect(formatTime(Number.NaN)).toBe('--:--:--');
  });

  it('reads valid session state and ignores malformed channel entries', () => {
    const storage = window.sessionStorage;
    storage.setItem('the-bot-live-channels-open', JSON.stringify([
      { echoToAlias: 'IRC-TEST', label: '#test', sendAllowed: true },
      { echoToAlias: 42, label: '#invalid' },
    ]));
    storage.setItem('the-bot-live-channels-active', 'IRC-TEST');

    expect(readOpenChannels(storage)).toEqual([{
      echoToAlias: 'IRC-TEST',
      label: '#test',
      sendAllowed: true,
      adminAllowed: false,
      modeAllowed: false,
    }]);
    expect(hasSavedOpenChannels(storage)).toBe(true);
    expect(readActiveAlias(storage)).toBe('IRC-TEST');
  });
});
