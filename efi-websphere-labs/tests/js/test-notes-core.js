// Tests de applications/07-efi-notes/.../notes-core.js
load(ROOT + '/applications/07-efi-notes/web/src/main/webapp/assets/js/notes-core.js');
var N = NotesCore;
var T0 = 1760000000000;

test('Validation : titre obligatoire, longueur et catégorie', function () {
  ok(!N.validate({ title: '   ' }).ok);
  eq(N.validate({ title: '' }).errors.title, 'Le titre est obligatoire.');
  ok(!N.validate({ title: new Array(122).join('x') }).ok);
  ok(!N.validate({ title: 'A', category: 'inconnue' }).ok);
  var r = N.validate({ title: '  Ports  ', content: ' x ', category: 'websphere', favorite: 'true' });
  ok(r.ok); eq(r.value.title, 'Ports'); eq(r.value.content, 'x'); eq(r.value.favorite, true);
});
test('Création et modification conservent createdAt', function () {
  var n = N.create({ title: 'Note', category: 'formation' }, T0, 0.5);
  ok(/^n[a-z0-9]+$/.test(n.id)); eq(n.createdAt, T0); eq(n.updatedAt, T0);
  var u = N.update(n, { title: 'Note 2', category: 'commandes', favorite: true }, T0 + 1000);
  eq(u.id, n.id); eq(u.createdAt, T0); eq(u.updatedAt, T0 + 1000); eq(u.category, 'commandes'); ok(u.favorite);
  throws(function () { N.create({ title: '' }, T0); }, /obligatoire/);
});
test('Recherche par titre insensible à la casse et aux accents', function () {
  var list = [N.create({ title: 'Dépannage SSL', content: 'keystore' }, T0, 0.1),
    N.create({ title: 'Déploiement EAR', content: 'wsadmin' }, T0, 0.2)];
  eq(N.filter(list, { query: 'depannage' }).length, 1);
  eq(N.filter(list, { query: 'DÉPLOIEMENT' }).length, 1);
  eq(N.filter(list, { query: 'wsadmin' }).length, 0);
  eq(N.filter(list, { query: 'wsadmin', inContent: true }).length, 1);
});
test('Filtres catégorie et favoris', function () {
  var a = N.create({ title: 'A', category: 'websphere', favorite: true }, T0, 0.1);
  var b = N.create({ title: 'B', category: 'personnel' }, T0, 0.2);
  eq(N.filter([a, b], { category: 'personnel' })[0].title, 'B');
  eq(N.filter([a, b], { favoritesOnly: true })[0].title, 'A');
});
test('Tri : favoris d\u2019abord, puis récents ; tri par titre', function () {
  var a = N.create({ title: 'b-ancienne' }, T0, 0.1);
  var b = N.create({ title: 'a-récente' }, T0 + 5000, 0.2);
  var c = N.create({ title: 'c-favorite', favorite: true }, T0 - 5000, 0.3);
  eq(N.sort([a, b, c], 'recent').map(function (n) { return n.title; }), ['c-favorite', 'a-récente', 'b-ancienne']);
  eq(N.sort([a, b, c], 'title').map(function (n) { return n.title; }), ['a-récente', 'b-ancienne', 'c-favorite']);
});
test('Statistiques par catégorie', function () {
  var s = N.stats(N.samples(T0));
  eq(s.total, 4); eq(s.favorites, 1); eq(s.byCategory.websphere, 1); eq(s.byCategory.commandes, 1);
});
test('Export JSON puis import (aller-retour)', function () {
  var list = N.samples(T0);
  var json = N.exportJson(list, T0, 'nodesa1:9080');
  var doc = JSON.parse(json);
  eq(doc.application, 'EFI Notes'); eq(doc.count, 4); eq(doc.source, 'nodesa1:9080');
  var back = N.parseImport(json, T0);
  eq(back.notes.length, 4); eq(back.skipped, 0); eq(back.notes[0].title, list[0].title);
});
test('Import : rejet des fichiers invalides et des entrées incorrectes', function () {
  throws(function () { N.parseImport('pas du json', T0); }, /invalide/);
  throws(function () { N.parseImport('{"x":1}', T0); }, /Aucune liste/);
  var r = N.parseImport('[{"title":"ok"},{"title":""},42,{"title":"<b>x</b>","category":"hack"}]', T0);
  eq(r.notes.length, 1); eq(r.skipped, 3);
});
test('Fusion : les notes importées remplacent celles de même id', function () {
  var a = N.create({ title: 'A' }, T0, 0.1);
  var a2 = N.update(a, { title: 'A modifiée' }, T0 + 1);
  var b = N.create({ title: 'B' }, T0, 0.2);
  var m = N.merge([a], [a2, b]);
  eq(m.length, 2); eq(m[0].title, 'A modifiée');
});
test('Dates relatives en français', function () {
  eq(N.relative(T0, T0 + 10000), 'à l\u2019instant');
  eq(N.relative(T0, T0 + 5 * 60000), 'il y a 5 min');
  eq(N.relative(T0, T0 + 3 * 3600000), 'il y a 3 h');
  eq(N.relative(T0, T0 + 30 * 3600000), 'hier');
  eq(N.relative(T0, T0 + 3 * 86400000), 'il y a 3 jours');
});
done('notes-core');
