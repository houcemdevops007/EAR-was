// Tests de applications/02-efi-calendar-weather/.../calendar-core.js
load(ROOT + '/applications/02-efi-calendar-weather/web/src/main/webapp/assets/js/calendar-core.js');
var K = EFICalendarCore;

test('Pâques 2024, 2025, 2026', function () {
  eq(K.easter(2024), { y: 2024, m: 2, d: 31 });
  eq(K.easter(2025), { y: 2025, m: 3, d: 20 });
  eq(K.easter(2026), { y: 2026, m: 3, d: 5 });
});
test('Jours fériés 2026 (11 jours, lundi de Pâques, Ascension, Pentecôte)', function () {
  var h = K.holidays(2026);
  eq(Object.keys(h).length, 11);
  eq(h['2026-04-06'], 'Lundi de Pâques');
  eq(h['2026-05-14'], 'Ascension');
  eq(h['2026-05-25'], 'Lundi de Pentecôte');
  eq(h['2026-07-14'], 'Fête nationale');
});
test('Années bissextiles et jours par mois', function () {
  ok(K.isLeap(2024)); ok(!K.isLeap(2026)); ok(K.isLeap(2000)); ok(!K.isLeap(1900));
  eq(K.daysInMonth(2024, 1), 29); eq(K.daysInMonth(2026, 1), 28); eq(K.daysInMonth(2026, 9), 31);
});
test('Semaine ISO (cas limites de début/fin d\u2019année)', function () {
  eq(K.isoWeek(2026, 0, 1), 1);     // jeudi 1er janvier 2026
  eq(K.isoWeek(2027, 0, 1), 53);    // vendredi 1er janvier 2027 → semaine 53 de 2026
  eq(K.isoWeek(2024, 11, 30), 1);   // lundi 30 décembre 2024 → semaine 1 de 2025
  eq(K.isoWeek(2026, 9, 15), 42);
});
test('Jour de la semaine (lundi = 0)', function () {
  eq(K.weekdayMon(2026, 9, 15), 3); // jeudi
  eq(K.weekdayMon(2026, 9, 12), 0); // lundi
});
test('Grille mensuelle : 6 semaines de 7 jours commençant un lundi', function () {
  var g = K.monthMatrix(2026, 9);
  eq(g.length, 6);
  g.forEach(function (w) { eq(w.days.length, 7); });
  eq(g[0].days[0].key, '2026-09-28');
  eq(g[0].days[3].key, '2026-10-01');
  ok(g[0].days[3].inMonth && !g[0].days[0].inMonth);
  ok(g[0].days[5].weekend && g[0].days[6].weekend);
});
test('Ajout de jours et écart entre dates', function () {
  eq(K.addDays(2026, 11, 31, 1), { y: 2027, m: 0, d: 1 });
  eq(K.diffDays({ y: 2026, m: 0, d: 1 }, { y: 2026, m: 11, d: 31 }), 364);
  eq(K.dayOfYear(2026, 9, 15), 288);
});
done('calendar-core');
