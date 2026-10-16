/*
 * EFI Notes — logique métier pure (aucune dépendance au DOM).
 * Syntaxe ES5 : testable hors navigateur avec Nashorn (jjs, JDK 8) — voir tests/js.
 */
(function (root) {
  'use strict';

  var LIMITS = { title: 120, content: 5000, notes: 500 };

  var CATEGORIES = [
    { id: 'formation', label: 'Formation', icon: '🎓' },
    { id: 'websphere', label: 'WebSphere', icon: '🧩' },
    { id: 'commandes', label: 'Commandes', icon: '⌨️' },
    { id: 'depannage', label: 'Dépannage', icon: '🛠️' },
    { id: 'personnel', label: 'Personnel', icon: '📌' }
  ];

  function categoryById(id) {
    for (var i = 0; i < CATEGORIES.length; i++) { if (CATEGORIES[i].id === id) { return CATEGORIES[i]; } }
    return null;
  }

  function trim(s) { return String(s === undefined || s === null ? '' : s).replace(/^\s+|\s+$/g, ''); }

  /** Supprime les accents et met en minuscules (String.normalize n'existe pas en ES5). */
  var ACCENTS = { 'à': 'a', 'â': 'a', 'ä': 'a', 'á': 'a', 'ã': 'a', 'ç': 'c', 'é': 'e', 'è': 'e', 'ê': 'e', 'ë': 'e',
    'î': 'i', 'ï': 'i', 'í': 'i', 'ô': 'o', 'ö': 'o', 'ó': 'o', 'õ': 'o', 'ù': 'u', 'û': 'u', 'ü': 'u', 'ú': 'u',
    'ÿ': 'y', 'ñ': 'n', 'œ': 'oe', 'æ': 'ae' };
  function fold(s) {
    return String(s || '').toLowerCase().replace(/[àâäáãçéèêëîïíôöóõùûüúÿñœæ]/g, function (c) { return ACCENTS[c]; });
  }

  /** Valide une saisie ; retourne { ok, errors, value }. */
  function validate(input) {
    input = input || {};
    var v = {
      title: trim(input.title),
      content: trim(input.content),
      category: trim(input.category) || 'formation',
      favorite: input.favorite === true || input.favorite === 'true'
    };
    var errors = {};
    if (v.title.length === 0) { errors.title = 'Le titre est obligatoire.'; }
    else if (v.title.length > LIMITS.title) { errors.title = 'Le titre ne doit pas dépasser ' + LIMITS.title + ' caractères.'; }
    if (v.content.length > LIMITS.content) { errors.content = 'Le contenu ne doit pas dépasser ' + LIMITS.content + ' caractères.'; }
    if (!categoryById(v.category)) { errors.category = 'Catégorie inconnue.'; }
    var ok = true;
    for (var k in errors) { if (Object.prototype.hasOwnProperty.call(errors, k)) { ok = false; } }
    return { ok: ok, errors: errors, value: v };
  }

  function newId(now, rnd) {
    return 'n' + now.toString(36) + Math.floor((rnd === undefined ? Math.random() : rnd) * 1e9).toString(36);
  }

  function create(input, now, rnd) {
    var r = validate(input);
    if (!r.ok) { throw new Error(firstError(r.errors)); }
    return { id: newId(now, rnd), title: r.value.title, content: r.value.content, category: r.value.category,
      favorite: r.value.favorite, createdAt: now, updatedAt: now };
  }

  function update(note, input, now) {
    var r = validate(input);
    if (!r.ok) { throw new Error(firstError(r.errors)); }
    return { id: note.id, title: r.value.title, content: r.value.content, category: r.value.category,
      favorite: r.value.favorite, createdAt: note.createdAt, updatedAt: now };
  }

  function firstError(errors) { for (var k in errors) { if (Object.prototype.hasOwnProperty.call(errors, k)) { return errors[k]; } } return ''; }

  /** Filtre : { query, inContent, category, favoritesOnly }. Recherche insensible à la casse et aux accents. */
  function filter(notes, f) {
    f = f || {};
    var q = fold(trim(f.query));
    return notes.filter(function (n) {
      if (f.category && n.category !== f.category) { return false; }
      if (f.favoritesOnly && !n.favorite) { return false; }
      if (q) {
        var hay = fold(n.title) + (f.inContent ? ' ' + fold(n.content) : '');
        if (hay.indexOf(q) < 0) { return false; }
      }
      return true;
    });
  }

  /** Tri : 'recent' (favoris d'abord puis récents), 'title', 'created'. */
  function sort(notes, mode) {
    var copy = notes.slice();
    copy.sort(function (a, b) {
      if (mode === 'title') { return fold(a.title) < fold(b.title) ? -1 : (fold(a.title) > fold(b.title) ? 1 : 0); }
      if (mode === 'created') { return b.createdAt - a.createdAt; }
      if (a.favorite !== b.favorite) { return a.favorite ? -1 : 1; }
      return b.updatedAt - a.updatedAt;
    });
    return copy;
  }

  function stats(notes) {
    var s = { total: notes.length, favorites: 0, byCategory: {} };
    CATEGORIES.forEach(function (c) { s.byCategory[c.id] = 0; });
    notes.forEach(function (n) {
      if (n.favorite) { s.favorites++; }
      if (Object.prototype.hasOwnProperty.call(s.byCategory, n.category)) { s.byCategory[n.category]++; }
    });
    return s;
  }

  /** Document d'export JSON (format versionné). */
  function exportJson(notes, now, host) {
    return JSON.stringify({ application: 'EFI Notes', format: 1, exportedAt: new Date(now).toISOString(),
      source: host || '', count: notes.length, notes: notes }, null, 2);
  }

  /** Analyse un fichier importé ; retourne { notes, skipped } ou lève une Error. */
  function parseImport(text, now) {
    var data;
    try { data = JSON.parse(text); } catch (e) { throw new Error('Fichier JSON invalide.'); }
    var list = data && data.notes instanceof Array ? data.notes : (data instanceof Array ? data : null);
    if (!list) { throw new Error('Aucune liste de notes trouvée dans le fichier.'); }
    if (list.length > LIMITS.notes) { throw new Error('Trop de notes (maximum ' + LIMITS.notes + ').'); }
    var out = [], skipped = 0, seen = {};
    for (var i = 0; i < list.length; i++) {
      var n = list[i];
      if (!n || typeof n !== 'object') { skipped++; continue; }
      var r = validate(n);
      if (!r.ok) { skipped++; continue; }
      var id = typeof n.id === 'string' && /^[a-z0-9]{4,40}$/i.test(n.id) && !seen[n.id] ? n.id : newId(now + i);
      seen[id] = true;
      out.push({ id: id, title: r.value.title, content: r.value.content, category: r.value.category,
        favorite: r.value.favorite, createdAt: num(n.createdAt, now), updatedAt: num(n.updatedAt, now) });
    }
    return { notes: out, skipped: skipped };
  }

  function num(v, d) { return typeof v === 'number' && isFinite(v) && v > 0 ? v : d; }

  /** Fusionne deux listes (les notes importées remplacent celles de même id). */
  function merge(existing, imported) {
    var byId = {}, out = [];
    imported.forEach(function (n) { byId[n.id] = n; });
    existing.forEach(function (n) { if (!byId[n.id]) { out.push(n); } });
    return out.concat(imported).slice(0, LIMITS.notes);
  }

  /** « il y a 5 min », « hier », date courte… */
  function relative(ts, now) {
    var d = Math.max(0, now - ts) / 1000;
    if (d < 60) { return 'à l\u2019instant'; }
    if (d < 3600) { return 'il y a ' + Math.floor(d / 60) + ' min'; }
    if (d < 86400) { return 'il y a ' + Math.floor(d / 3600) + ' h'; }
    if (d < 172800) { return 'hier'; }
    if (d < 604800) { return 'il y a ' + Math.floor(d / 86400) + ' jours'; }
    var dt = new Date(ts);
    return pad(dt.getDate()) + '/' + pad(dt.getMonth() + 1) + '/' + dt.getFullYear();
  }
  function pad(n) { return (n < 10 ? '0' : '') + n; }

  function samples(now) {
    var base = [
      { title: 'Ports du laboratoire', category: 'websphere', favorite: true,
        content: 'DMGR vm-was-efi (172.16.3.20)\nSOAP : 8880\nConsole HTTPS : 9044\nCellule : vm-was-efiCell02' },
      { title: 'Synchroniser un nœud', category: 'commandes',
        content: 'Console : Administration système › Nœuds › nodesa1Node01 › Synchroniser\nwsadmin : AdminControl.invoke(sync, \'sync\')' },
      { title: 'Application non démarrée', category: 'depannage',
        content: 'Vérifier SystemOut.log du serveur test, l\u2019état du Node Agent et la synchronisation du nœud.' },
      { title: 'EAR ou WAR ?', category: 'formation',
        content: 'Un EAR regroupe un ou plusieurs modules (WAR, EJB…) et décrit le context root dans application.xml.' }
    ];
    return base.map(function (b, i) { return create(b, now - i * 3600000, i / 10); });
  }

  root.NotesCore = { LIMITS: LIMITS, CATEGORIES: CATEGORIES, categoryById: categoryById, fold: fold, validate: validate,
    create: create, update: update, filter: filter, sort: sort, stats: stats, exportJson: exportJson,
    parseImport: parseImport, merge: merge, relative: relative, samples: samples };
})(this);
