"""Summarises one cover probe CSV: events, searches, how far covers were from their target, and query times.

Also splits the error into frames just after a search and the rest, and expresses it in frames of movement.
"""
import csv, statistics, sys
rows = list(csv.DictReader(open(sys.argv[1])))
def pct(v, p): v = sorted(v); return v[min(len(v) - 1, int(p / 100 * len(v)))] if v else None
kinds = {}
for r in rows: kinds[r["kind"]] = kinds.get(r["kind"], 0) + 1
print("  rows by kind:", kinds)
scrolled = [int(r["uptime_ms"]) for r in rows if r["kind"] == "scrolled"]
gaps = [b - a for a, b in zip(scrolled, scrolled[1:]) if b - a < 1000]
if gaps: print("  scroll event gap ms: median %d p10 %d p90 %d" % (statistics.median(gaps), pct(gaps, 10), pct(gaps, 90)))
frames = [r for r in rows if r["kind"] == "frame" and r["true_top"] and r["shown_top"]]
moving, prev = [], None
for r in frames:
    t = int(r["true_top"])
    if prev is not None and t != prev: moving.append(abs(t - int(r["shown_top"])))
    prev = t
if moving: print("  cover error px over %d moving frames: median %d p95 %d max %d" % (len(moving), statistics.median(moving), pct(moving, 95), max(moving)))
for kind in ("frame", "find"):
    q = [int(r["query_us"]) for r in rows if r["kind"] == kind]
    if q: print("  %s query us: median %d p95 %d max %d (n=%d)" % (kind, statistics.median(q), pct(q, 95), max(q), len(q)))
clean, after_find, speeds, lags = [], [], [], []
prev_t, prev_ms, last_find = None, None, -1e9
for r in rows:
    ms = int(r["uptime_ms"])
    if r["kind"] == "find": last_find = ms; prev_t = None; continue
    if r["kind"] != "frame" or not r["true_top"] or not r["shown_top"]: continue
    t, shown = int(r["true_top"]), int(r["shown_top"])
    if prev_t is not None and t != prev_t:
        err, step = abs(t - shown), abs(t - prev_t)
        (after_find if ms - last_find < 40 else clean).append(err)
        speeds.append(step * 1000 / max(1, ms - prev_ms))
        if step: lags.append(err / step)
    prev_t, prev_ms = t, ms
for name, v in (("clean frames", clean), ("within 40 ms of a search", after_find)):
    if v: print("  %s: n=%d median %d p95 %d max %d px" % (name, len(v), statistics.median(v), pct(v, 95), max(v)))
if speeds: print("  scroll speed px/s: median %d p95 %d" % (statistics.median(speeds), pct(speeds, 95)))
if lags: print("  error in frames of movement: median %.1f p95 %.1f" % (statistics.median(lags), pct(lags, 95)))
