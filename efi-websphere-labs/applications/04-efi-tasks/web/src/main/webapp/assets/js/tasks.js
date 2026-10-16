/* EFI Tasks — client Kanban : CRUD via l'API REST, glisser-déposer, filtres */
(function () {
  'use strict';
  var $ = EFI.$;
  var STATUS = ['todo', 'doing', 'done'];
  var STATUS_LABEL = { todo: 'À faire', doing: 'En cours', done: 'Terminé' };
  var PRIO_LABEL = { basse: 'Basse', moyenne: 'Moyenne', haute: 'Haute' };
  var tasks = [];
  var today = '';
  var dialog = $('#task-dialog');
  var form = $('#task-form');

  function api(method, path, data) {
    return EFI.ajax({ method: method, url: EFI.url('/api/tasks' + (path || '')), data: data });
  }

  function load() {
    return api('GET').then(function (d) {
      tasks = d.tasks;
      today = d.today;
      renderStats(d.stats);
      render();
    }, function (e) { EFI.toast('Chargement impossible : ' + e.message, 'error'); });
  }

  function renderStats(s) {
    $('#st-total').textContent = s.total;
    $('#st-todo').textContent = s.todo;
    $('#st-doing').textContent = s.doing;
    $('#st-overdue').textContent = s.overdue;
    $('#st-high').textContent = s.high;
    var pct = s.total ? Math.round(s.done * 100 / s.total) : 0;
    $('#st-pct').textContent = pct;
    $('#st-progress').style.width = pct + '%';
  }

  function daysUntil(iso) {
    if (!iso || !today) { return null; }
    var a = Date.UTC(+today.substr(0, 4), +today.substr(5, 2) - 1, +today.substr(8, 2));
    var b = Date.UTC(+iso.substr(0, 4), +iso.substr(5, 2) - 1, +iso.substr(8, 2));
    return Math.round((b - a) / 86400000);
  }

  function fmtDate(iso) {
    var p = iso.split('-');
    return p[2] + '/' + p[1] + '/' + p[0];
  }

  function matches(t) {
    var q = $('#search').value.trim().toLowerCase();
    var prio = $('#filter-priority').value;
    if (q && (t.title + ' ' + t.description).toLowerCase().indexOf(q) < 0) { return false; }
    if (prio && t.priority !== prio) { return false; }
    if ($('#filter-overdue').checked && !t.overdue) { return false; }
    return true;
  }

  function card(t) {
    var idx = STATUS.indexOf(t.status);
    var due = null;
    if (t.dueDate) {
      var n = daysUntil(t.dueDate);
      var cls = t.overdue ? 'due overdue' : (t.status !== 'done' && n !== null && n <= 2 ? 'due soon' : 'due');
      var txt = t.overdue ? 'En retard · ' + fmtDate(t.dueDate) : (n === 0 && t.status !== 'done' ? 'Aujourd\u2019hui' : fmtDate(t.dueDate));
      due = EFI.el('span', { 'class': cls, title: 'Échéance' }, ['📅 ', txt]);
    }
    var left = EFI.el('button', { type: 'button', 'class': 'icon-btn mini', 'aria-label': 'Déplacer vers ' + (STATUS_LABEL[STATUS[idx - 1]] || ''), disabled: idx === 0 }, ['←']);
    var right = EFI.el('button', { type: 'button', 'class': 'icon-btn mini', 'aria-label': 'Déplacer vers ' + (STATUS_LABEL[STATUS[idx + 1]] || ''), disabled: idx === 2 }, ['→']);
    left.addEventListener('click', function (e) { e.stopPropagation(); move(t, STATUS[idx - 1]); });
    right.addEventListener('click', function (e) { e.stopPropagation(); move(t, STATUS[idx + 1]); });
    var edit = EFI.el('button', { type: 'button', 'class': 'icon-btn mini', 'aria-label': 'Modifier « ' + t.title + ' »' }, ['✎']);
    edit.addEventListener('click', function (e) { e.stopPropagation(); openForm(t); });

    var el = EFI.el('article', {
      'class': 'task prio-' + t.priority + (t.overdue ? ' is-overdue' : '') + (t.status === 'done' ? ' is-done' : ''),
      draggable: 'true', tabindex: '0', 'aria-label': t.title + ', priorité ' + PRIO_LABEL[t.priority], dataset: { id: String(t.id) }
    }, [
      EFI.el('div', { 'class': 'task-top' }, [
        EFI.el('span', { 'class': 'prio-badge', text: PRIO_LABEL[t.priority] }),
        EFI.el('span', { 'class': 'task-id', text: '#' + t.id })
      ]),
      EFI.el('h3', { 'class': 'task-title', text: t.title }),
      t.description ? EFI.el('p', { 'class': 'task-desc', text: t.description }) : null,
      EFI.el('div', { 'class': 'task-foot' }, [due || EFI.el('span', { 'class': 'due muted', text: 'Sans échéance' }), EFI.el('span', { 'class': 'spacer' }), left, edit, right])
    ]);
    el.addEventListener('dblclick', function () { openForm(t); });
    el.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') { openForm(t); }
      if (e.key === 'ArrowRight' && idx < 2) { move(t, STATUS[idx + 1]); }
      if (e.key === 'ArrowLeft' && idx > 0) { move(t, STATUS[idx - 1]); }
      if (e.key === 'Delete') { remove(t); }
    });
    el.addEventListener('dragstart', function (e) {
      e.dataTransfer.setData('text/plain', String(t.id));
      e.dataTransfer.effectAllowed = 'move';
      el.classList.add('dragging');
    });
    el.addEventListener('dragend', function () { el.classList.remove('dragging'); });
    return el;
  }

  function render() {
    STATUS.forEach(function (s) {
      var col = $('[data-drop="' + s + '"]');
      col.textContent = '';
      var list = tasks.filter(function (t) { return t.status === s && matches(t); });
      $('[data-count="' + s + '"]').textContent = String(list.length);
      if (!list.length) {
        col.appendChild(EFI.el('div', { 'class': 'col-empty', text: 'Aucune tâche' }));
      }
      list.forEach(function (t) { col.appendChild(card(t)); });
    });
  }

  function move(t, status) {
    if (!status || status === t.status) { return; }
    api('PUT', '/' + t.id + '/status', { status: status }).then(function () {
      EFI.toast('« ' + t.title + ' » → ' + STATUS_LABEL[status], 'success');
      load();
    }, function (e) { EFI.toast(e.message, 'error'); load(); });
  }

  function remove(t) {
    if (!window.confirm('Supprimer la tâche « ' + t.title + ' » ?')) { return; }
    api('DELETE', '/' + t.id).then(function () {
      EFI.toast('Tâche supprimée.', 'success');
      closeForm();
      load();
    }, function (e) { EFI.toast(e.message, 'error'); });
  }

  /* --- Glisser-déposer --- */
  EFI.$$('.col-body').forEach(function (col) {
    col.addEventListener('dragover', function (e) { e.preventDefault(); col.classList.add('drag-over'); });
    col.addEventListener('dragleave', function () { col.classList.remove('drag-over'); });
    col.addEventListener('drop', function (e) {
      e.preventDefault();
      col.classList.remove('drag-over');
      var id = parseInt(e.dataTransfer.getData('text/plain'), 10);
      var t = tasks.filter(function (x) { return x.id === id; })[0];
      if (t) { move(t, col.getAttribute('data-drop')); }
    });
  });

  /* --- Formulaire (création / modification) --- */
  function clearErrors() {
    EFI.$$('.error-text', form).forEach(function (e) { e.textContent = ''; });
    EFI.$$('[aria-invalid]', form).forEach(function (e) { e.removeAttribute('aria-invalid'); });
  }
  function openForm(t) {
    clearErrors();
    $('#task-id').value = t ? t.id : '';
    $('#task-title').value = t ? t.title : '';
    $('#task-desc').value = t ? t.description : '';
    $('#task-priority').value = t ? t.priority : 'moyenne';
    $('#task-status').value = t ? t.status : 'todo';
    $('#task-due').value = t && t.dueDate ? t.dueDate : '';
    $('#task-dialog-title').textContent = t ? 'Modifier la tâche #' + t.id : 'Nouvelle tâche';
    $('#task-submit').textContent = t ? 'Enregistrer' : 'Créer la tâche';
    $('#task-delete').hidden = !t;
    if (dialog.showModal) { dialog.showModal(); } else { dialog.setAttribute('open', ''); }
    $('#task-title').focus();
  }
  function closeForm() { if (dialog.open) { dialog.close(); } }

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    clearErrors();
    var title = $('#task-title').value.trim();
    if (title.length < 3) {
      $('#err-title').textContent = 'Le titre doit contenir au moins 3 caractères.';
      $('#task-title').setAttribute('aria-invalid', 'true');
      $('#task-title').focus();
      return;
    }
    var id = $('#task-id').value;
    var data = {
      title: title, description: $('#task-desc').value, priority: $('#task-priority').value,
      status: $('#task-status').value, dueDate: $('#task-due').value
    };
    var req = id ? api('PUT', '/' + id, data) : api('POST', '', data);
    req.then(function () {
      EFI.toast(id ? 'Tâche mise à jour.' : 'Tâche créée.', 'success');
      closeForm();
      load();
    }, function (err) {
      if (err.data && err.data.fields) {
        for (var f in err.data.fields) {
          if (err.data.fields.hasOwnProperty(f) && $('#err-' + f)) {
            $('#err-' + f).textContent = err.data.fields[f];
          }
        }
      }
      EFI.toast(err.message, 'error');
    });
  });
  $('#task-delete').addEventListener('click', function () {
    var id = parseInt($('#task-id').value, 10);
    var t = tasks.filter(function (x) { return x.id === id; })[0];
    if (t) { remove(t); }
  });
  EFI.$$('[data-close]', dialog).forEach(function (b) { b.addEventListener('click', closeForm); });
  $('#new-task-btn').addEventListener('click', function () { openForm(null); });

  $('#search').addEventListener('input', EFI.debounce(render, 150));
  $('#filter-priority').addEventListener('change', render);
  $('#filter-overdue').addEventListener('change', render);
  $('#reset-btn').addEventListener('click', function () {
    if (!window.confirm('Remplacer toutes les tâches par les données de démonstration ?')) { return; }
    api('POST', '/reset', {}).then(function () { EFI.toast('Données de démonstration restaurées.', 'success'); load(); },
      function (e) { EFI.toast(e.message, 'error'); });
  });
  document.addEventListener('keydown', function (e) {
    if (e.key === 'n' && !dialog.open && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
      openForm(null);
    }
  });

  load();
  setInterval(function () { if (!dialog.open && !document.hidden) { load(); } }, 20000);
})();
