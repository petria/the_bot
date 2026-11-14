import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const cssPath = resolve(dirname(fileURLToPath(import.meta.url)), 'app.css');
const css = readFileSync(cssPath, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');
const mobileStart = '@media (max-width: 48em) {';

type Rule = {
  selectors: string[];
  declarations: string;
};

function parseRules(block: string): Rule[] {
  return Array.from(block.matchAll(/([^{}]+)\{([^{}]*)\}/g))
      .map((match) => ({
        selectors: match[1].split(',').map((selector) => selector.trim()).filter(Boolean),
        declarations: match[2],
      }))
      .filter((rule) => !rule.selectors.some((selector) => selector.startsWith('@')));
}

function mobileBlock(): string {
  const start = css.indexOf(mobileStart);
  expect(start, 'app.css must keep the shared 48em mobile block').toBeGreaterThan(-1);

  let depth = 0;
  for (let index = start + mobileStart.length - 1; index < css.length; index += 1) {
    if (css[index] === '{') {
      depth += 1;
    }
    if (css[index] === '}') {
      depth -= 1;
      if (depth === 0) {
        return css.slice(start, index + 1);
      }
    }
  }

  throw new Error('Unbalanced braces in the 48em mobile block.');
}

const mobile = mobileBlock();
const desktop = css.replace(mobile, '');

/** Selectors whose rule body declares the given CSS declaration. */
function selectorsFor(rules: Rule[], declaration: string): string[] {
  return rules.filter((rule) => rule.declarations.includes(declaration)).flatMap((rule) => rule.selectors);
}

const mobileRules = parseRules(mobile);
const desktopRules = parseRules(desktop);

describe('app.css mobile utilities', () => {
  it('stacks field and info rows and keeps the Manage AI Routes aliases', () => {
    expect(selectorsFor(mobileRules, 'flex-direction: column')).toEqual(expect.arrayContaining([
      '.field-row',
      '.system-field-row',
      '.info-row',
      '.system-info-row',
      '.profile-grid',
    ]));

    expect(selectorsFor(mobileRules, 'align-items: stretch')).toEqual(expect.arrayContaining([
      '.field-row',
      '.system-field-row',
      '.info-row',
      '.system-info-row',
    ]));

    expect(selectorsFor(mobileRules, 'max-width: 100% !important')).toEqual(expect.arrayContaining([
      '.field-row > *',
      '.system-field-row > *',
      '.profile-grid > *',
    ]));

    // Text's `ta` prop is an inline style, so a plain text-align would lose.
    expect(selectorsFor(mobileRules, 'text-align: left !important')).toEqual(expect.arrayContaining([
      '.info-row > .info-value',
      '.info-row > .system-info-value',
      '.system-info-row > .system-info-value',
    ]));
  });

  it('keeps fixed children and full-width actions available under both class names', () => {
    expect(selectorsFor(mobileRules, 'flex-grow: 0')).toEqual(expect.arrayContaining([
      '.field-row > .stack-fixed',
      '.field-row > .system-fixed',
      '.system-field-row > .stack-fixed',
      '.system-field-row > .system-fixed',
    ]));

    expect(selectorsFor(mobileRules, 'justify-content: stretch')).toEqual(expect.arrayContaining([
      '.action-row',
      '.system-actions',
    ]));

    const fullWidth = selectorsFor(mobileRules, 'width: 100%');
    for (const selector of [
      '.field-row > *',
      '.system-field-row > *',
      '.field-row .mantine-Button-root',
      '.system-field-row .mantine-Button-root',
      '.action-row .mantine-Button-root',
      '.system-actions .mantine-Button-root',
    ]) {
      expect(fullWidth).toContain(selector);
    }
  });

  it('gives phone touch targets and readable text without touching desktop', () => {
    const touchTargets = selectorsFor(mobileRules, 'min-height: 44px');
    for (const selector of [
      '.mantine-Input-input',
      '.mantine-NumberInput-input',
      '.mantine-Button-root',
      '.mantine-ActionIcon-root',
      'textarea',
      '.app-header .mantine-Burger-root',
      '.app-header .home-button',
      '.app-header .mantine-UnstyledButton-root',
      '.mantine-Modal-close',
      '.mantine-Drawer-close',
    ]) {
      expect(touchTargets).toContain(selector);
    }

    const readable = selectorsFor(mobileRules, 'font-size: 1rem');
    expect(readable).toEqual(expect.arrayContaining([
      '.mantine-Input-input',
      '.mantine-NumberInput-input',
      'textarea',
    ]));

    // Mantine names the stepper parts `-controls` (the pair) and `-control`
    // (each half); an unknown class here means the steppers went back to 24px.
    const wide = selectorsFor(mobileRules, 'min-width: 44px');
    for (const selector of [
      '.mantine-NumberInput-controls',
      '.mantine-NumberInput-control',
      '.mantine-Modal-close',
    ]) {
      expect(wide).toContain(selector);
    }
    expect(selectorsFor(mobileRules, 'min-height: 22px')).toContain('.mantine-NumberInput-control');

    expect(desktop).not.toMatch(/min-height:\s*44px/);
    expect(desktop).not.toMatch(/min-width:\s*44px/);
    expect(desktop).not.toMatch(/\.mantine-Tabs-list/);
  });

  it('scrolls tab bars and swaps wide tables for cards', () => {
    expect(selectorsFor(mobileRules, 'flex-wrap: nowrap')).toContain('.mantine-Tabs-list');
    expect(selectorsFor(mobileRules, 'overflow-x: auto')).toContain('.mantine-Tabs-list');
    expect(selectorsFor(mobileRules, 'min-width: 0')).toContain('.mantine-Tabs-root');
    expect(selectorsFor(mobileRules, 'min-height: 0')).toContain('.mantine-Tabs-tab .mantine-Button-root');
    expect(selectorsFor(mobileRules, 'display: none')).toContain('.data-table');
    expect(selectorsFor(mobileRules, 'display: flex')).toContain('.data-cards');
  });

  it('hides cards above the breakpoint and clamps modals on every viewport', () => {
    expect(selectorsFor(desktopRules, 'display: none')).toContain('.data-cards');
    expect(selectorsFor(desktopRules, 'max-width: calc(100vw - 16px)')).toContain('.mantine-Modal-content');
  });
});
