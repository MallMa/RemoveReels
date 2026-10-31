// Checks every rule list in ../rules so a broken list fails CI instead of breaking the apps.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import { allRuleSets } from './helpers.mjs';

const { document } = new JSDOM('<!doctype html><html><body></body></html>').window;

const TYPES = {
  route: ['match', 'action'],
  hide: ['selector'],
  hideClosest: ['match', 'closest'],
  hideText: ['container', 'text']
};

function assertSelector(selector, where) {
  assert.doesNotThrow(() => document.querySelectorAll(selector), `${where}: invalid CSS selector ${selector}`);
}

function assertRegex(source, where) {
  assert.doesNotThrow(() => new RegExp(source), `${where}: invalid regular expression ${source}`);
}

for (const { file, ruleSet } of allRuleSets()) {
  test(`${file} is a valid rule list`, () => {
    assert.equal(ruleSet.schema, 0, `${file}: unknown schema`);
    assert.equal(file, `${ruleSet.platform}.json`, `${file}: file name must match the platform id`);
    for (const key of ['name', 'version', 'startUrl', 'homePath']) assert.ok(ruleSet[key], `${file}: missing ${key}`);
    assert.ok(ruleSet.startUrl.startsWith('https://'), `${file}: startUrl must be https`);
    assert.ok(ruleSet.origins.length > 0 && ruleSet.origins.every((o) => /^https:\/\/[^/]+$/.test(o)),
      `${file}: origins must be https origins without a path`);
    assert.ok(ruleSet.origins.includes(new URL(ruleSet.startUrl).origin), `${file}: startUrl origin not in origins`);
    assert.ok(Array.isArray(ruleSet.hosts) && ruleSet.hosts.length > 0, `${file}: missing hosts`);

    const ids = new Set();
    for (const rule of ruleSet.rules) {
      const where = `${file}#${rule.id}`;
      assert.match(rule.id, /^[a-z0-9-]+$/, `${where}: id must be lowercase letters, digits and dashes`);
      assert.ok(!ids.has(rule.id), `${where}: duplicate id`);
      ids.add(rule.id);
      assert.ok(rule.name, `${where}: missing name`);
      assert.ok(TYPES[rule.type], `${where}: unknown type ${rule.type}`);
      for (const key of TYPES[rule.type]) assert.ok(rule[key], `${where}: missing ${key}`);
      if (rule.routes) assertRegex(rule.routes, where);
      if (rule.canaryWhen) {
        assert.ok(rule.canary, `${where}: canaryWhen without canary`);
        assertSelector(rule.canaryWhen, where);
      }

      if (rule.type === 'route') {
        assertRegex(rule.match, where);
        if (rule.from) assertRegex(rule.from, where);
        assert.ok(['block', 'redirect'].includes(rule.action), `${where}: action must be block or redirect`);
        assert.ok(['any', 'load', 'spa', undefined].includes(rule.on), `${where}: on must be any, load or spa`);
        if (rule.action === 'redirect') assert.ok(rule.to?.startsWith('/'), `${where}: redirect needs a path in "to"`);
      } else if (rule.type === 'hide') {
        assertSelector(rule.selector, where);
      } else if (rule.type === 'hideClosest') {
        assertSelector(rule.match, where);
        assertSelector(rule.closest, where);
      } else if (rule.type === 'hideText') {
        assertSelector(rule.container, where);
        assert.ok(Array.isArray(rule.text) && rule.text.length > 0 && rule.text.every((t) => typeof t === 'string' && t),
          `${where}: text must be a list of non-empty strings`);
      }
    }
  });
}
