import { test } from 'node:test';
import assert from 'node:assert/strict';
import engine from '../engine.js';
import { loadRuleSet } from './helpers.mjs';

const youtube = engine.compileRules(loadRuleSet('youtube'));
const instagram = engine.compileRules(loadRuleSet('instagram'));

function verdict(compiled, path, prev = null) {
  const v = engine.evaluateRoute(compiled, path, prev);
  return v.action === 'allow' ? 'allow' : `${v.action}:${v.rule.id}${v.to ? ' ' + v.to : ''}`;
}

test('rules use their default unless the user overrode them', () => {
  const ids = (c) => [...c.routes, ...c.hides, ...c.closest, ...c.texts].map((r) => r.id);
  assert.ok(!ids(instagram).includes('ig-feed-reels'));
  const withFeedReels = engine.compileRules(loadRuleSet('instagram'), { 'ig-feed-reels': true, 'ig-reels-tab': false });
  assert.ok(ids(withFeedReels).includes('ig-feed-reels'));
  assert.ok(!ids(withFeedReels).includes('ig-reels-tab'));
});

test('YouTube Shorts open as normal videos', () => {
  assert.equal(verdict(youtube, '/shorts/abc_D-1'), 'redirect:yt-shorts-redirect /watch?v=abc_D-1');
  assert.equal(verdict(youtube, '/shorts/abc?feature=share', '/results?search_query=x'),
    'redirect:yt-shorts-redirect /watch?v=abc');
  assert.equal(verdict(youtube, '/watch?v=abc'), 'allow');
  assert.equal(verdict(youtube, '/results?search_query=shorts'), 'allow');
});

test('Instagram Reels feed is blocked, single reels are allowed', () => {
  assert.equal(verdict(instagram, '/reels/'), 'block:ig-reels-tab');
  assert.equal(verdict(instagram, '/reels/?next=1'), 'block:ig-reels-tab');
  assert.equal(verdict(instagram, '/reels/ABC123/'), 'redirect:ig-reels-link /reel/ABC123/');
  assert.equal(verdict(instagram, '/reel/ABC123/'), 'allow');
  assert.equal(verdict(instagram, '/reel/ABC123/', '/direct/t/42/'), 'allow');
});

test('Instagram: swiping from one reel to the next is blocked', () => {
  assert.equal(verdict(instagram, '/reels/B/', '/reel/A/'), 'block:ig-reel-swipe');
  assert.equal(verdict(instagram, '/reel/B/', '/reel/A/'), 'block:ig-reel-swipe');
  // Same reel with a different query string is not a swipe.
  assert.equal(verdict(instagram, '/reel/A/?utm=1', '/reel/A/'), 'allow');
  // Leaving the reel is fine.
  assert.equal(verdict(instagram, '/direct/t/42/', '/reel/A/'), 'allow');
});

test('a redirect that would not change the URL is ignored', () => {
  const compiled = engine.compileRules({
    rules: [{ id: 'loop', type: 'route', match: '^/a$', action: 'redirect', to: '/a' }]
  });
  assert.equal(verdict(compiled, '/a'), 'allow');
});

test('route-scoped hide rules only apply on their routes', () => {
  assert.match(engine.buildCss(youtube, '/watch?v=1'), /ytm-single-column-watch-next-results-renderer/);
  assert.doesNotMatch(engine.buildCss(youtube, '/results?search_query=x'), /watch-next/);
  assert.match(engine.buildCss(youtube, '/'), /ytm-rich-grid-renderer/);
  assert.match(engine.buildCss(youtube, '/?app=m'), /ytm-rich-grid-renderer/);
  assert.doesNotMatch(engine.buildCss(youtube, '/feed/subscriptions'), /ytm-rich-grid-renderer/);
});
