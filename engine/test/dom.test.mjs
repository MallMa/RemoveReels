// Runs the engine against small fake pages that mirror the real sites' structure.
import { test, mock } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM, VirtualConsole } from 'jsdom';
import engine from '../engine.js';
import { loadRuleSet } from './helpers.mjs';

const YOUTUBE_SEARCH = `<!doctype html><html><head></head><body><ytm-app>
  <ytm-pivot-bar-renderer>
    <ytm-pivot-bar-item-renderer><div class="pivot-bar-item-tab pivot-w2w">Home</div></ytm-pivot-bar-item-renderer>
    <ytm-pivot-bar-item-renderer><div class="pivot-bar-item-tab pivot-shorts">Shorts</div></ytm-pivot-bar-item-renderer>
  </ytm-pivot-bar-renderer>
  <ytm-search><lazy-list>
    <ytm-video-with-context-renderer id="video"><a href="/watch?v=normal1">A video</a></ytm-video-with-context-renderer>
    <ytm-video-with-context-renderer id="short"><a href="/shorts/short1">A short</a></ytm-video-with-context-renderer>
  </lazy-list></ytm-search>
</ytm-app></body></html>`;

const INSTAGRAM_FEED = `<!doctype html><html><head></head><body>
  <nav><a href="/">Home</a><a href="/reels/">Reels</a><a href="/direct/inbox/">Messages</a></nav>
  <main>
    <article id="friend"><header>friend_account</header><p>Holiday photos</p></article>
    <article id="suggested"><header>Suggested for you</header><p>Some stranger</p></article>
  </main>
</body></html>`;

function open(html, url, platform, enabled) {
  const virtualConsole = new VirtualConsole(); // swallow jsdom's "not implemented" notices (window.stop)
  const dom = new JSDOM(html, { url, pretendToBeVisual: true, virtualConsole });
  const navigations = [];
  const messages = [];
  const handle = engine.start(
    { platform, ruleSet: loadRuleSet(platform), enabled },
    { window: dom.window, navigate: (u) => navigations.push(u), post: (m) => messages.push(m) }
  );
  return { dom, window: dom.window, document: dom.window.document, handle, navigations, messages };
}

test('YouTube: Shorts cards and the Shorts tab are hidden, normal videos stay', (t) => {
  const page = open(YOUTUBE_SEARCH, 'https://m.youtube.com/results?search_query=cooking', 'youtube');
  t.after(() => page.window.close());
  page.handle.scanNow();

  assert.equal(page.document.getElementById('short').getAttribute('data-rr-hidden'), 'yt-shorts-cards');
  assert.equal(page.document.getElementById('video').hasAttribute('data-rr-hidden'), false);
  const css = page.document.getElementById('rr-style').textContent;
  assert.match(css, /ytm-pivot-bar-item-renderer:has\(\.pivot-shorts\)\{display:none!important\}/);
  const shortsTab = page.document.querySelector('.pivot-shorts').parentElement;
  assert.equal(page.window.getComputedStyle(shortsTab).display, 'none');
});

test('YouTube: tapping a Short opens it as a normal video', (t) => {
  const page = open(YOUTUBE_SEARCH, 'https://m.youtube.com/results?search_query=cooking', 'youtube');
  t.after(() => page.window.close());
  const link = page.document.querySelector('a[href^="/shorts/"]');
  const event = new page.window.MouseEvent('click', { bubbles: true, cancelable: true });
  link.dispatchEvent(event);
  assert.equal(event.defaultPrevented, true);
  assert.deepEqual(page.navigations, ['/watch?v=short1']);
});

test('YouTube: in-app navigation to a Short is redirected too', (t) => {
  const page = open(YOUTUBE_SEARCH, 'https://m.youtube.com/results?search_query=cooking', 'youtube');
  t.after(() => page.window.close());
  page.window.history.pushState({}, '', '/shorts/xyz');
  assert.deepEqual(page.navigations, ['/watch?v=xyz']);
  assert.ok(page.messages.some((m) => m.type === 'route' && m.id === 'yt-shorts-redirect'));
});

test('Instagram: opening the Reels feed directly shows the block screen', (t) => {
  const page = open(INSTAGRAM_FEED, 'https://www.instagram.com/reels/', 'instagram');
  t.after(() => page.window.close());
  assert.equal(page.handle.blocked.id, 'ig-reels-tab');
  const block = page.document.getElementById('rr-block');
  assert.ok(block, 'block screen is shown');
  assert.match(block.shadowRoot.textContent, /The Reels feed is blocked/);
});

test('the block screen works on pages that enforce Trusted Types (no HTML string sinks)', (t) => {
  const virtualConsole = new VirtualConsole();
  const dom = new JSDOM(INSTAGRAM_FEED, { url: 'https://www.instagram.com/reels/', virtualConsole });
  // Simulate require-trusted-types-for 'script': string assignments to HTML sinks throw.
  const sinks = [
    [dom.window.Element.prototype, 'innerHTML'],
    [dom.window.ShadowRoot.prototype, 'innerHTML'],
    [dom.window.Element.prototype, 'insertAdjacentHTML'],
  ].map(([proto, name]) => [proto, name, Object.getOwnPropertyDescriptor(proto, name)]);
  const reject = () => { throw new TypeError('TrustedHTML required'); };
  for (const [proto, name] of sinks) {
    Object.defineProperty(proto, name, name === 'innerHTML' ? { set: reject, configurable: true } : { value: reject, configurable: true });
  }
  t.after(() => {
    // jsdom's own window.close() uses innerHTML.
    for (const [proto, name, descriptor] of sinks) {
      if (descriptor) Object.defineProperty(proto, name, descriptor); else delete proto[name];
    }
    dom.window.close();
  });
  const handle = engine.start(
    { platform: 'instagram', ruleSet: loadRuleSet('instagram') },
    { window: dom.window, navigate: () => {}, post: () => {} }
  );
  assert.equal(handle.blocked.id, 'ig-reels-tab');
  assert.match(dom.window.document.getElementById('rr-block').shadowRoot.textContent, /The Reels feed is blocked/);
});

test('Instagram: a sent reel plays, swiping to the next one is blocked, going back unblocks', (t) => {
  const page = open(INSTAGRAM_FEED, 'https://www.instagram.com/direct/t/42/', 'instagram');
  t.after(() => page.window.close());

  page.window.history.pushState({}, '', '/reel/A/');
  assert.equal(page.handle.blocked, null);

  page.window.history.pushState({}, '', '/reels/B/');
  assert.equal(page.handle.blocked.id, 'ig-reel-swipe');
  assert.ok(page.document.getElementById('rr-block'));

  page.window.history.pushState({}, '', '/direct/t/42/');
  assert.equal(page.handle.blocked, null);
  assert.equal(page.document.getElementById('rr-block'), null);
});

test('Instagram: suggested posts and the Reels button are hidden on the feed', (t) => {
  const page = open(INSTAGRAM_FEED, 'https://www.instagram.com/', 'instagram');
  t.after(() => page.window.close());
  page.handle.scanNow();
  assert.equal(page.document.getElementById('suggested').getAttribute('data-rr-hidden'), 'ig-suggested-posts');
  assert.equal(page.document.getElementById('friend').hasAttribute('data-rr-hidden'), false);
  const reelsLink = page.document.querySelector('a[href="/reels/"]');
  assert.equal(page.window.getComputedStyle(reelsLink).display, 'none');
});

test('page views report per-rule counts and canary checks to the host', (t) => {
  mock.timers.enable({ apis: ['Date'], now: 1_000_000 });
  t.after(() => mock.timers.reset());
  const page = open(YOUTUBE_SEARCH, 'https://m.youtube.com/results?search_query=cooking', 'youtube');
  t.after(() => page.window.close());
  page.handle.scanNow();

  mock.timers.tick(engine.MIN_PAGE_VIEW_MS + 1);
  page.window.history.pushState({}, '', '/results?search_query=baking');

  const pageViews = page.messages.filter((m) => m.type === 'pageview');
  assert.equal(pageViews.length, 1);
  const byId = Object.fromEntries(pageViews[0].rules.map((r) => [r.id, r]));
  assert.deepEqual(byId['yt-shorts-tab'], { id: 'yt-shorts-tab', count: 1, canary: true });
  assert.deepEqual(byId['yt-shorts-cards'], { id: 'yt-shorts-cards', count: 1, canary: false });
  assert.equal(byId['yt-watch-next'], undefined, 'rules for other routes are not reported');
});

test('very short page views are not counted', (t) => {
  const page = open(YOUTUBE_SEARCH, 'https://m.youtube.com/results?search_query=cooking', 'youtube');
  t.after(() => page.window.close());
  page.window.history.pushState({}, '', '/results?search_query=baking');
  assert.equal(page.messages.filter((m) => m.type === 'pageview').length, 0);
});
