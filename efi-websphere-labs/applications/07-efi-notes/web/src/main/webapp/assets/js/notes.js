/* EFI Notes — interface (DOM construit avec textContent, jamais innerHTML). */
(function () {
  'use strict';
  var $ = EFI.$, C = window.NotesCore;
  var KEY = 'efi.notes.v1';
  var notes = [], storageOk = true;
  var state = { category: '', query: '', inContent: false, favoritesOnly: false, sort: 'recent' };
  var editingId = null;

  /* ---------- Persistance localStorage ---------- */
  function load() {
    try {
      var raw = window.localStorage.getItem(KEY);
      notes = raw ? (C.parseImport(raw, Date.now()).notes) : [];
    } catch (e) {
      if (e && e.name === 'SecurityError' || !window.localStorage) { storageOk = false; }
      notes = [];
    }
  }
  function save() {
    try {
      window.localStorage.setItem(KEY, JSON.stringify({ format: 1, notes: notes }));
      storageOk = true;
    } catch (e) {
      storageOk = false;
      EFI.toast(e && e.name === 'QuotaExceededError' ? 'Quota de stockage dépassé.' : 'Impossible d\u2019enregistrer dans le navigateur.', 'error');
    }
    $('#storage-error').hidden = storageOk;
  }

  /* ---------- Rendu ---------- */
  function renderCategories() {
    var list = $('#cat-list'), st = C.stats(notes);
    while (list.firstChild) { list.removeChild(list.firstChild); }
    var items = [{ id: '', label: 'Toutes les notes', icon: '🗂️', count: st.total }].concat(C.CATEGORIES.map(function (c) {
      return { id: c.id, label: c.label, icon: c.icon, count: st.byCategory[c.id] };
    }));
    items.forEach(function (c) {
      var btn = EFI.el('button', { type: 'button', 'class': 'cat-btn cat-' + (c.id || 'all'), 'aria-pressed': String(state.category === c.id),
        onclick: function () { state.category = c.id; render(); } }, [
        EFI.el('span', { 'class': 'cat-icon', 'aria-hidden': 'true', text: c.icon }),
        EFI.el('span', { 'class': 'cat-label', text: c.label }),
        EFI.el('span', { 'class': 'badge', text: String(c.count) })
      ]);
      list.appendChild(EFI.el('li', null, [btn]));
    });
    $('#st-total').textContent = String(st.total);
    $('#st-fav').textContent = String(st.favorites);
    var bytes = 0;
    try { bytes = (window.localStorage.getItem(KEY) || '').length * 2; } catch (e) { bytes = 0; }
    $('#st-storage').textContent = 'Espace utilisé : ' + EFI.fmtBytes(bytes) + ' dans le localStorage de ce navigateur.';
  }

  function render() {
    renderCategories();
    var grid = $('#notes-grid');
    while (grid.firstChild) { grid.removeChild(grid.firstChild); }
    var visible = C.sort(C.filter(notes, state), state.sort);
    var now = Date.now();
    visible.forEach(function (n, i) { grid.appendChild(card(n, now, i)); });

    var empty = $('#empty-state');
    empty.hidden = visible.length > 0;
    $('#empty-text').textContent = notes.length === 0 ? 'Aucune note pour l\u2019instant.' : 'Aucune note ne correspond à vos critères.';
    $('#empty-new').hidden = notes.length > 0;
    $('#result-line').textContent = notes.length === 0 ? '' :
      visible.length + ' note(s) affichée(s) sur ' + notes.length + (state.query ? ' pour « ' + state.query + ' »' : '');
  }

  function card(n, now, i) {
    var cat = C.categoryById(n.category) || C.CATEGORIES[0];
    var star = EFI.el('button', { type: 'button', 'class': 'star-btn' + (n.favorite ? ' on' : ''), 'aria-pressed': String(n.favorite),
      'aria-label': n.favorite ? 'Retirer des favorites' : 'Marquer comme favorite', title: 'Favorite',
      onclick: function (e) { e.stopPropagation(); toggleFav(n.id); } }, [n.favorite ? '★' : '☆']);
    var edit = EFI.el('button', { type: 'button', 'class': 'icon-btn mini', 'aria-label': 'Modifier « ' + n.title + ' »', title: 'Modifier',
      onclick: function (e) { e.stopPropagation(); openForm(n.id); } }, [EFI.icon('M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z')]);
    var del = EFI.el('button', { type: 'button', 'class': 'icon-btn mini danger', 'aria-label': 'Supprimer « ' + n.title + ' »', title: 'Supprimer',
      onclick: function (e) { e.stopPropagation(); remove(n.id); } }, [EFI.icon('M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6')]);
    var art = EFI.el('article', { 'class': 'note-card cat-' + cat.id + (n.favorite ? ' is-fav' : ''), tabindex: '0',
      'aria-label': n.title, style: 'animation-delay:' + Math.min(i * 40, 400) + 'ms',
      ondblclick: function () { openForm(n.id); },
      onkeydown: function (e) { if (e.key === 'Enter' && e.target === art) { openForm(n.id); } } }, [
      EFI.el('div', { 'class': 'note-top' }, [EFI.el('span', { 'class': 'note-cat', text: cat.icon + ' ' + cat.label }), star]),
      EFI.el('h3', { 'class': 'note-title', text: n.title }),
      EFI.el('p', { 'class': 'note-content', text: n.content || '(note vide)' }),
      EFI.el('div', { 'class': 'note-foot' }, [
        EFI.el('span', { 'class': 'note-date', title: 'Créée le ' + EFI.fmtDateTime(n.createdAt), text: 'Modifiée ' + C.relative(n.updatedAt, now) }),
        EFI.el('span', { 'class': 'spacer' }), edit, del
      ])
    ]);
    return art;
  }

  /* ---------- Actions ---------- */
  function find(id) { for (var i = 0; i < notes.length; i++) { if (notes[i].id === id) { return i; } } return -1; }

  function toggleFav(id) {
    var i = find(id); if (i < 0) { return; }
    var n = notes[i];
    notes[i] = C.update(n, { title: n.title, content: n.content, category: n.category, favorite: !n.favorite }, n.updatedAt);
    save(); render();
  }

  function remove(id) {
    var i = find(id); if (i < 0) { return; }
    if (!window.confirm('Supprimer définitivement la note « ' + notes[i].title + ' » ?')) { return; }
    notes.splice(i, 1);
    save(); render(); closeForm();
    EFI.toast('Note supprimée.', 'info');
  }

  var dialog = $('#note-dialog'), form = $('#note-form');
  function fillCategories() {
    var sel = $('#note-category');
    C.CATEGORIES.forEach(function (c) { sel.appendChild(EFI.el('option', { value: c.id, text: c.icon + ' ' + c.label })); });
  }
  function clearErrors() { ['title', 'content', 'category'].forEach(function (k) { $('#err-' + k).textContent = ''; }); $('#note-title').removeAttribute('aria-invalid'); }

  function openForm(id) {
    clearErrors();
    editingId = id || null;
    var n = id ? notes[find(id)] : null;
    $('#note-dialog-title').textContent = n ? 'Modifier la note' : 'Nouvelle note';
    $('#note-title').value = n ? n.title : '';
    $('#note-content').value = n ? n.content : '';
    $('#note-category').value = n ? n.category : (state.category || 'formation');
    $('#note-favorite').checked = n ? n.favorite : false;
    $('#note-delete').hidden = !n;
    $('#note-submit').textContent = n ? 'Enregistrer les modifications' : 'Créer la note';
    updateCount();
    if (dialog.showModal) { dialog.showModal(); } else { dialog.setAttribute('open', ''); }
    $('#note-title').focus();
  }
  function closeForm() { if (dialog.open) { dialog.close(); } else { dialog.removeAttribute('open'); } }
  function updateCount() { $('#content-count').textContent = $('#note-content').value.length + ' / ' + C.LIMITS.content; }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    clearErrors();
    var input = { title: $('#note-title').value, content: $('#note-content').value, category: $('#note-category').value, favorite: $('#note-favorite').checked };
    var r = C.validate(input);
    if (!r.ok) {
      for (var k in r.errors) { if (Object.prototype.hasOwnProperty.call(r.errors, k)) { $('#err-' + k).textContent = r.errors[k]; } }
      if (r.errors.title) { $('#note-title').setAttribute('aria-invalid', 'true'); $('#note-title').focus(); }
      return;
    }
    var now = Date.now();
    if (editingId) {
      var i = find(editingId);
      if (i >= 0) { notes[i] = C.update(notes[i], input, now); }
      EFI.toast('Note mise à jour.', 'success');
    } else {
      if (notes.length >= C.LIMITS.notes) { EFI.toast('Limite de ' + C.LIMITS.notes + ' notes atteinte.', 'error'); return; }
      notes.push(C.create(input, now));
      EFI.toast('Note créée.', 'success');
    }
    save(); render(); closeForm();
  });
  $('#note-content').addEventListener('input', updateCount);
  $('#note-delete').addEventListener('click', function () { if (editingId) { remove(editingId); } });
  Array.prototype.forEach.call(document.querySelectorAll('[data-close]'), function (b) { b.addEventListener('click', closeForm); });
  $('#new-note-btn').addEventListener('click', function () { openForm(null); });
  $('#empty-new').addEventListener('click', function () { openForm(null); });

  /* ---------- Filtres ---------- */
  $('#search').addEventListener('input', EFI.debounce(function () { state.query = $('#search').value; render(); }, 150));
  $('#in-content').addEventListener('change', function () { state.inContent = this.checked; render(); });
  $('#fav-only').addEventListener('change', function () { state.favoritesOnly = this.checked; render(); });
  $('#sort').addEventListener('change', function () { state.sort = this.value; render(); });

  /* ---------- Export / import ---------- */
  $('#export-btn').addEventListener('click', function () {
    if (!notes.length) { EFI.toast('Aucune note à exporter.', 'info'); return; }
    var json = C.exportJson(notes, Date.now(), window.location.host);
    var blob = new Blob([json], { type: 'application/json;charset=utf-8' });
    var url = URL.createObjectURL(blob);
    var d = new Date();
    var a = EFI.el('a', { href: url, download: 'efi-notes-' + d.getFullYear() + EFI.pad(d.getMonth() + 1) + EFI.pad(d.getDate()) + '.json' });
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    EFI.toast(notes.length + ' note(s) exportée(s).', 'success');
  });

  $('#import-file').addEventListener('change', function () {
    var f = this.files && this.files[0];
    var input = this;
    if (!f) { return; }
    if (f.size > 2 * 1024 * 1024) { EFI.toast('Fichier trop volumineux (2 Mo maximum).', 'error'); input.value = ''; return; }
    var reader = new FileReader();
    reader.onload = function () {
      try {
        var r = C.parseImport(String(reader.result), Date.now());
        notes = C.merge(notes, r.notes);
        save(); render();
        EFI.toast(r.notes.length + ' note(s) importée(s)' + (r.skipped ? ', ' + r.skipped + ' ignorée(s)' : '') + '.', 'success');
      } catch (e) { EFI.toast(e.message, 'error'); }
      input.value = '';
    };
    reader.onerror = function () { EFI.toast('Lecture du fichier impossible.', 'error'); input.value = ''; };
    reader.readAsText(f, 'UTF-8');
  });

  $('#samples-btn').addEventListener('click', function () {
    notes = C.merge(notes, C.samples(Date.now()));
    save(); render();
    EFI.toast('Notes d\u2019exemple ajoutées.', 'success');
  });

  /* ---------- Raccourcis clavier ---------- */
  document.addEventListener('keydown', function (e) {
    var tag = e.target && e.target.tagName;
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || (dialog && dialog.open)) { return; }
    if (e.key === '/') { e.preventDefault(); $('#search').focus(); }
    else if (e.key === 'n' || e.key === 'N') { e.preventDefault(); openForm(null); }
  });

  /* Synchronisation entre onglets du même navigateur */
  window.addEventListener('storage', function (e) { if (e.key === KEY) { load(); render(); } });

  fillCategories();
  load();
  try { window.localStorage.setItem('efi.notes.probe', '1'); window.localStorage.removeItem('efi.notes.probe'); }
  catch (e) { storageOk = false; }
  $('#storage-error').hidden = storageOk;
  render();
})();
