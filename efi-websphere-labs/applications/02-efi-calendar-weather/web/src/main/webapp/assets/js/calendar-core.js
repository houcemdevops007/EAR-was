/*
 * EFI Calendar — fonctions de calcul pures (sans DOM), testables hors navigateur.
 * Mois indexés de 0 (janvier) à 11 (décembre), semaine commençant le lundi.
 */
(function (root) {
  'use strict';

  var MONTHS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
  var DAYS = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];

  function pad2(n) { return (n < 10 ? '0' : '') + n; }

  function key(y, m, d) { return y + '-' + pad2(m + 1) + '-' + pad2(d); }

  function utc(y, m, d) { return Date.UTC(y, m, d); }

  function addDays(y, m, d, n) {
    var t = new Date(utc(y, m, d) + n * 86400000);
    return { y: t.getUTCFullYear(), m: t.getUTCMonth(), d: t.getUTCDate() };
  }

  function daysInMonth(y, m) { return new Date(Date.UTC(y, m + 1, 0)).getUTCDate(); }

  /** Jour de la semaine (0 = lundi ... 6 = dimanche). */
  function weekdayMon(y, m, d) { return (new Date(utc(y, m, d)).getUTCDay() + 6) % 7; }

  /** Date de Pâques (algorithme de Meeus/Jones/Butcher, calendrier grégorien). */
  function easter(y) {
    var a = y % 19, b = Math.floor(y / 100), c = y % 100, d = Math.floor(b / 4), e = b % 4;
    var f = Math.floor((b + 8) / 25), g = Math.floor((b - f + 1) / 3);
    var h = (19 * a + b - d - g + 15) % 30, i = Math.floor(c / 4), k = c % 4;
    var l = (32 + 2 * e + 2 * i - h - k) % 7, mm = Math.floor((a + 11 * h + 22 * l) / 451);
    var month = Math.floor((h + l - 7 * mm + 114) / 31) - 1;
    var day = ((h + l - 7 * mm + 114) % 31) + 1;
    return { y: y, m: month, d: day };
  }

  var holidayCache = {};
  /** Jours fériés de France métropolitaine : { 'AAAA-MM-JJ': 'nom' }. */
  function holidays(y) {
    if (holidayCache[y]) { return holidayCache[y]; }
    var h = {};
    h[key(y, 0, 1)] = 'Jour de l\u2019An';
    h[key(y, 4, 1)] = 'Fête du Travail';
    h[key(y, 4, 8)] = 'Victoire 1945';
    h[key(y, 6, 14)] = 'Fête nationale';
    h[key(y, 7, 15)] = 'Assomption';
    h[key(y, 10, 1)] = 'Toussaint';
    h[key(y, 10, 11)] = 'Armistice 1918';
    h[key(y, 11, 25)] = 'Noël';
    var e = easter(y);
    var lp = addDays(e.y, e.m, e.d, 1), asc = addDays(e.y, e.m, e.d, 39), pent = addDays(e.y, e.m, e.d, 50);
    h[key(lp.y, lp.m, lp.d)] = 'Lundi de Pâques';
    h[key(asc.y, asc.m, asc.d)] = 'Ascension';
    h[key(pent.y, pent.m, pent.d)] = 'Lundi de Pentecôte';
    holidayCache[y] = h;
    return h;
  }

  /** Numéro de semaine ISO-8601. */
  function isoWeek(y, m, d) {
    var date = new Date(utc(y, m, d));
    var day = (date.getUTCDay() + 6) % 7;
    date.setUTCDate(date.getUTCDate() - day + 3);
    var firstThursday = new Date(Date.UTC(date.getUTCFullYear(), 0, 4));
    var diff = (date - firstThursday) / 86400000;
    return 1 + Math.round((diff - 3 + ((firstThursday.getUTCDay() + 6) % 7)) / 7);
  }

  function dayOfYear(y, m, d) { return Math.round((utc(y, m, d) - utc(y, 0, 1)) / 86400000) + 1; }

  function isLeap(y) { return (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0; }

  function diffDays(a, b) { return Math.round((utc(b.y, b.m, b.d) - utc(a.y, a.m, a.d)) / 86400000); }

  /** Grille du mois : 6 semaines de 7 jours (lundi → dimanche). */
  function monthMatrix(y, m) {
    var offset = weekdayMon(y, m, 1);
    var start = addDays(y, m, 1, -offset);
    var weeks = [];
    for (var w = 0; w < 6; w++) {
      var days = [];
      for (var i = 0; i < 7; i++) {
        var c = addDays(start.y, start.m, start.d, w * 7 + i);
        days.push({ y: c.y, m: c.m, d: c.d, inMonth: c.m === m, weekend: i >= 5, key: key(c.y, c.m, c.d) });
      }
      weeks.push({ week: isoWeek(days[0].y, days[0].m, days[0].d), days: days });
    }
    return weeks;
  }

  /** Date du jour dans le fuseau Europe/Paris (indépendamment du fuseau du poste). */
  function todayParis(now) {
    now = now || new Date();
    try {
      var parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Paris', year: 'numeric', month: '2-digit', day: '2-digit' }).format(now).split('-');
      return { y: parseInt(parts[0], 10), m: parseInt(parts[1], 10) - 1, d: parseInt(parts[2], 10) };
    } catch (e) {
      return { y: now.getFullYear(), m: now.getMonth(), d: now.getDate() };
    }
  }

  root.EFICalendarCore = {
    MONTHS: MONTHS, DAYS: DAYS, pad2: pad2, key: key, addDays: addDays, daysInMonth: daysInMonth,
    weekdayMon: weekdayMon, easter: easter, holidays: holidays, isoWeek: isoWeek, dayOfYear: dayOfYear,
    isLeap: isLeap, diffDays: diffDays, monthMatrix: monthMatrix, todayParis: todayParis
  };
})(typeof window !== 'undefined' ? window : this);
