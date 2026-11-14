/**
 * Dev-only phone-width screenshot pass for every route in App.tsx.
 *
 * Run `npm run mock-api` and `npm run dev` first, then `npm run mobile-shots`.
 * Every route is rendered at 390x844, screenshotted, and then re-shot with each
 * tab selected and each modal open. The run fails when a route scrolls sideways.
 *
 * Browsers come from ~/.cache/ms-playwright; on a Playwright version mismatch run
 * `npx playwright install chromium`.
 */
import { mkdir } from 'node:fs/promises';
import { resolve } from 'node:path';
import process from 'node:process';
import { chromium } from 'playwright';

const baseUrl = (process.env.MOBILE_SHOTS_BASE_URL ?? 'http://localhost:5173').replace(/\/$/, '');
const width = Number(process.env.MOBILE_SHOTS_WIDTH ?? 390);
const height = Number(process.env.MOBILE_SHOTS_HEIGHT ?? 844);
const deviceScaleFactor = Number(process.env.MOBILE_SHOTS_SCALE ?? 3);
const outputDir = resolve(process.cwd(), process.env.MOBILE_SHOTS_DIR ?? 'mobile-shots');

const liveChannelTabs = [
  'IRC-TEST',
  'IRC-BOT-CHAT',
  'DISCORD-TEST',
  'TELEGRAM-HOME',
  'WA-TEST',
  'IRC-JYVSKYLA',
  'TELEGRAM-BOT-DEV',
];

const routes = [
  { path: '/system', name: '01-system', heading: 'System' },
  { path: '/overview', name: '02-overview', heading: 'My Account' },
  { path: '/users', name: '03-known-users', heading: 'Known Users', modals: [
    { name: 'create-observed-user', trigger: { role: 'button', text: 'Create user' } },
    { name: 'link-observed-identity', trigger: { role: 'button', text: 'Link' } },
  ] },
  { path: '/commands', name: '04-commands', heading: 'Commands' },
  { path: '/console', name: '05-console', heading: 'Console' },
  { path: '/live-channels', name: '06-live-channels', heading: 'Live Channels', seedLiveChannels: true, tabs: true },
  { path: '/live-media', name: '07-live-media', heading: 'Live Media', contentSelector: '.data-cards' },
  { path: '/send', name: '08-send', heading: 'Send Message', tabs: true },
  { path: '/connections', name: '09-connections', heading: 'Connections' },
  { path: '/profile', name: '10-profile', heading: 'Profile' },
  { path: '/admin/manage-system', name: '11-admin-manage-system', heading: 'Manage System' },
  { path: '/admin/users', name: '12-admin-users', heading: 'Manage Users', modals: [
    { name: 'add-user', trigger: { role: 'button', text: 'Add user' } },
    { name: 'edit-user', trigger: { role: 'button', name: 'Edit user' } },
    { name: 'reset-password', trigger: { role: 'button', name: 'Reset password' } },
    { name: 'link-observed-identity', trigger: { role: 'button', name: 'Link observed identity' } },
    { name: 'delete-user', trigger: { role: 'button', name: 'Delete user' } },
  ] },
  { path: '/admin/config', name: '13-admin-config', heading: 'Manage Connections', tabs: true },
  { path: '/admin/media-storage', name: '14-admin-media-storage', heading: 'Manage Media Storage' },
  { path: '/admin/media-content', name: '15-admin-media-content', heading: 'Media Content', contentSelector: '.data-cards' },
  { path: '/admin/collected-urls', name: '16-admin-collected-urls', heading: 'Collected URLs', contentSelector: '.data-cards' },
  { path: '/admin/ai-commands', name: '17-admin-ai-commands', heading: 'Manage AI Commands' },
  { path: '/admin/system', name: '18-admin-system', heading: 'Manage AI Routes' },
  { path: '/generated/glugga-1?token=demo-token', name: '19-generated', heading: 'the_bot', contentText: 'GLUGGA top nicks' },
  { path: '/definitely-not-a-page', name: '20-not-found', heading: 'Page not found' },
];

const results = [];
const consoleErrors = new Map();

async function newPage(browser, { seedChannels = false } = {}) {
  const context = await browser.newContext({
    viewport: { width, height },
    deviceScaleFactor,
    isMobile: true,
    hasTouch: true,
    reducedMotion: 'reduce',
  });

  if (seedChannels) {
    await context.addInitScript((aliases) => {
      window.sessionStorage.setItem('the-bot-live-channels-open', JSON.stringify(aliases.map((alias) => ({
        echoToAlias: alias,
        label: alias.toLowerCase(),
        sendAllowed: true,
        adminAllowed: true,
        modeAllowed: true,
        connectionType: 'IRC_CONNECTION',
      }))));
      window.sessionStorage.setItem('the-bot-live-channels-active', aliases[0]);
    }, liveChannelTabs);
  }

  const page = await context.newPage();
  page.on('console', (message) => {
    if (message.type() === 'error') {
      const seen = consoleErrors.get(context) ?? new Set();
      seen.add(message.text());
      consoleErrors.set(context, seen);
    }
  });
  page.on('pageerror', (error) => {
    const seen = consoleErrors.get(context) ?? new Set();
    seen.add(String(error));
    consoleErrors.set(context, seen);
  });

  return { context, page };
}

async function openRoute(page, route) {
  await page.goto(`${baseUrl}${route.path}`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle');

  const heading = route.heading ?? null;
  if (heading) {
    await page.getByRole('heading', { name: heading, exact: true }).first().waitFor({ timeout: 15_000 });
  } else {
    await page.locator('h2').first().waitFor({ timeout: 15_000 });
  }

  if (route.contentSelector) {
    await page.locator(route.contentSelector).first().waitFor({ timeout: 15_000 });
  }

  if (route.contentText) {
    await page.getByText(route.contentText).first().waitFor({ timeout: 15_000 });
  }

  // Let react-query settle so screenshots show loaded content, not spinners.
  await page.waitForTimeout(500);
}

const controlSelector = 'button, a[role="button"], input, textarea, select, [role="tab"]';

async function measure(page) {
  return page.evaluate(({ selector }) => {
    // Composite-control parts that stay shorter than 44px on purpose: the two
    // stepper halves share the field's 44px height, and the clear-all and pill
    // remove affordances live inside a field that is itself the 44px target —
    // enlarging them collides with the field's own chevron section. Each one has
    // an equivalent path: type over the value, drop it from the dropdown, or use
    // the pill itself.
    const documentedClasses = [
      'mantine-NumberInput-control',
      'mantine-Pill-remove',
      'mantine-InputClearButton-root',
    ];

    const describe = (element) => {
      const classes = String(element.className ?? '').split(/\s+/).filter(Boolean);
      // State classes and CSS-module hashes say nothing about which control this is.
      const stateClasses = new Set(['mantine-focus-auto', 'mantine-active']);
      const label = element.getAttribute('aria-label')
          || classes.find((name) => name.startsWith('mantine-') && !stateClasses.has(name))
          || classes.find((name) => !name.startsWith('m_') && !stateClasses.has(name))
          || element.tagName.toLowerCase();
      return `${element.tagName.toLowerCase()}.${label}`;
    };

    // Only real, visible touch targets count. The tab strip is deliberately
    // compact, and Mantine hides the native inputs behind Switch and
    // SegmentedControl. checkVisibility() is used instead of offsetParent so
    // controls inside fixed-position modal content still get measured.
    const isTouchTarget = (element) => {
      if (element.closest('.mantine-Tabs-tab')) {
        return false;
      }
      if (element.tagName === 'INPUT' && ['checkbox', 'radio', 'hidden', 'range'].includes(element.type)) {
        return false;
      }
      if (!element.checkVisibility({ opacityProperty: true, visibilityProperty: true })) {
        return false;
      }
      const box = element.getBoundingClientRect();
      return box.width > 0 && box.height > 0;
    };

    const counts = new Map();
    const documented = new Map();
    const format = (map) => Array.from(map, ([key, count]) => (count > 1 ? `${key} x${count}` : key)).sort();

    for (const element of document.querySelectorAll(selector)) {
      if (!isTouchTarget(element)) {
        continue;
      }
      const box = element.getBoundingClientRect();
      if (box.height >= 44) {
        continue;
      }
      const key = `${describe(element)} ${Math.round(box.height)}px`;
      const bucket = documentedClasses.some((name) => element.classList.contains(name)) ? documented : counts;
      bucket.set(key, (bucket.get(key) ?? 0) + 1);
    }

    return {
      scrollWidth: Math.max(document.documentElement.scrollWidth, document.body.scrollWidth),
      smallControls: format(counts),
      documentedControls: format(documented),
    };
  }, { selector: controlSelector });
}

async function capture(context, page, label) {
  const file = resolve(outputDir, `${label}.png`);
  await page.screenshot({ path: file, fullPage: true });
  const { scrollWidth, smallControls, documentedControls } = await measure(page);
  results.push({ label, file, scrollWidth, overflow: scrollWidth > width, smallControls, documentedControls });
  console.log(`${scrollWidth > width ? 'OVERFLOW' : 'ok'} ${label} scrollWidth=${scrollWidth}`
      + (smallControls.length ? ` small=[${smallControls.join(', ')}]` : '')
      + (documentedControls.length ? ` documented=[${documentedControls.join(', ')}]` : ''));
}

async function clickFirst(page, trigger) {
  const locator = trigger.name
      ? page.getByRole(trigger.role, { name: trigger.name })
      : page.getByRole(trigger.role, { name: trigger.text, exact: true });
  await locator.first().scrollIntoViewIfNeeded();
  await locator.first().click();
  await page.getByRole('dialog').first().waitFor({ timeout: 10_000 });
  await page.waitForTimeout(400);
}

async function captureTabs(context, page, route) {
  const tabs = page.getByRole('tab');
  const count = await tabs.count();
  for (let index = 0; index < count; index += 1) {
    const tab = tabs.nth(index);
    const label = (await tab.textContent())?.trim() || `tab-${index + 1}`;
    await tab.scrollIntoViewIfNeeded();
    await tab.click();
    await page.waitForTimeout(400);
    await capture(context, page, `${route.name}--tab-${index + 1}-${slug(label)}`);
  }
}

async function captureModals(context, page, route) {
  for (const modal of route.modals ?? []) {
    await clickFirst(page, modal.trigger);
    await capture(context, page, `${route.name}--modal-${modal.name}`);
    await page.keyboard.press('Escape');
    await page.getByRole('dialog').first().waitFor({ state: 'detached', timeout: 10_000 }).catch(() => undefined);
    await page.waitForTimeout(300);
  }
}

async function captureOpenNavigation(context, page, route) {
  const burger = page.locator('.mantine-Burger-root').first();
  if (await burger.count() === 0) {
    return;
  }
  await burger.click();
  await page.waitForTimeout(400);
  await capture(context, page, `${route.name}--nav-open`);
}

function slug(value) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 28);
}

async function run() {
  await mkdir(outputDir, { recursive: true });
  const browser = await chromium.launch();

  for (const route of routes) {
    const { context, page } = await newPage(browser, { seedChannels: route.seedLiveChannels === true });
    try {
      await openRoute(page, route);
      await capture(context, page, `${route.name}--page`);
      if (route.path === '/system') {
        await captureOpenNavigation(context, page, route);
      }
      if (route.tabs) {
        await captureTabs(context, page, route);
      }
      if (route.modals) {
        await captureModals(context, page, route);
      }
    } catch (error) {
      results.push({
        label: `${route.name} (route)`,
        file: '',
        scrollWidth: 0,
        overflow: true,
        smallControls: [],
        documentedControls: [],
        error: String(error),
      });
      console.log(`FAILED ${route.name}: ${error instanceof Error ? error.message : error}`);
    } finally {
      const errors = consoleErrors.get(context);
      if (errors?.size) {
        console.log(`console errors on ${route.name}: ${[...errors].slice(0, 3).join(' | ')}`);
      }
      await context.close();
    }
  }

  await browser.close();

  const overflowing = results.filter((result) => result.overflow);
  const small = results.filter((result) => result.smallControls.length > 0);
  const documented = new Set(results.flatMap((result) => result.documentedControls ?? []));
  console.log(`\n${results.length} states captured into ${outputDir}`);
  console.log(`${overflowing.length} states scroll sideways past ${width}px`);
  if (small.length > 0) {
    console.log(`${small.length} states report controls shorter than 44px (inspect the PNGs):`);
    for (const result of small.slice(0, 10)) {
      console.log(`  ${result.label}: ${result.smallControls.join(', ')}`);
    }
  }
  if (documented.size > 0) {
    console.log(`documented sub-44px composite parts: ${[...documented].sort().join(', ')}`);
  }

  if (overflowing.length > 0) {
    for (const result of overflowing) {
      console.log(`  ${result.label}: scrollWidth=${result.scrollWidth}${result.error ? ` error=${result.error}` : ''}`);
    }
    process.exitCode = 1;
  }
}

await run();
