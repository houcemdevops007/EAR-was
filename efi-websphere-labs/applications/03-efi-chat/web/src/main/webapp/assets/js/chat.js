/* EFI Chat — client AJAX (polling), rendu sécurisé via textContent */
(function () {
  'use strict';
  var $ = EFI.$;
  var app = $('#chat-app');
  var me = app.getAttribute('data-nick') || null;
  var lastId = 0;
  var timer = null;
  var failures = 0;
  var lastRenderedNick = null;
  var lastRenderedTime = 0;
  var isAdmin = false;
  var generation = null;

  var msgBox = $('#messages');
  var input = $('#message');

  function hue(name) {
    var h = 0;
    for (var i = 0; i < name.length; i++) { h = (h * 31 + name.charCodeAt(i)) % 360; }
    return h;
  }
  function initials(name) {
    var p = name.trim().split(/[\s._-]+/);
    return ((p[0] || '?').charAt(0) + (p.length > 1 ? p[1].charAt(0) : (p[0] || '').charAt(1) || '')).toUpperCase();
  }
  function avatar(name, cls) {
    var a = EFI.el('span', { 'class': 'avatar ' + (cls || ''), 'aria-hidden': 'true', text: initials(name) });
    a.style.background = 'linear-gradient(135deg, hsl(' + hue(name) + ' 70% 55%), hsl(' + ((hue(name) + 40) % 360) + ' 70% 45%))';
    return a;
  }

  /* ---------- Écrans ---------- */
  function showChat() {
    $('#join-screen').hidden = true;
    $('#chat-screen').hidden = false;
    $('#me-nick').textContent = me;
    var av = $('#me-avatar');
    av.textContent = initials(me);
    av.style.background = 'linear-gradient(135deg, hsl(' + hue(me) + ' 70% 55%), hsl(' + ((hue(me) + 40) % 360) + ' 70% 45%))';
    input.focus();
    startPolling(true);
  }
  function showJoin() {
    stopPolling();
    $('#chat-screen').hidden = true;
    $('#join-screen').hidden = false;
    $('#nickname').focus();
  }

  /* ---------- Rendu des messages ---------- */
  function renderMessage(m) {
    var empty = $('#empty-state');
    if (empty) { empty.parentNode.removeChild(empty); }
    var t = new Date(m.time).getTime();
    if (m.type === 'system') {
      msgBox.appendChild(EFI.el('div', { 'class': 'sys-msg' }, [
        EFI.el('span', { text: m.text }), EFI.el('time', { datetime: m.time, text: ' · ' + EFI.fmtTime(m.time) })
      ]));
      lastRenderedNick = null;
      return;
    }
    var grouped = lastRenderedNick === m.nick && (t - lastRenderedTime) < 120000;
    var row = EFI.el('div', { 'class': 'msg-row' + (m.mine ? ' mine' : '') + (grouped ? ' grouped' : '') });
    if (!m.mine) { row.appendChild(grouped ? EFI.el('span', { 'class': 'avatar-spacer' }) : avatar(m.nick)); }
    var bubble = EFI.el('div', { 'class': 'bubble-msg' });
    if (!grouped && !m.mine) {
      var n = EFI.el('div', { 'class': 'msg-nick', text: m.nick });
      n.style.color = 'hsl(' + hue(m.nick) + ' 60% 40%)';
      bubble.appendChild(n);
    }
    bubble.appendChild(EFI.el('div', { 'class': 'msg-text', text: m.text }));
    bubble.appendChild(EFI.el('time', { 'class': 'msg-time', datetime: m.time, title: EFI.fmtDateTime(m.time, true), text: EFI.fmtTime(m.time) }));
    row.appendChild(bubble);
    msgBox.appendChild(row);
    lastRenderedNick = m.nick;
    lastRenderedTime = t;
  }

  function renderUsers(users) {
    var list = $('#user-list');
    list.textContent = '';
    users.forEach(function (u) {
      list.appendChild(EFI.el('li', { 'class': u === me ? 'is-me' : '' }, [
        avatar(u, 'avatar-sm'), EFI.el('span', { text: u + (u === me ? ' (vous)' : '') }), EFI.el('span', { 'class': 'online-dot', 'aria-label': 'en ligne' })
      ]));
    });
    $('#user-count').textContent = String(users.length);
  }

  /* ---------- Polling AJAX (une seule requête en vol à la fois) ---------- */
  var inflight = false;
  var again = false;
  function poll() {
    if (inflight) { again = true; return; }
    inflight = true;
    stopPolling();
    EFI.ajax({ url: EFI.url('/api/chat/messages?since=' + lastId), timeout: 8000 }).then(function (d) {
      failures = 0;
      setConn(true);
      if (d.me === null && me) { // session expirée ou serveur redémarré
        me = null;
        EFI.toast('Votre session a expiré : veuillez rejoindre à nouveau le salon.', 'info');
        showJoin();
        return;
      }
      if (generation !== null && d.generation !== generation) {
        // salon vidé par l'administrateur ou JVM redémarrée : on recharge tout l'historique
        generation = d.generation;
        lastId = 0;
        msgBox.textContent = '';
        lastRenderedNick = null;
        again = true;
        return;
      }
      generation = d.generation;
      var atBottom = msgBox.scrollHeight - msgBox.scrollTop - msgBox.clientHeight < 80;
      d.messages.forEach(renderMessage);
      if (d.messages.length && (atBottom || lastId === 0)) { msgBox.scrollTop = msgBox.scrollHeight; }
      lastId = d.lastId;
      renderUsers(d.users);
      setAdmin(d.admin);
    }, function () {
      failures++;
      setConn(false);
    }).then(function () {
      inflight = false;
      if (!me) { return; }
      if (again) { again = false; poll(); } else { schedule(); }
    });
  }
  function schedule() {
    stopPolling();
    var delay = document.hidden ? 6000 : (failures ? Math.min(2000 * (failures + 1), 15000) : 2000);
    timer = setTimeout(poll, delay);
  }
  function startPolling(reset) {
    if (reset) { lastId = 0; generation = null; msgBox.textContent = ''; lastRenderedNick = null; }
    poll();
  }
  function stopPolling() { if (timer) { clearTimeout(timer); timer = null; } }
  function setConn(ok) {
    var s = $('#conn-state');
    s.textContent = ok ? 'Connecté' : 'Reconnexion…';
    s.className = 'badge badge-live ' + (ok ? 'badge-success' : 'badge-warning');
  }
  function setAdmin(flag) {
    isAdmin = !!flag;
    $('#clear-btn').hidden = !isAdmin;
    $('#admin-btn').hidden = isAdmin;
  }

  /* ---------- Actions ---------- */
  $('#join-form').addEventListener('submit', function (e) {
    e.preventDefault();
    var nick = $('#nickname').value.trim();
    var err = $('#nick-error');
    err.textContent = '';
    $('#nickname').removeAttribute('aria-invalid');
    if (nick.length < 2 || nick.length > 20) {
      err.textContent = 'Le pseudonyme doit contenir entre 2 et 20 caractères.';
      $('#nickname').setAttribute('aria-invalid', 'true');
      return;
    }
    EFI.ajax({ method: 'POST', url: EFI.url('/api/chat/join'), data: { nickname: nick } }).then(function (d) {
      me = d.nickname;
      showChat();
      EFI.toast('Bienvenue ' + me + ' !', 'success');
    }, function (e2) {
      err.textContent = e2.message;
      $('#nickname').setAttribute('aria-invalid', 'true');
    });
  });

  function send() {
    var text = input.value;
    if (!text.trim()) { return; }
    var btn = $('.send-btn');
    btn.disabled = true;
    EFI.ajax({ method: 'POST', url: EFI.url('/api/chat/send'), data: { text: text } }).then(function () {
      input.value = '';
      updateCount();
      autosize();
      msgBox.scrollTop = msgBox.scrollHeight;
      poll();
    }, function (e) {
      EFI.toast(e.message, 'error');
      if (e.status === 401) { me = null; showJoin(); }
    }).then(function () { btn.disabled = false; input.focus(); });
  }
  $('#send-form').addEventListener('submit', function (e) { e.preventDefault(); send(); });
  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send(); }
  });
  function updateCount() {
    var n = input.value.length;
    var c = $('#char-count');
    c.textContent = n + ' / 500';
    c.classList.toggle('warn', n > 450);
  }
  function autosize() {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 160) + 'px';
  }
  input.addEventListener('input', function () { updateCount(); autosize(); });

  $('#leave-btn').addEventListener('click', function () {
    EFI.ajax({ method: 'POST', url: EFI.url('/api/chat/leave'), data: {} }).then(function () {
      me = null;
      setAdmin(false);
      showJoin();
      EFI.toast('Vous avez quitté le salon.', 'info');
    }, function (e) { EFI.toast(e.message, 'error'); });
  });

  /* Mode administrateur */
  var dlg = $('#admin-dialog');
  $('#admin-btn').addEventListener('click', function () {
    $('#admin-error').textContent = '';
    $('#admin-code').value = '';
    if (dlg.showModal) { dlg.showModal(); } else { dlg.setAttribute('open', ''); }
    $('#admin-code').focus();
  });
  $('#admin-cancel').addEventListener('click', function () { dlg.close(); });
  $('#admin-form').addEventListener('submit', function (e) {
    e.preventDefault();
    EFI.ajax({ method: 'POST', url: EFI.url('/api/chat/admin'), data: { code: $('#admin-code').value } }).then(function () {
      dlg.close();
      setAdmin(true);
      EFI.toast('Mode administrateur activé pour cette session.', 'success');
    }, function (e2) { $('#admin-error').textContent = e2.message; });
  });
  $('#clear-btn').addEventListener('click', function () {
    if (!window.confirm('Vider définitivement le salon pour tous les participants de cette JVM ?')) { return; }
    EFI.ajax({ method: 'POST', url: EFI.url('/api/chat/clear'), data: {} }).then(function () {
      EFI.toast('Salon vidé.', 'success');
      startPolling(true);
    }, function (e) { EFI.toast(e.message, 'error'); });
  });

  document.addEventListener('visibilitychange', function () { if (!document.hidden && me) { schedule(); } });

  if (me) { showChat(); } else { $('#nickname').focus(); }
})();
