/*
 * EFI Mini Shop — logique côté navigateur (ES5, sans dépendance).
 *  - Catalogue : recherche, filtre par catégorie et tri sur les articles rendus par la JSP.
 *  - Panier : tiroir latéral synchronisé avec /api/cart (HttpSession côté serveur).
 *  - Commande : focus sur le résumé d'erreurs, anti double-soumission.
 * Toutes les données sont insérées via textContent (EFI.el), jamais innerHTML.
 */
(function (EFI, document) {
  'use strict';

  var $ = EFI.$, $$ = EFI.$$;

  /* ------------------------------------------------------------------ */
  /* Utilitaires                                                         */
  /* ------------------------------------------------------------------ */

  /** Supprime les accents et passe en minuscules (même règle que Catalog.fold côté Java). */
  function fold(s) {
    s = String(s || '').toLowerCase();
    if (s.normalize) { s = s.normalize('NFD').replace(/[\u0300-\u036f]/g, ''); }
    return s.replace(/\s+/g, ' ').trim();
  }

  function plural(n, one, many) { return n + ' ' + (n > 1 ? many : one); }

  /* ------------------------------------------------------------------ */
  /* Catalogue : recherche / filtres / tri                               */
  /* ------------------------------------------------------------------ */

  function initCatalog() {
    var grid = $('#product-grid');
    if (!grid) { return; }
    var search = $('#search');
    var sort = $('#sort');
    var chips = $$('.chip[data-category]');
    var count = $('#result-count');
    var none = $('#no-results');
    var products = $$('article.product', grid);
    var category = '';

    function apply() {
      var q = fold(search.value);
      var terms = q ? q.split(' ') : [];
      var visible = 0;
      products.forEach(function (p) {
        var hay = p.getAttribute('data-name');
        var ok = (!category || p.getAttribute('data-category') === category);
        for (var i = 0; ok && i < terms.length; i++) {
          if (hay.indexOf(terms[i]) < 0) { ok = false; }
        }
        p.hidden = !ok;
        if (ok) { visible++; }
      });
      var mode = sort.value;
      var sorted = products.slice().sort(function (a, b) {
        function n(el, k) { return parseFloat(el.getAttribute('data-' + k)) || 0; }
        switch (mode) {
          case 'price-asc': return n(a, 'price') - n(b, 'price');
          case 'price-desc': return n(b, 'price') - n(a, 'price');
          case 'rating': return n(b, 'rating') - n(a, 'rating');
          case 'name':
            return a.querySelector('.product-name').textContent
              .localeCompare(b.querySelector('.product-name').textContent, 'fr');
          default: return n(a, 'order') - n(b, 'order');
        }
      });
      sorted.forEach(function (p) { grid.appendChild(p); });
      count.textContent = plural(visible, 'produit', 'produits') +
        (visible !== products.length ? ' sur ' + products.length : '');
      none.hidden = visible !== 0;
    }

    search.addEventListener('input', EFI.debounce(apply, 120));
    sort.addEventListener('change', apply);
    chips.forEach(function (chip) {
      chip.addEventListener('click', function () {
        category = chip.getAttribute('data-category');
        chips.forEach(function (c) { c.setAttribute('aria-pressed', c === chip ? 'true' : 'false'); });
        apply();
      });
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === '/' && document.activeElement !== search &&
          !/INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName)) {
        e.preventDefault();
        search.focus();
      }
    });

    grid.addEventListener('click', function (e) {
      var btn = e.target.closest ? e.target.closest('[data-add]') : null;
      if (!btn || btn.disabled) { return; }
      btn.disabled = true;
      btn.classList.add('is-loading');
      Cart.add(btn.getAttribute('data-add'), 1).then(function (cart) {
        var art = btn.closest('article.product');
        var name = art ? art.querySelector('.product-name').textContent : 'Article';
        EFI.toast('« ' + name + ' » ajouté au panier', 'success');
        btn.classList.add('added');
        setTimeout(function () { btn.classList.remove('added'); }, 900);
        return cart;
      }, function (err) {
        EFI.toast(err.message, 'error');
      }).then(function () {
        btn.disabled = false;
        btn.classList.remove('is-loading');
      });
    });
  }

  /* ------------------------------------------------------------------ */
  /* Panier : API + tiroir                                               */
  /* ------------------------------------------------------------------ */

  var Cart = {
    state: null,

    load: function () {
      return EFI.ajax({ url: EFI.url('/api/cart') }).then(Cart.render);
    },
    add: function (id, qty) {
      return EFI.ajax({ method: 'POST', url: EFI.url('/api/cart'), data: { productId: id, quantity: qty } })
        .then(Cart.render);
    },
    set: function (id, qty) {
      return EFI.ajax({ method: 'PUT', url: EFI.url('/api/cart/' + encodeURIComponent(id)), data: { quantity: qty } })
        .then(Cart.render);
    },
    remove: function (id) {
      return EFI.ajax({ method: 'DELETE', url: EFI.url('/api/cart/' + encodeURIComponent(id)) }).then(Cart.render);
    },
    clear: function () {
      return EFI.ajax({ method: 'DELETE', url: EFI.url('/api/cart') }).then(Cart.render);
    },

    render: function (cart) {
      if (!cart) { return cart; }
      var previous = Cart.state ? Cart.state.count : null;
      Cart.state = cart;

      var badge = $('#cart-count');
      if (badge) {
        badge.textContent = String(cart.count);
        if (previous !== null && previous !== cart.count) {
          var fab = $('#cart-fab');
          fab.classList.remove('bump');
          void fab.offsetWidth; /* relance l'animation */
          fab.classList.add('bump');
        }
      }
      var list = $('#cart-lines');
      if (!list) { return cart; }

      var dc = $('#drawer-count');
      dc.textContent = plural(cart.count, 'article', 'articles');
      list.textContent = '';
      cart.lines.forEach(function (l) { list.appendChild(Cart.line(l)); });

      var empty = cart.lines.length === 0;
      $('#cart-empty').hidden = !empty;
      $('#drawer-foot').hidden = empty;
      $('#t-subtotal').textContent = cart.subtotal;
      $('#t-shipping').textContent = cart.shipping;
      $('#t-total').textContent = cart.total;
      $('#t-vat').textContent = 'Dont TVA (20 %) : ' + cart.vat;

      var fs = $('#free-ship');
      if (cart.freeShippingRemaining) {
        fs.hidden = false;
        fs.textContent = '';
        fs.appendChild(document.createTextNode('Plus que '));
        fs.appendChild(EFI.el('strong', { text: cart.freeShippingRemaining }));
        fs.appendChild(document.createTextNode(' pour profiter de la livraison offerte 🚚'));
        var pct = Math.max(4, Math.min(100, Math.round(cart.subtotalCents / 100)));
        fs.style.setProperty('--progress', pct + '%');
      } else if (!empty) {
        fs.hidden = false;
        fs.textContent = '🎉 Livraison offerte sur cette commande !';
        fs.style.setProperty('--progress', '100%');
      } else {
        fs.hidden = true;
      }
      return cart;
    },

    line: function (l) {
      var minus = EFI.el('button', { type: 'button', 'class': 'qty-btn', 'aria-label': 'Retirer un exemplaire de ' + l.name,
        disabled: l.quantity <= 1, onclick: function () { Cart.mutate(Cart.set(l.productId, l.quantity - 1)); } }, ['−']);
      var plus = EFI.el('button', { type: 'button', 'class': 'qty-btn', 'aria-label': 'Ajouter un exemplaire de ' + l.name,
        disabled: l.quantity >= l.maxQuantity, onclick: function () { Cart.mutate(Cart.set(l.productId, l.quantity + 1)); } }, ['+']);
      var del = EFI.el('button', { type: 'button', 'class': 'icon-btn line-remove', 'aria-label': 'Supprimer ' + l.name + ' du panier',
        onclick: function () { Cart.mutate(Cart.remove(l.productId), '« ' + l.name + ' » retiré du panier'); } },
        [EFI.icon('M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6')]);
      return EFI.el('li', { 'class': 'cart-line' }, [
        EFI.el('img', { src: EFI.url('/' + l.image), alt: '', width: '64', height: '48' }),
        EFI.el('div', { 'class': 'cart-line-body' }, [
          EFI.el('strong', { text: l.name }),
          EFI.el('span', { 'class': 'small muted', text: l.unitPrice + ' l’unité' }),
          EFI.el('div', { 'class': 'qty', role: 'group', 'aria-label': 'Quantité de ' + l.name }, [
            minus, EFI.el('output', { 'class': 'qty-value', 'aria-live': 'polite', text: String(l.quantity) }), plus
          ])
        ]),
        EFI.el('div', { 'class': 'cart-line-side' }, [EFI.el('span', { 'class': 'line-total', text: l.total }), del])
      ]);
    },

    mutate: function (promise, okMessage) {
      var drawer = $('#cart-drawer');
      if (drawer) { drawer.classList.add('busy'); }
      return promise.then(function () {
        if (okMessage) { EFI.toast(okMessage, 'info'); }
      }, function (err) {
        EFI.toast(err.message, 'error');
      }).then(function () {
        if (drawer) { drawer.classList.remove('busy'); }
      });
    }
  };

  function initDrawer() {
    var drawer = $('#cart-drawer');
    if (!drawer) { return; }
    var fab = $('#cart-fab');
    var backdrop = $('#drawer-backdrop');
    var lastFocus = null;

    function open() {
      lastFocus = document.activeElement;
      backdrop.hidden = false;
      drawer.classList.add('open');
      drawer.setAttribute('aria-hidden', 'false');
      fab.setAttribute('aria-expanded', 'true');
      document.body.classList.add('drawer-open');
      setTimeout(function () { $('#drawer-close').focus(); }, 60);
      Cart.load().then(null, function (err) { EFI.toast(err.message, 'error'); });
    }
    function close() {
      drawer.classList.remove('open');
      drawer.setAttribute('aria-hidden', 'true');
      fab.setAttribute('aria-expanded', 'false');
      document.body.classList.remove('drawer-open');
      backdrop.hidden = true;
      if (lastFocus && lastFocus.focus) { lastFocus.focus(); }
    }

    fab.addEventListener('click', open);
    $('#drawer-close').addEventListener('click', close);
    backdrop.addEventListener('click', close);
    document.addEventListener('keydown', function (e) {
      if (!drawer.classList.contains('open')) { return; }
      if (e.key === 'Escape') { close(); return; }
      if (e.key === 'Tab') { /* piège de focus dans le tiroir */
        var f = $$('button:not([disabled]), a[href], input, [tabindex="0"]', drawer).filter(function (x) { return x.offsetParent !== null; });
        if (!f.length) { return; }
        if (e.shiftKey && document.activeElement === f[0]) { e.preventDefault(); f[f.length - 1].focus(); }
        else if (!e.shiftKey && document.activeElement === f[f.length - 1]) { e.preventDefault(); f[0].focus(); }
      }
    });
    $('#clear-cart').addEventListener('click', function () {
      if (window.confirm('Vider entièrement le panier ?')) { Cart.mutate(Cart.clear(), 'Panier vidé'); }
    });

    Cart.load().then(null, function () { /* compteur rendu côté serveur conservé */ });
  }

  /* ------------------------------------------------------------------ */
  /* Commande et confirmation                                            */
  /* ------------------------------------------------------------------ */

  function initCheckout() {
    var form = $('#checkout-form');
    if (form) {
      var summary = $('#error-summary');
      if (summary) { summary.focus(); }
      form.addEventListener('submit', function (e) {
        var btn = $('#place-order');
        if (btn.disabled) { e.preventDefault(); return; }
        btn.disabled = true;
        btn.classList.add('is-loading');
        btn.textContent = 'Validation en cours…';
      });
    }
    var print = $('#print-order');
    if (print) { print.addEventListener('click', function () { window.print(); }); }
  }

  document.addEventListener('DOMContentLoaded', function () {
    initCatalog();
    initDrawer();
    initCheckout();
  });
})(window.EFI, document);
