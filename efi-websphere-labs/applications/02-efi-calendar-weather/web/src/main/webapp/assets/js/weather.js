/*
 * EFI Weather — météo réelle de Paris (Open-Meteo).
 * 1) via le serveur WebSphere : GET /api/weather (proxy Java, cache 10 min)
 * 2) repli : appel direct du navigateur vers api.open-meteo.com (CORS autorisé)
 * 3) sinon : message d'erreur et bouton « Réessayer »
 */
(function () {
  'use strict';
  var $ = EFI.$;

  var DIRECT_URL = 'https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522' +
    '&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl' +
    '&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset' +
    '&timezone=Europe%2FParis&forecast_days=7&wind_speed_unit=kmh';
  var REFRESH_MS = 15 * 60 * 1000;

  /* Codes météo WMO → libellé français + icônes jour/nuit */
  var WMO = {
    0: ['Ciel dégagé', '☀️', '🌙'], 1: ['Principalement dégagé', '🌤️', '🌙'], 2: ['Partiellement nuageux', '⛅', '☁️'],
    3: ['Couvert', '☁️', '☁️'], 45: ['Brouillard', '🌫️', '🌫️'], 48: ['Brouillard givrant', '🌫️', '🌫️'],
    51: ['Bruine légère', '🌦️', '🌧️'], 53: ['Bruine modérée', '🌦️', '🌧️'], 55: ['Bruine dense', '🌧️', '🌧️'],
    56: ['Bruine verglaçante', '🌧️', '🌧️'], 57: ['Bruine verglaçante dense', '🌧️', '🌧️'],
    61: ['Pluie faible', '🌦️', '🌧️'], 63: ['Pluie modérée', '🌧️', '🌧️'], 65: ['Pluie forte', '🌧️', '🌧️'],
    66: ['Pluie verglaçante', '🌧️', '🌧️'], 67: ['Pluie verglaçante forte', '🌧️', '🌧️'],
    71: ['Neige faible', '🌨️', '🌨️'], 73: ['Neige modérée', '🌨️', '🌨️'], 75: ['Neige forte', '❄️', '❄️'],
    77: ['Grains de neige', '🌨️', '🌨️'], 80: ['Averses faibles', '🌦️', '🌧️'], 81: ['Averses modérées', '🌧️', '🌧️'],
    82: ['Averses violentes', '⛈️', '⛈️'], 85: ['Averses de neige', '🌨️', '🌨️'], 86: ['Fortes averses de neige', '❄️', '❄️'],
    95: ['Orage', '⛈️', '⛈️'], 96: ['Orage avec grêle', '⛈️', '⛈️'], 99: ['Orage violent avec grêle', '⛈️', '⛈️']
  };
  function wmo(code, isDay) {
    var w = WMO[code] || ['Conditions inconnues', '🌡️', '🌡️'];
    return { label: w[0], icon: isDay === 0 ? w[2] : w[1] };
  }
  function cardinal(deg) {
    var dirs = ['N', 'NE', 'E', 'SE', 'S', 'SO', 'O', 'NO'];
    return dirs[Math.round(((deg % 360) / 45)) % 8];
  }
  function round(v) { return (v === null || v === undefined || isNaN(v)) ? '--' : String(Math.round(v)); }
  function hhmm(iso) { return iso && iso.length >= 16 ? iso.substring(11, 16) : '--:--'; }

  var card = $('#weather-card');

  function setBadge(cell, text, cls) {
    cell.textContent = '';
    cell.appendChild(EFI.el('span', { 'class': 'badge ' + (cls || ''), text: text }));
  }

  function render(d, source, meta) {
    var cur = d.current || {};
    var w = wmo(cur.weather_code, cur.is_day);
    $('#wx-icon').textContent = w.icon;
    $('#wx-temp').textContent = (cur.temperature_2m !== undefined) ? String(cur.temperature_2m).replace('.', ',') : '--';
    $('#wx-cond').textContent = w.label;
    $('#wx-feel').textContent = round(cur.apparent_temperature);
    $('#wx-hum').textContent = round(cur.relative_humidity_2m);
    $('#wx-wind').textContent = round(cur.wind_speed_10m);
    $('#wx-dir').textContent = cur.wind_direction_10m !== undefined ? '(' + cardinal(cur.wind_direction_10m) + ')' : '';
    $('#wx-gust').textContent = round(cur.wind_gusts_10m);
    $('#wx-press').textContent = round(cur.pressure_msl);
    var daily = d.daily || { time: [] };
    $('#wx-sunrise').textContent = hhmm(daily.sunrise && daily.sunrise[0]);
    $('#wx-sunset').textContent = hhmm(daily.sunset && daily.sunset[0]);
    $('#wx-time').textContent = (cur.time ? cur.time.replace('T', ' à ') + ' (heure de Paris)' : EFI.fmtDateTime(new Date())) +
      (meta && meta.cached ? ' · cache serveur' : '');
    var src = $('#wx-source');
    src.textContent = source === 'server' ? 'Source : serveur WebSphere' : 'Source : navigateur';
    src.className = 'badge ' + (source === 'server' ? 'badge-success' : 'badge-warning');
    card.classList.add('wx-code-' + (cur.is_day === 0 ? 'night' : 'day'));

    /* Prévisions */
    var fcBox = $('#forecast');
    fcBox.textContent = '';
    var map = {};
    for (var i = 0; i < daily.time.length; i++) {
      var dw = wmo(daily.weather_code[i], 1);
      var rain = daily.precipitation_probability_max ? daily.precipitation_probability_max[i] : null;
      var item = { icon: dw.icon, label: dw.label, max: round(daily.temperature_2m_max[i]), min: round(daily.temperature_2m_min[i]), rain: rain };
      map[daily.time[i]] = item;
      var parts = daily.time[i].split('-');
      var dt = new Date(Date.UTC(+parts[0], +parts[1] - 1, +parts[2], 12));
      var dayName = i === 0 ? 'Aujourd\u2019hui' : dt.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', timeZone: 'UTC' });
      fcBox.appendChild(EFI.el('article', { 'class': 'fc-day' + (i === 0 ? ' fc-today' : ''), style: 'animation-delay:' + (i * 60) + 'ms' }, [
        EFI.el('div', { 'class': 'fc-name', text: dayName }),
        EFI.el('div', { 'class': 'fc-icon', 'aria-hidden': 'true', text: dw.icon }),
        EFI.el('div', { 'class': 'fc-label', text: dw.label }),
        EFI.el('div', { 'class': 'fc-temps' }, [
          EFI.el('span', { 'class': 'fc-max', text: item.max + '°' }),
          EFI.el('span', { 'class': 'fc-min', text: item.min + '°' })
        ]),
        EFI.el('div', { 'class': 'fc-rain', text: '💧 ' + (rain === null || rain === undefined ? 'n.d.' : rain + ' %') })
      ]));
    }
    var ev;
    try { ev = new CustomEvent('efi:forecast', { detail: map }); }
    catch (e) { ev = document.createEvent('CustomEvent'); ev.initCustomEvent('efi:forecast', false, false, map); }
    document.dispatchEvent(ev);

    $('#wx-loading').hidden = true;
    $('#wx-error').hidden = true;
    $('#wx-content').hidden = false;
    card.setAttribute('aria-busy', 'false');
  }

  function showError(message) {
    $('#wx-loading').hidden = true;
    $('#wx-content').hidden = true;
    $('#wx-error').hidden = false;
    $('#wx-error-msg').textContent = message;
    var src = $('#wx-source');
    src.textContent = 'Hors ligne';
    src.className = 'badge badge-danger';
    card.setAttribute('aria-busy', 'false');
    $('#forecast').textContent = '';
    $('#forecast').appendChild(EFI.el('div', { 'class': 'empty-state', text: 'Prévisions indisponibles.' }));
  }

  /* Appel direct du navigateur, sans en-tête personnalisé (évite le pré-vol CORS) */
  function direct() {
    return new Promise(function (resolve, reject) {
      var xhr = new XMLHttpRequest();
      var t0 = Date.now();
      xhr.open('GET', DIRECT_URL, true);
      xhr.timeout = 8000;
      xhr.onload = function () {
        if (xhr.status === 200) {
          try { resolve({ data: JSON.parse(xhr.responseText), ms: Date.now() - t0 }); } catch (e) { reject(new Error('Réponse JSON invalide')); }
        } else { reject(new Error('HTTP ' + xhr.status)); }
      };
      xhr.onerror = function () { reject(new Error('Accès Internet bloqué ou politique CSP / proxy du poste.')); };
      xhr.ontimeout = function () { reject(new Error('Délai dépassé (8 s).')); };
      xhr.send();
    });
  }

  function load() {
    $('#wx-loading').hidden = false;
    $('#wx-error').hidden = true;
    card.setAttribute('aria-busy', 'true');
    setBadge($('#diag-server-status'), 'en cours…', 'badge-info');
    var t0 = Date.now();
    EFI.ajax({ url: EFI.url('/api/weather'), timeout: 15000 }).then(function (res) {
      setBadge($('#diag-server-status'), 'OK', 'badge-success');
      $('#diag-server-detail').textContent = (res.cached ? 'Réponse depuis le cache serveur' : 'Appel réel en ' + res.durationMs + ' ms') +
        ' · nœud ' + res.hostname + ' · aller-retour navigateur ' + (Date.now() - t0) + ' ms';
      setBadge($('#diag-browser-status'), 'non utilisé', '');
      render(res.data, 'server', res);
    }, function (err) {
      var d = err.data || {};
      setBadge($('#diag-server-status'), 'Échec' + (d.category ? ' · ' + d.category : ''), 'badge-danger');
      $('#diag-server-detail').textContent = err.message + (err.status ? ' (HTTP ' + err.status + ')' : '');
      setBadge($('#diag-browser-status'), 'en cours…', 'badge-info');
      direct().then(function (r) {
        setBadge($('#diag-browser-status'), 'OK', 'badge-success');
        $('#diag-browser-detail').textContent = 'Appel direct réussi en ' + r.ms + ' ms (le serveur n\u2019a pas accès au service).';
        render(r.data, 'browser');
        EFI.toast('Météo obtenue directement par le navigateur (serveur sans accès).', 'info');
      }, function (e2) {
        setBadge($('#diag-browser-status'), 'Échec', 'badge-danger');
        $('#diag-browser-detail').textContent = e2.message;
        showError('Ni le serveur ni le navigateur n\u2019ont pu joindre api.open-meteo.com. Consultez le panneau de diagnostic.');
      });
    });
  }

  $('#wx-retry').addEventListener('click', load);
  load();
  setInterval(load, REFRESH_MS);
})();
