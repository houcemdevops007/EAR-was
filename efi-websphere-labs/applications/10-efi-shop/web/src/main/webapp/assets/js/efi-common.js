/*
 * EFI Academy — WebSphere Application Lab
 * Bibliothèque JavaScript commune (sans dépendance, syntaxe ES5).
 * Fournit : sélecteurs, requêtes AJAX avec jeton CSRF, toasts, construction
 * DOM sécurisée (textContent, jamais innerHTML pour les données utilisateur).
 */
(function (window, document) {
  'use strict';

  function meta(name) {
    var m = document.querySelector('meta[name="' + name + '"]');
    return m ? m.getAttribute('content') : '';
  }

  var EFI = {
    ctx: meta('context-path'),
    csrf: meta('csrf-token'),

    $: function (sel, root) { return (root || document).querySelector(sel); },
    $$: function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); },

    /** URL relative à la racine de contexte de l'application. */
    url: function (path) { return EFI.ctx + path; },

    /** Encode un objet en application/x-www-form-urlencoded. */
    form: function (obj) {
      var parts = [];
      for (var k in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, k) && obj[k] !== undefined && obj[k] !== null) {
          parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(obj[k]));
        }
      }
      return parts.join('&');
    },

    /**
     * Requête AJAX (XMLHttpRequest) renvoyant une Promise.
     * opts : { method, url, data (objet → formulaire), body (FormData), timeout, onProgress }
     */
    ajax: function (opts) {
      return new Promise(function (resolve, reject) {
        var xhr = new XMLHttpRequest();
        var method = (opts.method || 'GET').toUpperCase();
        xhr.open(method, opts.url, true);
        xhr.timeout = opts.timeout || 15000;
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        xhr.setRequestHeader('Accept', 'application/json');
        if (method !== 'GET' && EFI.csrf) { xhr.setRequestHeader('X-CSRF-Token', EFI.csrf); }
        var payload = null;
        if (opts.body) {
          payload = opts.body;
        } else if (opts.data) {
          payload = EFI.form(opts.data);
          xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        }
        if (opts.onProgress && xhr.upload) {
          xhr.upload.onprogress = function (e) { if (e.lengthComputable) { opts.onProgress(e.loaded / e.total); } };
        }
        xhr.onload = function () {
          var json = null;
          try { json = xhr.responseText ? JSON.parse(xhr.responseText) : null; } catch (e) { json = null; }
          if (xhr.status >= 200 && xhr.status < 300) {
            resolve(json);
          } else {
            var msg = (json && json.error) ? json.error : ('Erreur HTTP ' + xhr.status);
            var err = new Error(msg);
            err.status = xhr.status;
            err.data = json;
            reject(err);
          }
        };
        xhr.onerror = function () { var e = new Error('Serveur injoignable (erreur réseau).'); e.status = 0; reject(e); };
        xhr.ontimeout = function () { var e = new Error('Délai d\u2019attente dépassé.'); e.status = 0; reject(e); };
        xhr.send(payload);
      });
    },

    /** Crée un élément DOM : EFI.el('div', {class:'x', text:'...'}, [enfants]). */
    el: function (tag, attrs, children) {
      var node = document.createElement(tag);
      attrs = attrs || {};
      for (var k in attrs) {
        if (!Object.prototype.hasOwnProperty.call(attrs, k)) { continue; }
        var v = attrs[k];
        if (v === undefined || v === null || v === false) { continue; }
        if (k === 'text') { node.textContent = v; }
        else if (k === 'class') { node.className = v; }
        else if (k.indexOf('on') === 0 && typeof v === 'function') { node.addEventListener(k.substring(2), v); }
        else if (k === 'dataset') { for (var d in v) { if (Object.prototype.hasOwnProperty.call(v, d)) { node.dataset[d] = v[d]; } } }
        else { node.setAttribute(k, v === true ? '' : v); }
      }
      (children || []).forEach(function (c) {
        if (c === null || c === undefined) { return; }
        node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
      });
      return node;
    },

    /** Crée une icône SVG à partir d'un symbole <path d="...">. */
    icon: function (pathD, cls) {
      var ns = 'http://www.w3.org/2000/svg';
      var svg = document.createElementNS(ns, 'svg');
      svg.setAttribute('viewBox', '0 0 24 24');
      svg.setAttribute('fill', 'none');
      svg.setAttribute('stroke', 'currentColor');
      svg.setAttribute('stroke-width', '2');
      svg.setAttribute('stroke-linecap', 'round');
      svg.setAttribute('stroke-linejoin', 'round');
      svg.setAttribute('aria-hidden', 'true');
      if (cls) { svg.setAttribute('class', cls); }
      var p = document.createElementNS(ns, 'path');
      p.setAttribute('d', pathD);
      svg.appendChild(p);
      return svg;
    },

    /** Échappe une chaîne pour un contexte HTML (préférer textContent). */
    esc: function (s) {
      return String(s === undefined || s === null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    },

    /** Affiche une notification temporaire. type : success | error | info */
    toast: function (message, type) {
      var zone = document.querySelector('.toast-zone');
      if (!zone) {
        zone = EFI.el('div', { 'class': 'toast-zone', role: 'status', 'aria-live': 'polite' });
        document.body.appendChild(zone);
      }
      var t = EFI.el('div', { 'class': 'toast ' + (type || 'info'), text: message });
      zone.appendChild(t);
      setTimeout(function () {
        t.className += ' leaving';
        setTimeout(function () { if (t.parentNode) { t.parentNode.removeChild(t); } }, 320);
      }, 3600);
    },

    pad: function (n) { return (n < 10 ? '0' : '') + n; },

    /** Formate une date ISO en français, fuseau Europe/Paris. */
    fmtDateTime: function (iso, withSeconds) {
      var d = iso instanceof Date ? iso : new Date(iso);
      if (isNaN(d.getTime())) { return ''; }
      var o = { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Paris' };
      if (withSeconds) { o.second = '2-digit'; }
      try { return d.toLocaleString('fr-FR', o); } catch (e) { return d.toLocaleString(); }
    },

    fmtTime: function (iso) {
      var d = iso instanceof Date ? iso : new Date(iso);
      try { return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Paris' }); }
      catch (e) { return EFI.pad(d.getHours()) + ':' + EFI.pad(d.getMinutes()); }
    },

    fmtBytes: function (n) {
      if (n < 1024) { return n + ' o'; }
      var u = ['Ko', 'Mo', 'Go', 'To'], i = -1;
      do { n = n / 1024; i++; } while (n >= 1024 && i < u.length - 1);
      return n.toFixed(n < 10 ? 1 : 0).replace('.', ',') + ' ' + u[i];
    },

    debounce: function (fn, ms) {
      var t;
      return function () {
        var args = arguments, self = this;
        clearTimeout(t);
        t = setTimeout(function () { fn.apply(self, args); }, ms);
      };
    }
  };

  /* Menu mobile */
  document.addEventListener('DOMContentLoaded', function () {
    var toggle = document.querySelector('.nav-toggle');
    var nav = document.querySelector('.main-nav');
    if (toggle && nav) {
      toggle.addEventListener('click', function () {
        var open = nav.classList.toggle('open');
        toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      });
    }
    var year = document.querySelector('[data-year]');
    if (year) { year.textContent = String(new Date().getFullYear()); }
  });

  window.EFI = EFI;
})(window, document);
