/* EFI Calculator — interface : clavier virtuel et physique, historique, thème clair/sombre. */
(function () {
  'use strict';
  var $ = EFI.$, C = window.CalcCore;
  var HKEY = 'efi.calc.history.v1', TKEY = 'efi.calc.theme';
  var expr = $('#expr'), result = $('#result'), errorBox = $('#calc-error');
  var history = [], justEvaluated = false, lastValue = null;

  function store(k, v) { try { window.localStorage.setItem(k, v); } catch (e) { /* stockage indisponible */ } }
  function read(k) { try { return window.localStorage.getItem(k); } catch (e) { return null; } }

  /* ---------- Saisie ---------- */
  function insert(token) {
    var v = expr.value;
    var isOp = /^[+\-*/%]$/.test(token);
    if (justEvaluated) {
      // après « = » : un opérateur continue avec le résultat, un chiffre démarre un nouveau calcul
      v = isOp && lastValue !== null ? String(lastValue).replace('.', ',') : '';
      justEvaluated = false;
    }
    if (isOp && token !== '-' && token !== '%' && v === '') { v = '0'; }
    // remplace un opérateur binaire final par le nouveau (évite « 2+*3 »)
    if (/[+*/]$/.test(v) && /[+*/]/.test(token)) { v = v.slice(0, -1); }
    if (token === ',') {
      var m = /[\d,]*$/.exec(v)[0];
      if (m.indexOf(',') >= 0) { flash(); return; }
      if (m === '') { token = '0,'; }
    }
    if (v.length + token.length > C.MAX_LENGTH) { flash(); return; }
    expr.value = v + token;
    update();
  }

  function back() {
    if (justEvaluated) { clearAll(); return; }
    expr.value = expr.value.slice(0, -1);
    update();
  }

  function clearAll() {
    expr.value = ''; justEvaluated = false; lastValue = null;
    update();
  }

  function sign() {
    if (justEvaluated && lastValue !== null) { expr.value = String(lastValue).replace('.', ','); justEvaluated = false; }
    expr.value = C.toggleSign(expr.value);
    update();
  }

  /** Aperçu en direct du résultat (sans erreur bloquante). */
  function update() {
    errorBox.textContent = '';
    $('#display').classList.remove('has-error');
    var v = expr.value;
    if (!v) { result.textContent = '0'; result.classList.remove('preview'); return; }
    var p = C.tryEvaluate(v);
    result.textContent = p === null ? '' : C.format(p);
    result.classList.toggle('preview', !justEvaluated);
  }

  function equals() {
    var v = expr.value;
    if (!v) { return; }
    try {
      var r = C.evaluate(C.autoClose(v));
      addHistory(C.autoClose(v), r);
      lastValue = r;
      justEvaluated = true;
      result.textContent = C.format(r);
      result.classList.remove('preview');
      result.classList.remove('pop'); void result.offsetWidth; result.classList.add('pop');
      expr.value = C.autoClose(v);
    } catch (e) {
      errorBox.textContent = e.message;
      $('#display').classList.add('has-error');
      result.textContent = 'Erreur';
      flash();
    }
  }

  function flash() {
    var d = $('#display');
    d.classList.remove('shake'); void d.offsetWidth; d.classList.add('shake');
  }

  /* ---------- Historique ---------- */
  function addHistory(e, r) {
    history.unshift({ e: e, r: r, t: Date.now() });
    if (history.length > 50) { history.length = 50; }
    store(HKEY, JSON.stringify(history));
    renderHistory();
  }

  function renderHistory() {
    var list = $('#history-list');
    while (list.firstChild) { list.removeChild(list.firstChild); }
    history.forEach(function (h, i) {
      var btn = EFI.el('button', { type: 'button', 'class': 'h-item', title: 'Réutiliser ce calcul',
        onclick: function () { expr.value = h.e; justEvaluated = false; update(); expr.focus(); } }, [
        EFI.el('span', { 'class': 'h-expr', text: C.pretty(h.e) + ' =' }),
        EFI.el('span', { 'class': 'h-res', text: C.format(h.r) }),
        EFI.el('span', { 'class': 'h-time', text: EFI.fmtTime(new Date(h.t)) })
      ]);
      list.appendChild(EFI.el('li', { style: i === 0 ? 'animation: fadeUp .35s var(--ease) both' : null }, [btn]));
    });
    $('#history-empty').hidden = history.length > 0;
    $('#history-clear').disabled = history.length === 0;
  }

  $('#history-clear').addEventListener('click', function () {
    history = []; store(HKEY, '[]'); renderHistory();
    EFI.toast('Historique effacé.', 'info');
  });

  /* ---------- Clavier virtuel ---------- */
  Array.prototype.forEach.call(document.querySelectorAll('.keypad .key'), function (k) {
    k.addEventListener('click', function () {
      var ins = k.getAttribute('data-insert'), act = k.getAttribute('data-action');
      if (ins !== null) { insert(ins); }
      else if (act === 'clear') { clearAll(); }
      else if (act === 'back') { back(); }
      else if (act === 'sign') { sign(); }
      else if (act === 'equals') { equals(); }
      press(k);
    });
  });

  function press(k) { if (!k) { return; } k.classList.add('pressed'); setTimeout(function () { k.classList.remove('pressed'); }, 140); }
  function keyFor(sel) { return document.querySelector('.keypad ' + sel); }

  /* ---------- Clavier physique ---------- */
  document.addEventListener('keydown', function (e) {
    if (e.ctrlKey || e.metaKey || e.altKey) { return; }
    var k = e.key, handled = true;
    if ((k === 'Enter' || k === ' ') && e.target && e.target.tagName === 'BUTTON') { return; }
    if (/^[0-9]$/.test(k)) { insert(k); press(keyFor('[data-insert="' + k + '"]')); }
    else if (k === ',' || k === '.') { insert(','); press(keyFor('[data-insert=","]')); }
    else if (k === '+' || k === '-' || k === '*' || k === '/' || k === '%' || k === '(' || k === ')') { insert(k); press(keyFor('[data-insert="' + k + '"]')); }
    else if (k === 'x' || k === 'X') { insert('*'); press(keyFor('[data-insert="*"]')); }
    else if (k === 'Enter' || k === '=') { equals(); press(keyFor('[data-action="equals"]')); }
    else if (k === 'Backspace') { back(); press(keyFor('[data-action="back"]')); }
    else if (k === 'Escape' || k === 'Delete') { clearAll(); press(keyFor('[data-action="clear"]')); }
    else { handled = false; }
    if (handled) { e.preventDefault(); }
  });
  // le champ est en lecture via le clavier global : on empêche la saisie directe en double
  expr.addEventListener('beforeinput', function (e) { e.preventDefault(); });
  expr.addEventListener('paste', function (e) {
    e.preventDefault();
    var text = (e.clipboardData || window.clipboardData).getData('text') || '';
    text = text.replace(/[^0-9+\-*/%().,×÷−\s]/g, '').replace(/\s+/g, '');
    text = text.replace(/×/g, '*').replace(/÷/g, '/').replace(/−/g, '-').replace(/\./g, ',');
    if (text) { justEvaluated = false; expr.value = (expr.value + text).slice(0, C.MAX_LENGTH); update(); }
  });

  $('#copy-btn').addEventListener('click', function () {
    var text = result.textContent;
    if (!text || text === 'Erreur') { return; }
    var done = function () { EFI.toast('Résultat copié : ' + text, 'success'); };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done, function () { EFI.toast('Copie impossible.', 'error'); });
    } else { EFI.toast('Copie non prise en charge par ce navigateur.', 'error'); }
  });

  /* ---------- Thème clair / sombre ---------- */
  var toggle = $('#theme-toggle');
  function applyTheme(dark, persist) {
    document.body.classList.toggle('theme-dark', dark);
    toggle.setAttribute('aria-pressed', String(dark));
    toggle.setAttribute('aria-label', dark ? 'Activer le mode clair' : 'Activer le mode sombre');
    $('#theme-label').textContent = dark ? 'Mode sombre' : 'Mode clair';
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) { meta.setAttribute('content', dark ? '#020617' : '#0b1437'); }
    if (persist) { store(TKEY, dark ? 'dark' : 'light'); }
  }
  toggle.addEventListener('click', function () { applyTheme(!document.body.classList.contains('theme-dark'), true); });
  var saved = read(TKEY);
  var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  applyTheme(saved ? saved === 'dark' : prefersDark, false);

  /* ---------- Démarrage ---------- */
  try { history = JSON.parse(read(HKEY) || '[]'); if (!(history instanceof Array)) { history = []; } }
  catch (e) { history = []; }
  history = history.filter(function (h) { return h && typeof h.e === 'string' && typeof h.r === 'number' && h.e.length <= C.MAX_LENGTH; });
  renderHistory();
  update();
})();
