// RemoveReels rule engine.
//
// One file, no dependencies, runs inside the platform's own web page (Android WebView, iOS WKWebView, browser
// extension). The host wraps it as `(function(){ <this file>; RemoveReelsEngine.start(config, env); })()` and injects
// it at document start. In Node it exports the pure functions for tests.
//
// config = { platform, ruleSet: <rules/*.json>, enabled: { ruleId: bool } }
// env    = optional overrides for tests: { window, navigate(url), post(message) }
var RemoveReelsEngine = (function () {
  'use strict';

  var MIN_PAGE_VIEW_MS = 3000; // shorter page views (redirects, bounces) are not counted in the stats
  var SCAN_DELAY_MS = 250;
  var HIDDEN_ATTR = 'data-rr-hidden';

  // ---- Pure functions (shared with tests) -------------------------------------------------------------------------

  function isEnabled(rule, enabled) {
    if (enabled && Object.prototype.hasOwnProperty.call(enabled, rule.id)) return !!enabled[rule.id];
    return rule.default !== false;
  }

  // Splits the enabled rules by type and pre-compiles their regular expressions.
  function compileRules(ruleSet, enabled) {
    var out = { routes: [], hides: [], closest: [], texts: [] };
    (ruleSet.rules || []).forEach(function (rule) {
      if (!isEnabled(rule, enabled)) return;
      var r = Object.assign({}, rule);
      if (r.routes) r.routesRe = new RegExp(r.routes);
      if (r.type === 'route') {
        r.matchRe = new RegExp(r.match);
        if (r.from) r.fromRe = new RegExp(r.from);
        out.routes.push(r);
      } else if (r.type === 'hide') out.hides.push(r);
      else if (r.type === 'hideClosest') out.closest.push(r);
      else if (r.type === 'hideText') out.texts.push(r);
    });
    return out;
  }

  function appliesTo(rule, path) {
    return !rule.routesRe || rule.routesRe.test(path);
  }

  // Decides what to do with a URL. `path` is pathname + search. `prevPath` is the previous URL inside the same page,
  // or null on a full document load.
  function evaluateRoute(compiled, path, prevPath) {
    for (var i = 0; i < compiled.routes.length; i++) {
      var rule = compiled.routes[i];
      var on = rule.on || 'any';
      if (on === 'spa' && prevPath == null) continue;
      if (on === 'load' && prevPath != null) continue;
      var m = rule.matchRe.exec(path);
      if (!m) continue;
      if (rule.fromRe) {
        if (prevPath == null) continue;
        var pm = rule.fromRe.exec(prevPath);
        // Moving within the same item (e.g. a query string change on the same reel) is not a transition.
        if (!pm || pm[0] === m[0]) continue;
      }
      if (rule.action === 'redirect') {
        // The target is the template with $1..$9 filled in; anything after the match ("?feature=share") is dropped.
        var to = rule.to.replace(/\$(\d)/g, function (_, d) { return m[+d] || ''; });
        if (to === path) continue;
        return { action: 'redirect', rule: rule, to: to };
      }
      return { action: 'block', rule: rule };
    }
    return { action: 'allow' };
  }

  function buildCss(compiled, path) {
    var css = '[' + HIDDEN_ATTR + ']{display:none!important}';
    compiled.hides.forEach(function (rule) {
      if (appliesTo(rule, path)) css += '\n' + rule.selector + '{display:none!important}';
    });
    return css;
  }

  function containsAny(text, needles) {
    for (var i = 0; i < needles.length; i++) if (text.indexOf(needles[i]) !== -1) return true;
    return false;
  }

  // ---- Runtime (needs a DOM) --------------------------------------------------------------------------------------

  function start(config, env) {
    env = env || {};
    var win = env.window || window;
    var doc = win.document;
    if (win.top !== win || win.__removeReelsStarted) return null;
    win.__removeReelsStarted = true;

    var ruleSet = config.ruleSet || {};
    var compiled = compileRules(ruleSet, config.enabled);
    var homePath = ruleSet.homePath || '/';
    var navigate = env.navigate || function (url) { win.location.replace(url); };
    var post = env.post || function (msg) {
      if (win.rrNative && win.rrNative.postMessage) win.rrNative.postMessage(JSON.stringify(msg));
    };

    var currentPath = pathOf(win.location);
    var pageView = newPageView();
    var blocked = null;
    var styleEl = null;
    var scanTimer = null;

    function pathOf(loc) { return loc.pathname + loc.search; }

    function newPageView() { return { startedAt: Date.now(), counts: {}, canaryReady: {} }; }

    function count(rule, n) {
      var c = pageView.counts;
      c[rule.id] = Math.max(c[rule.id] || 0, n);
    }

    // Sends the finished page view's per-rule numbers to the host. Only rules active on that page are included, so
    // the host can tell "matched nothing" from "did not apply".
    function flushPageView(path) {
      var pv = pageView;
      pageView = newPageView();
      if (Date.now() - pv.startedAt < MIN_PAGE_VIEW_MS || blocked) return;
      var rules = [];
      compiled.hides.concat(compiled.closest, compiled.texts).forEach(function (rule) {
        if (!appliesTo(rule, path)) return;
        var canary = !!rule.canary && (!rule.canaryWhen || !!pv.canaryReady[rule.id]);
        rules.push({ id: rule.id, count: pv.counts[rule.id] || 0, canary: canary });
      });
      if (rules.length) post({ type: 'pageview', platform: config.platform, rules: rules });
    }

    function applyRoute(prevPath) {
      var verdict = evaluateRoute(compiled, currentPath, prevPath);
      if (verdict.action === 'redirect') {
        post({ type: 'route', platform: config.platform, id: verdict.rule.id });
        navigate(verdict.to);
        return;
      }
      if (verdict.action === 'block') {
        if (!blocked || blocked.id !== verdict.rule.id) {
          post({ type: 'route', platform: config.platform, id: verdict.rule.id });
        }
        showBlock(verdict.rule, prevPath == null);
      } else {
        hideBlock();
      }
      updateStyle();
      scheduleScan();
    }

    function onUrlMaybeChanged() {
      var next = pathOf(win.location);
      if (next === currentPath) return;
      var prev = currentPath;
      flushPageView(prev);
      currentPath = next;
      applyRoute(prev);
    }

    function updateStyle() {
      if (!styleEl) {
        styleEl = doc.createElement('style');
        styleEl.id = 'rr-style';
      }
      if (!styleEl.isConnected) (doc.head || doc.documentElement).appendChild(styleEl);
      styleEl.textContent = buildCss(compiled, currentPath);
    }

    // ---- Hiding and counting ----

    function scheduleScan() {
      if (scanTimer) return;
      scanTimer = win.setTimeout(function () { scanTimer = null; scan(); }, SCAN_DELAY_MS);
    }

    function safeQueryAll(selector) {
      try { return doc.querySelectorAll(selector); } catch (e) { return []; }
    }

    function scan() {
      if (!doc.documentElement) return;
      if (styleEl && !styleEl.isConnected) updateStyle(); // some sites rebuild <head>
      var path = currentPath;
      compiled.hides.forEach(function (rule) {
        if (!appliesTo(rule, path)) return;
        count(rule, safeQueryAll(rule.selector).length);
        if (rule.canaryWhen && safeQueryAll(rule.canaryWhen).length) pageView.canaryReady[rule.id] = true;
      });
      compiled.closest.forEach(function (rule) {
        if (!appliesTo(rule, path)) return;
        Array.prototype.forEach.call(safeQueryAll(rule.match), function (el) {
          var box = el.closest(rule.closest) || el;
          if (!box.hasAttribute(HIDDEN_ATTR)) box.setAttribute(HIDDEN_ATTR, rule.id);
        });
        count(rule, safeQueryAll('[' + HIDDEN_ATTR + '="' + rule.id + '"]').length);
      });
      compiled.texts.forEach(function (rule) {
        if (!appliesTo(rule, path)) return;
        Array.prototype.forEach.call(safeQueryAll(rule.container), function (box) {
          if (box.hasAttribute(HIDDEN_ATTR)) return;
          if (containsAny(box.textContent || '', rule.text)) box.setAttribute(HIDDEN_ATTR, rule.id);
        });
        count(rule, safeQueryAll('[' + HIDDEN_ATTR + '="' + rule.id + '"]').length);
      });
    }

    // ---- Block screen ----

    function pauseMedia() {
      Array.prototype.forEach.call(doc.querySelectorAll('video, audio'), function (m) {
        try { m.pause(); } catch (e) { /* ignore */ }
      });
    }

    function showBlock(rule, isDocumentLoad) {
      blocked = rule;
      // On a full page load, stop the site from loading at all; the parser may not have created <html> yet.
      if (isDocumentLoad && typeof win.stop === 'function') win.stop();
      if (!doc.documentElement) doc.appendChild(doc.createElement('html'));
      var host = doc.getElementById('rr-block');
      if (!host) host = buildBlockScreen();
      host.__rrMessage.textContent = rule.message || 'This part of the app is blocked.';
      if (!host.isConnected) {
        if (!doc.body) doc.documentElement.appendChild(doc.createElement('body'));
        doc.body.appendChild(host);
      }
      pauseMedia();
    }

    // Built with createElement only: YouTube enforces Trusted Types, so assigning HTML strings (innerHTML) throws.
    function buildBlockScreen() {
      var el = function (tag, css, text) {
        var node = doc.createElement(tag);
        if (css) node.style.cssText = css;
        if (text) node.textContent = text;
        return node;
      };
      var host = el('div', 'position:fixed;inset:0;z-index:2147483647;');
      host.id = 'rr-block';
      var root = host.attachShadow ? host.attachShadow({ mode: 'open' }) : host;
      var button = 'font:16px system-ui,sans-serif;padding:12px 20px;border-radius:999px;border:0;';
      var wrap = el('div', 'position:fixed;inset:0;display:flex;flex-direction:column;align-items:center;' +
        'justify-content:center;gap:16px;padding:24px;background:#101418;color:#e8eaed;' +
        'font:16px/1.5 system-ui,sans-serif;text-align:center');
      var title = el('h1', 'font-size:22px;margin:0', 'Blocked by RemoveReels');
      var message = el('p', 'margin:0;max-width:320px;color:#b8bec6');
      var back = el('button', button + 'background:#3b82f6;color:#fff', 'Go back');
      var home = el('button', button + 'background:transparent;color:#93c5fd', 'Home');
      back.addEventListener('click', function () {
        if (win.history.length > 1) win.history.back(); else navigate(homePath);
      });
      home.addEventListener('click', function () { navigate(homePath); });
      [title, message, back, home].forEach(function (n) { wrap.appendChild(n); });
      root.appendChild(wrap);
      host.__rrMessage = message;
      return host;
    }

    function toast(text) {
      var el = doc.createElement('div');
      el.textContent = text;
      el.style.cssText = 'position:fixed;left:50%;bottom:96px;transform:translateX(-50%);z-index:2147483647;' +
        'padding:10px 16px;border-radius:999px;background:#202833;color:#e8eaed;font:14px system-ui,sans-serif';
      (doc.body || doc.documentElement).appendChild(el);
      win.setTimeout(function () { el.remove(); }, 2500);
    }

    function hideBlock() {
      blocked = null;
      var host = doc.getElementById('rr-block');
      if (host) host.remove();
    }

    // ---- Wiring ----

    // Single-page apps change the URL with pushState/replaceState; watch both, plus back/forward and a slow poll
    // as a fallback for anything that slips past.
    ['pushState', 'replaceState'].forEach(function (name) {
      var original = win.history[name];
      win.history[name] = function () {
        var result = original.apply(this, arguments);
        onUrlMaybeChanged();
        return result;
      };
    });
    win.addEventListener('popstate', onUrlMaybeChanged);
    win.setInterval(onUrlMaybeChanged, 1000);

    // Stop clicks on blocked links before the site's router starts loading the content.
    doc.addEventListener('click', function (event) {
      var a = event.target && event.target.closest && event.target.closest('a[href]');
      if (!a) return;
      var url;
      try { url = new win.URL(a.getAttribute('href'), win.location.href); } catch (e) { return; }
      if (url.origin !== win.location.origin) return;
      var verdict = evaluateRoute(compiled, url.pathname + url.search, currentPath);
      if (verdict.action === 'allow') return;
      event.preventDefault();
      event.stopImmediatePropagation();
      post({ type: 'route', platform: config.platform, id: verdict.rule.id });
      if (verdict.action === 'redirect') navigate(verdict.to);
      else toast(verdict.rule.message || 'Blocked by RemoveReels');
    }, true);

    // Keep media paused while the block screen is up.
    doc.addEventListener('play', function (event) {
      if (blocked && event.target && event.target.pause) event.target.pause();
    }, true);

    win.addEventListener('pagehide', function () { flushPageView(currentPath); });
    doc.addEventListener('visibilitychange', function () {
      if (doc.visibilityState === 'hidden') flushPageView(currentPath);
    });

    var observer = new win.MutationObserver(scheduleScan);
    var observe = function () { observer.observe(doc.documentElement, { childList: true, subtree: true }); };
    if (doc.documentElement) observe();
    else doc.addEventListener('DOMContentLoaded', observe, { once: true });

    applyRoute(null);

    return {
      scanNow: scan,
      flush: function () { flushPageView(currentPath); },
      get blocked() { return blocked; },
      get path() { return currentPath; }
    };
  }

  return {
    compileRules: compileRules,
    evaluateRoute: evaluateRoute,
    buildCss: buildCss,
    start: start,
    MIN_PAGE_VIEW_MS: MIN_PAGE_VIEW_MS
  };
})();

if (typeof module === 'object' && module.exports) module.exports = RemoveReelsEngine;
