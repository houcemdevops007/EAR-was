/* EFI Calendar — interface du calendrier mensuel interactif et horloge Europe/Paris */
(function () {
  'use strict';
  var C = window.EFICalendarCore;
  var $ = EFI.$;

  var today = C.todayParis();
  var view = { y: today.y, m: today.m };
  var selected = { y: today.y, m: today.m, d: today.d };
  var forecast = {}; // 'AAAA-MM-JJ' -> { code, max, min, rain, icon, label }

  var body = $('#cal-body');
  var selMonth = $('#sel-month');
  var selYear = $('#sel-year');

  /* --- Sélecteurs mois / année --- */
  C.MONTHS.forEach(function (name, i) {
    selMonth.appendChild(EFI.el('option', { value: String(i), text: name.charAt(0).toUpperCase() + name.slice(1) }));
  });
  for (var y = today.y - 10; y <= today.y + 10; y++) {
    selYear.appendChild(EFI.el('option', { value: String(y), text: String(y) }));
  }
  selMonth.addEventListener('change', function () { go(view.y, parseInt(selMonth.value, 10)); });
  selYear.addEventListener('change', function () { go(parseInt(selYear.value, 10), view.m); });

  function go(y, m) {
    while (m < 0) { m += 12; y--; }
    while (m > 11) { m -= 12; y++; }
    view.y = y;
    view.m = m;
    if (!selYear.querySelector('option[value="' + y + '"]')) {
      selYear.appendChild(EFI.el('option', { value: String(y), text: String(y) }));
    }
    render();
  }

  function same(a, b) { return a.y === b.y && a.m === b.m && a.d === b.d; }

  function render(focus) {
    $('#cal-month').textContent = C.MONTHS[view.m].charAt(0).toUpperCase() + C.MONTHS[view.m].slice(1);
    $('#cal-year').textContent = String(view.y);
    selMonth.value = String(view.m);
    selYear.value = String(view.y);
    var hol = {};
    var h1 = C.holidays(view.y - (view.m === 0 ? 1 : 0)), h2 = C.holidays(view.y), h3 = C.holidays(view.y + (view.m === 11 ? 1 : 0));
    [h1, h2, h3].forEach(function (h) { for (var k in h) { if (h.hasOwnProperty(k)) { hol[k] = h[k]; } } });

    body.textContent = '';
    C.monthMatrix(view.y, view.m).forEach(function (week) {
      var row = EFI.el('div', { 'class': 'cal-row', role: 'row' });
      row.appendChild(EFI.el('span', { 'class': 'wk', role: 'rowheader', title: 'Semaine ' + week.week, text: String(week.week) }));
      week.days.forEach(function (day) {
        var cls = ['day'];
        if (!day.inMonth) { cls.push('out'); }
        if (day.weekend) { cls.push('weekend'); }
        if (same(day, today)) { cls.push('today'); }
        var isSel = same(day, selected);
        if (isSel) { cls.push('selected'); }
        if (hol[day.key]) { cls.push('holiday'); }
        var fc = forecast[day.key];
        var label = C.DAYS[new Date(Date.UTC(day.y, day.m, day.d)).getUTCDay()] + ' ' + day.d + ' ' + C.MONTHS[day.m] + ' ' + day.y +
          (hol[day.key] ? ', ' + hol[day.key] : '') + (same(day, today) ? ', aujourd\u2019hui' : '');
        var btn = EFI.el('button', {
          type: 'button', role: 'gridcell', 'class': cls.join(' '), 'aria-selected': isSel ? 'true' : 'false',
          'aria-label': label, tabindex: isSel ? '0' : '-1', title: hol[day.key] || null, dataset: { key: day.key }
        }, [EFI.el('span', { 'class': 'num', text: String(day.d) })]);
        if (fc) { btn.appendChild(EFI.el('span', { 'class': 'wx-mini', 'aria-hidden': 'true', text: fc.icon })); }
        btn.addEventListener('click', function () { select(day.y, day.m, day.d); });
        row.appendChild(btn);
      });
      body.appendChild(row);
    });
    if (focus) {
      var s = body.querySelector('.selected');
      if (s) { s.focus(); }
    }
    renderDetails(hol);
  }

  function select(y, m, d, focus) {
    selected = { y: y, m: m, d: d };
    if (y !== view.y || m !== view.m) { view.y = y; view.m = m; }
    render(focus);
  }

  function renderDetails(hol) {
    var box = $('#day-details');
    var s = selected;
    var dow = C.DAYS[new Date(Date.UTC(s.y, s.m, s.d)).getUTCDay()];
    var diff = C.diffDays(today, s);
    var rel = diff === 0 ? 'Aujourd\u2019hui' : diff === 1 ? 'Demain' : diff === -1 ? 'Hier' :
      diff > 0 ? 'Dans ' + diff + ' jours' : 'Il y a ' + (-diff) + ' jours';
    var k = C.key(s.y, s.m, s.d);
    var total = C.isLeap(s.y) ? 366 : 365;
    box.textContent = '';
    box.appendChild(EFI.el('div', { 'class': 'day-big' }, [
      EFI.el('span', { 'class': 'day-num', text: String(s.d) }),
      EFI.el('div', {}, [
        EFI.el('div', { 'class': 'day-name', text: dow.charAt(0).toUpperCase() + dow.slice(1) }),
        EFI.el('div', { 'class': 'muted', text: C.MONTHS[s.m] + ' ' + s.y })
      ]),
      EFI.el('span', { 'class': 'badge ' + (diff === 0 ? 'badge-success' : ''), text: rel })
    ]));
    var list = EFI.el('dl', { 'class': 'day-facts' });
    function fact(dt, dd) { list.appendChild(EFI.el('div', {}, [EFI.el('dt', { text: dt }), EFI.el('dd', { text: dd })])); }
    fact('Semaine ISO', 'S' + C.isoWeek(s.y, s.m, s.d));
    fact('Jour de l\u2019année', C.dayOfYear(s.y, s.m, s.d) + ' / ' + total);
    fact('Jour férié', hol[k] || 'Non');
    var fc = forecast[k];
    if (fc) {
      fact('Prévision', fc.icon + ' ' + fc.label);
      fact('Températures', fc.min + ' °C / ' + fc.max + ' °C');
      fact('Pluie (probabilité)', fc.rain === null ? 'n.d.' : fc.rain + ' %');
    } else {
      fact('Prévision', 'Aucune (hors des 7 prochains jours)');
    }
    box.appendChild(list);
  }

  /* --- Navigation clavier dans la grille --- */
  body.addEventListener('keydown', function (e) {
    var moves = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 };
    var n = null;
    if (moves.hasOwnProperty(e.key)) { n = C.addDays(selected.y, selected.m, selected.d, moves[e.key]); }
    else if (e.key === 'PageUp') { n = { y: selected.y, m: selected.m - 1, d: 1 }; }
    else if (e.key === 'PageDown') { n = { y: selected.y, m: selected.m + 1, d: 1 }; }
    else if (e.key === 'Home') { n = { y: today.y, m: today.m, d: today.d }; }
    if (n) {
      e.preventDefault();
      var dt = new Date(Date.UTC(n.y, n.m, n.d));
      select(dt.getUTCFullYear(), dt.getUTCMonth(), dt.getUTCDate(), true);
    }
  });

  $('#prev-month').addEventListener('click', function () { go(view.y, view.m - 1); });
  $('#next-month').addEventListener('click', function () { go(view.y, view.m + 1); });
  $('#prev-year').addEventListener('click', function () { go(view.y - 1, view.m); });
  $('#next-year').addEventListener('click', function () { go(view.y + 1, view.m); });
  $('#today-btn').addEventListener('click', function () {
    today = C.todayParis();
    select(today.y, today.m, today.d, true);
  });

  /* --- Horloge numérique (heure du serveur, fuseau Europe/Paris) --- */
  var clock = $('#big-clock');
  var dateEl = $('#big-date');
  var offset = parseInt(clock.getAttribute('data-server-epoch'), 10) - Date.now();
  if (isNaN(offset) || Math.abs(offset) < 1500) { offset = 0; }
  function tick() {
    var now = new Date(Date.now() + offset);
    try {
      clock.textContent = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'Europe/Paris' });
      dateEl.textContent = now.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Europe/Paris' });
      var tz = new Intl.DateTimeFormat('fr-FR', { timeZone: 'Europe/Paris', timeZoneName: 'short' }).formatToParts(now)
        .filter(function (p) { return p.type === 'timeZoneName'; });
      if (tz.length) { $('#tz-label').textContent = 'Europe/Paris · ' + tz[0].value; }
    } catch (e) {
      clock.textContent = now.toTimeString().substring(0, 8);
    }
    // Changement de jour à minuit : on rafraîchit la date du jour
    var t = C.todayParis(now);
    if (!same(t, today)) { today = t; render(); }
  }
  tick();
  setInterval(tick, 1000);

  /* --- Intégration des prévisions météo (événement émis par weather.js) --- */
  document.addEventListener('efi:forecast', function (e) {
    forecast = e.detail || {};
    render();
  });

  render();
})();
