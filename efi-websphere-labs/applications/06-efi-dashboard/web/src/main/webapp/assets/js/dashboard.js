/* EFI Server Dashboard — rafraîchissement toutes les 10 s et graphiques SVG sans bibliothèque. */
(function () {
  'use strict';
  var $ = EFI.$;
  var PERIOD = 10;            // secondes
  var MAX_POINTS = 30;        // 5 minutes d'historique
  var SVGNS = 'http://www.w3.org/2000/svg';
  var history = [];           // { t, used, committed, max, requests }
  var countdown = PERIOD, paused = false, inflight = false, lastReq = null;

  function mb(b) { return b / 1048576; }
  function fmtMb(b) { return mb(b).toFixed(mb(b) < 100 ? 1 : 0).replace('.', ',') + ' Mo'; }
  function fmtInt(n) { try { return Number(n).toLocaleString('fr-FR'); } catch (e) { return String(n); } }
  function set(id, v) { var e = document.getElementById(id); if (e) { e.textContent = (v === null || v === undefined || v === '') ? '—' : v; } }

  /* ---------- Collecte ---------- */
  function load() {
    if (inflight) { return; }
    inflight = true;
    EFI.ajax({ url: EFI.url('/api/metrics'), timeout: 8000 }).then(function (m) {
      $('#metrics-error').hidden = true;
      render(m);
      setLive(true);
    }).catch(function (err) {
      $('#metrics-error').hidden = false;
      set('metrics-error-text', err.message);
      setLive(false);
    }).then(function () { inflight = false; countdown = PERIOD; tickRing(); });
  }

  function setLive(ok) {
    var b = $('#live-badge');
    b.className = 'badge badge-live ' + (paused ? 'badge-warning' : (ok ? 'badge-success' : 'badge-danger'));
    b.textContent = paused ? 'En pause' : (ok ? 'En direct' : 'Hors ligne');
  }

  /* ---------- Rendu ---------- */
  function render(m) {
    var mem = m.memory, app = m.app;
    set('id-host', m.server.hostname);
    set('id-server', m.server.serverName || 'hors WebSphere');
    set('id-node', m.server.nodeName);
    set('id-cell', m.server.cellName);
    set('last-update', 'Dernier relevé : ' + EFI.fmtDateTime(m.server.time, true));

    var pct = Math.round(mem.heapUsedPercent);
    set('k-heap-pct', String(pct));
    set('k-heap-sub', fmtMb(mem.heapUsed) + ' utilisés / ' + fmtMb(mem.heapMax) + ' max · libre ' + fmtMb(mem.heapFree));
    var g = $('#gauge-fg');
    g.style.strokeDasharray = pct + ' 100';
    g.setAttribute('class', 'g-fg ' + (pct >= 85 ? 'hot' : (pct >= 65 ? 'warm' : '')));

    set('k-uptime', m.jvm.uptimeText);
    set('k-uptime-sub', 'Application : ' + app.uptimeText);

    var delta = lastReq === null ? 0 : Math.max(0, app.requests - lastReq);
    lastReq = app.requests;
    set('k-req', fmtInt(app.requests));
    set('k-req-sub', '+' + delta + ' depuis le dernier relevé · ' + app.errors + ' erreur(s)');

    set('k-cpu', String(m.os.processors));
    set('k-cpu-sub', m.os.loadAverage >= 0 ? 'Charge moyenne : ' + String(m.os.loadAverage).replace('.', ',') : 'Charge non disponible sur ce système');
    set('k-threads', fmtInt(m.threads.live));
    set('k-threads-sub', 'Pic : ' + m.threads.peak + ' · démons : ' + m.threads.daemon);
    set('k-sessions', fmtInt(app.activeSessions));
    set('k-sessions-sub', app.createdSessions + ' créée(s) depuis le démarrage');

    set('j-version', m.java.version);
    set('j-vendor', m.java.vendor);
    set('j-vm', m.java.vmName + ' ' + m.java.vmVersion);
    set('j-spec', 'Java SE ' + m.java.specVersion);
    set('j-classes', fmtInt(m.classes.loaded) + ' (total ' + fmtInt(m.classes.totalLoaded) + ')');
    set('o-name', m.os.name);
    set('o-version', m.os.version);
    set('o-arch', m.os.arch);
    set('o-load', m.os.loadAverage >= 0 ? String(m.os.loadAverage).replace('.', ',') : 'non disponible');
    set('o-time', m.server.timeParis + ' (Europe/Paris)');
    set('j-start', EFI.fmtDateTime(m.jvm.startTime, true));
    set('a-start', EFI.fmtDateTime(app.startedAt, true));

    var tbody = $('#gc-table tbody');
    while (tbody.firstChild) { tbody.removeChild(tbody.firstChild); }
    m.gc.forEach(function (c) {
      tbody.appendChild(EFI.el('tr', null, [
        EFI.el('td', { text: c.name }), EFI.el('td', { text: fmtInt(c.count) }), EFI.el('td', { text: fmtInt(c.timeMs) + ' ms' })
      ]));
    });

    history.push({ t: new Date(m.server.time), used: mem.heapUsed, committed: mem.heapCommitted, max: mem.heapMax, delta: history.length ? delta : 0 });
    if (history.length > MAX_POINTS) { history.shift(); }
    drawHeap();
    drawRequests();
  }

  /* ---------- Graphiques SVG ---------- */
  function svgEl(name, attrs) {
    var e = document.createElementNS(SVGNS, name);
    for (var k in attrs) { if (Object.prototype.hasOwnProperty.call(attrs, k)) { e.setAttribute(k, attrs[k]); } }
    return e;
  }
  function clear(svg) { while (svg.firstChild) { svg.removeChild(svg.firstChild); } }
  var W = 600, H = 220, L = 46, R = 12, T = 12, B = 26;

  function axes(svg, maxV, unit) {
    for (var i = 0; i <= 4; i++) {
      var y = T + (H - T - B) * i / 4;
      svg.appendChild(svgEl('line', { x1: L, x2: W - R, y1: y, y2: y, 'class': 'grid-line' }));
      var t = svgEl('text', { x: L - 6, y: y + 4, 'class': 'axis-label', 'text-anchor': 'end' });
      t.textContent = Math.round(maxV * (4 - i) / 4) + unit;
      svg.appendChild(t);
    }
    if (history.length) {
      [0, history.length - 1].forEach(function (idx, n) {
        var x = xAt(idx);
        var t2 = svgEl('text', { x: x, y: H - 6, 'class': 'axis-label', 'text-anchor': n ? 'end' : 'start' });
        t2.textContent = EFI.fmtTime(history[idx].t);
        svg.appendChild(t2);
      });
    }
  }
  function xAt(i) { return L + (W - L - R) * (MAX_POINTS <= 1 ? 0 : (i + (MAX_POINTS - history.length)) / (MAX_POINTS - 1)); }
  function yAt(v, maxV) { return T + (H - T - B) * (1 - (maxV ? v / maxV : 0)); }

  function drawHeap() {
    var svg = $('#chart-heap'); clear(svg);
    var maxV = 0;
    history.forEach(function (p) { maxV = Math.max(maxV, mb(p.max || p.committed)); });
    maxV = Math.ceil(maxV / 64) * 64 || 64;
    axes(svg, maxV, '');
    if (!history.length) { return; }
    var defs = svgEl('defs', {});
    var lg = svgEl('linearGradient', { id: 'heapFill', x1: 0, y1: 0, x2: 0, y2: 1 });
    lg.appendChild(svgEl('stop', { offset: '0', style: 'stop-color:var(--accent);stop-opacity:.35' }));
    lg.appendChild(svgEl('stop', { offset: '1', style: 'stop-color:var(--accent);stop-opacity:0' }));
    defs.appendChild(lg); svg.appendChild(defs);

    var maxY = yAt(mb(history[history.length - 1].max), maxV);
    svg.appendChild(svgEl('line', { x1: L, x2: W - R, y1: maxY, y2: maxY, 'class': 'max-line' }));
    svg.appendChild(svgEl('polyline', { points: pts('committed', maxV), 'class': 'line-committed' }));
    var used = pts('used', maxV);
    svg.appendChild(svgEl('polygon', { points: xAt(0) + ',' + yAt(0, maxV) + ' ' + used + ' ' + xAt(history.length - 1) + ',' + yAt(0, maxV), fill: 'url(#heapFill)' }));
    svg.appendChild(svgEl('polyline', { points: used, 'class': 'line-used' }));
    var last = history[history.length - 1];
    svg.appendChild(svgEl('circle', { cx: xAt(history.length - 1), cy: yAt(mb(last.used), maxV), r: 4.5, 'class': 'dot-used' }));
    set('heap-legend', fmtMb(last.used) + ' / ' + fmtMb(last.max));
  }
  function pts(key, maxV) {
    return history.map(function (p, i) { return xAt(i).toFixed(1) + ',' + yAt(mb(p[key]), maxV).toFixed(1); }).join(' ');
  }

  function drawRequests() {
    var svg = $('#chart-req'); clear(svg);
    var maxV = 5;
    history.forEach(function (p) { maxV = Math.max(maxV, p.delta); });
    maxV = Math.ceil(maxV / 5) * 5;
    axes(svg, maxV, '');
    var step = (W - L - R) / MAX_POINTS;
    var total = 0;
    history.forEach(function (p, i) {
      total += p.delta;
      var h = (H - T - B) * p.delta / maxV;
      var x = xAt(i) - step * 0.35;
      var r = svgEl('rect', { x: Math.max(L, x), y: H - B - h, width: Math.max(2, step * 0.7), height: Math.max(0, h), rx: 3, 'class': 'bar' });
      var title = svgEl('title', {}); title.textContent = EFI.fmtTime(p.t) + ' : ' + p.delta + ' requête(s)';
      r.appendChild(title);
      svg.appendChild(r);
    });
    set('req-legend', total + ' requête(s) sur la période');
  }

  /* ---------- Compte à rebours ---------- */
  function tickRing() {
    set('rr-count', paused ? '⏸' : String(countdown));
    var fg = $('#rr-fg');
    var c = 2 * Math.PI * 19;
    fg.style.strokeDasharray = (c * countdown / PERIOD).toFixed(1) + ' ' + c.toFixed(1);
  }

  setInterval(function () {
    if (paused || document.hidden) { return; }
    countdown--;
    if (countdown <= 0) { countdown = PERIOD; load(); }
    tickRing();
  }, 1000);

  $('#btn-refresh').addEventListener('click', function () { load(); });
  $('#btn-pause').addEventListener('click', function () {
    paused = !paused;
    this.setAttribute('aria-pressed', String(paused));
    this.textContent = paused ? '▶ Reprendre' : '⏸ Pause';
    setLive(true);
    tickRing();
    EFI.toast(paused ? 'Rafraîchissement automatique en pause.' : 'Rafraîchissement automatique repris.', 'info');
  });
  document.addEventListener('visibilitychange', function () { if (!document.hidden && !paused) { load(); } });

  drawHeap(); drawRequests(); tickRing();
  load();
})();
