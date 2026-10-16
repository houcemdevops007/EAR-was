/* EFI File Center — dépôt par glisser-déposer, file d'envoi avec progression, liste et suppression. */
(function () {
  'use strict';
  var $ = EFI.$;
  var cfg = null, files = [];
  var ICONS = { pdf: '📕', txt: '📄', md: '📝', csv: '📊', log: '📜', json: '🧾', xml: '🧾', png: '🖼️', jpg: '🖼️',
    jpeg: '🖼️', gif: '🖼️', zip: '🗜️', docx: '📘', xlsx: '📗', pptx: '📙', odt: '📘' };

  function ext(name) { var i = name.lastIndexOf('.'); return i < 0 ? '' : name.substring(i + 1).toLowerCase(); }

  /* ---------- Chargement ---------- */
  function load() {
    return EFI.ajax({ url: EFI.url('/api/files') }).then(function (data) {
      cfg = data.config;
      files = data.files;
      renderStatus(data.storage);
      renderList();
    }).catch(function (err) {
      var body = $('#files-body');
      clear(body);
      body.appendChild(EFI.el('tr', null, [EFI.el('td', { colspan: '4', 'class': 'muted', text: 'Impossible de charger la liste : ' + err.message })]));
    });
  }

  function renderStatus(st) {
    var box = $('#storage-status');
    box.hidden = false;
    box.className = 'alert ' + (st.writable ? 'alert-success' : 'alert-danger');
    $('#storage-title').textContent = st.writable ? 'Stockage opérationnel.' : 'Stockage indisponible.';
    $('#storage-message').textContent = st.writable ? '' : st.message;
    $('#storage-dir').textContent = cfg.directory;
    $('#storage-host').textContent = st.host;
    var fix = $('#storage-fix');
    fix.hidden = st.writable;
    if (!st.writable) {
      $('#fix-commands').textContent =
        'sudo mkdir -p ' + cfg.directory + '\n' +
        'sudo chown wasadmin:wasgroup ' + cfg.directory + '\n' +
        'sudo chmod 750 ' + cfg.directory + '\n' +
        'ls -ld ' + cfg.directory;
    }
    $('#dz-rules').textContent = 'Max ' + cfg.maxBytesText + ' par fichier · ' + cfg.allowedExtensions.join(', ');
    $('#file-input').setAttribute('accept', cfg.allowedExtensions.map(function (e) { return '.' + e; }).join(','));
    $('#hs-count').textContent = String(st.count);
    $('#hs-size').textContent = EFI.fmtBytes(st.totalBytes);
    $('#hs-limit').textContent = cfg.maxBytesText;
    var pct = Math.min(100, Math.round(st.count * 100 / cfg.maxCount));
    $('#usage-bar').style.width = pct + '%';
    $('#usage-text').textContent = st.count + ' / ' + cfg.maxCount + ' fichiers · ' + EFI.fmtBytes(st.totalBytes) + ' au total';
  }

  function clear(n) { while (n.firstChild) { n.removeChild(n.firstChild); } }

  function renderList() {
    var body = $('#files-body');
    clear(body);
    var q = $('#file-search').value.toLowerCase();
    var shown = files.filter(function (f) { return !q || f.name.toLowerCase().indexOf(q) >= 0; });
    if (!shown.length) {
      body.appendChild(EFI.el('tr', null, [EFI.el('td', { colspan: '4', 'class': 'empty-row', text: files.length ? 'Aucun fichier ne correspond au filtre.' : 'Aucun fichier pour le moment : déposez-en un !' })]));
      return;
    }
    shown.forEach(function (f, i) {
      var dl = EFI.el('a', { 'class': 'btn btn-ghost btn-sm', href: EFI.url('/api/files/' + f.id), download: f.name, title: 'Télécharger' }, [EFI.icon('M12 3v12M7 10l5 5 5-5M5 21h14'), 'Télécharger']);
      var del = EFI.el('button', { type: 'button', 'class': 'icon-btn danger', 'aria-label': 'Supprimer ' + f.name, title: 'Supprimer',
        onclick: function () { remove(f); } }, [EFI.icon('M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6')]);
      body.appendChild(EFI.el('tr', { 'class': 'file-row', style: 'animation-delay:' + Math.min(i * 30, 300) + 'ms' }, [
        EFI.el('td', null, [EFI.el('div', { 'class': 'file-name' }, [
          EFI.el('span', { 'class': 'file-icon', 'aria-hidden': 'true', text: ICONS[f.extension] || '📁' }),
          EFI.el('div', null, [EFI.el('strong', { text: f.name }), EFI.el('div', { 'class': 'sha', title: 'Empreinte SHA-256 : ' + f.sha256, text: 'SHA-256 ' + f.sha256.substring(0, 12) + '…' })])
        ])]),
        EFI.el('td', { 'class': 'nowrap', text: EFI.fmtBytes(f.size) }),
        EFI.el('td', { 'class': 'nowrap', text: EFI.fmtDateTime(f.uploadedAt) }),
        EFI.el('td', { 'class': 'actions' }, [dl, del])
      ]));
    });
  }

  function remove(f) {
    if (!window.confirm('Supprimer définitivement « ' + f.name + ' » ?')) { return; }
    EFI.ajax({ method: 'DELETE', url: EFI.url('/api/files/' + f.id) }).then(function () {
      EFI.toast('Fichier supprimé.', 'success');
      return load();
    }).catch(function (err) { EFI.toast(err.message, 'error'); });
  }

  /* ---------- Envoi ---------- */
  function precheck(file) {
    if (!cfg) { return null; }
    var e = ext(file.name);
    if (cfg.allowedExtensions.indexOf(e) < 0) { return 'Extension .' + (e || '?') + ' non autorisée.'; }
    if (file.size > cfg.maxBytes) { return 'Trop volumineux (' + EFI.fmtBytes(file.size) + ' > ' + cfg.maxBytesText + ').'; }
    if (file.size === 0) { return 'Fichier vide.'; }
    return null;
  }

  var queue = [], busy = false;
  function enqueue(list) {
    Array.prototype.forEach.call(list, function (file) {
      var bar = EFI.el('span', { 'class': 'q-bar' });
      var status = EFI.el('span', { 'class': 'q-status', text: 'En attente…' });
      var item = EFI.el('li', { 'class': 'q-item' }, [
        EFI.el('span', { 'class': 'file-icon', 'aria-hidden': 'true', text: ICONS[ext(file.name)] || '📁' }),
        EFI.el('div', { 'class': 'q-main' }, [
          EFI.el('div', { 'class': 'q-top' }, [EFI.el('strong', { 'class': 'q-name', text: file.name }), EFI.el('span', { 'class': 'small muted', text: EFI.fmtBytes(file.size) })]),
          EFI.el('div', { 'class': 'q-progress' }, [bar]), status
        ])
      ]);
      $('#queue').insertBefore(item, $('#queue').firstChild);
      var err = precheck(file);
      if (err) { fail(item, status, err); return; }
      queue.push({ file: file, item: item, bar: bar, status: status });
    });
    next();
  }

  function fail(item, status, msg) { item.classList.add('is-error'); status.textContent = '✗ ' + msg; }

  function next() {
    if (busy || !queue.length) { return; }
    busy = true;
    var job = queue.shift();
    var fd = new FormData();
    fd.append('_csrf', EFI.csrf);
    fd.append('file', job.file, job.file.name);
    job.status.textContent = 'Envoi en cours…';
    job.item.classList.add('is-sending');
    EFI.ajax({ method: 'POST', url: EFI.url('/api/files'), body: fd, timeout: 120000,
      onProgress: function (p) { job.bar.style.width = Math.round(p * 100) + '%'; } })
      .then(function (res) {
        job.bar.style.width = '100%';
        job.item.classList.remove('is-sending');
        if (res.errors && res.errors.length) { fail(job.item, job.status, res.errors[0].error); }
        else { job.item.classList.add('is-done'); job.status.textContent = '✓ Enregistré sous un nom généré par le serveur'; }
        return load();
      })
      .catch(function (err) {
        job.item.classList.remove('is-sending');
        var msg = err.data && err.data.errors && err.data.errors.length ? err.data.errors[0].error : err.message;
        fail(job.item, job.status, msg);
      })
      .then(function () {
        busy = false;
        setTimeout(function () { if (job.item.classList.contains('is-done') && job.item.parentNode) { job.item.classList.add('leaving'); setTimeout(function () { if (job.item.parentNode) { job.item.parentNode.removeChild(job.item); } }, 400); } }, 6000);
        next();
      });
  }

  /* ---------- Zone de dépôt ---------- */
  var dz = $('#dropzone'), input = $('#file-input');
  ['dragenter', 'dragover'].forEach(function (t) {
    dz.addEventListener(t, function (e) { e.preventDefault(); dz.classList.add('drag-over'); });
  });
  ['dragleave', 'drop'].forEach(function (t) {
    dz.addEventListener(t, function (e) { e.preventDefault(); if (t === 'drop' || e.target === dz) { dz.classList.remove('drag-over'); } });
  });
  dz.addEventListener('drop', function (e) { if (e.dataTransfer && e.dataTransfer.files.length) { enqueue(e.dataTransfer.files); } });
  input.addEventListener('change', function () { if (input.files.length) { enqueue(input.files); } input.value = ''; });
  dz.addEventListener('keydown', function (e) { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); input.click(); } });
  dz.setAttribute('tabindex', '0');
  $('#upload-form').addEventListener('submit', function (e) { e.preventDefault(); });
  /* Empêche le navigateur d'ouvrir un fichier lâché à côté de la zone */
  window.addEventListener('dragover', function (e) { e.preventDefault(); });
  window.addEventListener('drop', function (e) { e.preventDefault(); });

  $('#file-search').addEventListener('input', EFI.debounce(renderList, 120));
  $('#refresh-btn').addEventListener('click', function () { load().then(function () { EFI.toast('Liste actualisée.', 'info'); }); });

  load();
})();
