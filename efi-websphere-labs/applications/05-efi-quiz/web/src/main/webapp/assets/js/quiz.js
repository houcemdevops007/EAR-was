/* EFI Quiz WebSphere — amélioration progressive (le quiz fonctionne sans JavaScript). */
(function () {
  'use strict';
  var $ = EFI.$, $$ = EFI.$$;

  /* ---- Page question : raccourcis clavier 1..n et Entrée ---- */
  var form = $('#question-form');
  if (form) {
    var radios = $$('input[name="choice"]', form);
    var feedbackMode = !!$('#btn-next', form);

    radios.forEach(function (r) {
      r.addEventListener('change', function () {
        $$('.choice', form).forEach(function (c) { c.classList.remove('selected'); });
        r.closest('.choice').classList.add('selected');
      });
      if (r.checked) { r.closest('.choice').classList.add('selected'); }
    });

    form.addEventListener('submit', function (e) {
      if (!feedbackMode && !radios.some(function (r) { return r.checked; })) {
        e.preventDefault();
        EFI.toast('Sélectionnez une réponse avant de valider.', 'error');
        var first = radios[0];
        if (first) { first.focus(); }
        return;
      }
      var btn = $('button[type="submit"]', form);
      if (btn) { btn.disabled = true; btn.classList.add('is-loading'); }
    });

    document.addEventListener('keydown', function (e) {
      if (e.altKey || e.ctrlKey || e.metaKey) { return; }
      var t = e.target;
      if (t && (t.tagName === 'INPUT' && t.type !== 'radio' || t.tagName === 'TEXTAREA' || t.tagName === 'SELECT')) { return; }
      if (!feedbackMode && /^[1-9]$/.test(e.key)) {
        var r = radios[parseInt(e.key, 10) - 1];
        if (r) {
          r.checked = true;
          r.dispatchEvent(new Event('change', { bubbles: true }));
          r.focus();
          e.preventDefault();
        }
      } else if (e.key === 'Enter' && !(t && t.tagName === 'BUTTON')) {
        e.preventDefault();
        if (typeof form.requestSubmit === 'function') { form.requestSubmit(); }
        else { form.submit(); }
      }
    });

    if (feedbackMode) {
      var fb = $('.feedback', form);
      if (fb) { fb.classList.add('reveal'); }
      if (form.classList.contains('is-correct')) { burst(form); }
    }
  }

  /* ---- Confirmation d'abandon ---- */
  $$('[data-confirm]').forEach(function (b) {
    b.addEventListener('click', function (e) {
      if (!window.confirm(b.getAttribute('data-confirm'))) { e.preventDefault(); }
    });
  });

  /* ---- Page résultat : compteur animé + filtre des corrections ---- */
  var counter = $('.ring-label [data-count]');
  if (counter) {
    var target = parseInt(counter.getAttribute('data-count'), 10) || 0;
    var start = null;
    var animate = function (ts) {
      if (!start) { start = ts; }
      var p = Math.min(1, (ts - start) / 900);
      counter.textContent = String(Math.round(target * (1 - Math.pow(1 - p, 3))));
      if (p < 1) { window.requestAnimationFrame(animate); }
    };
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      counter.textContent = '0';
      window.requestAnimationFrame(animate);
    }
    if (target >= 70) { burst($('.result-hero')); }
  }

  $$('[data-filter]').forEach(function (b) {
    b.addEventListener('click', function () {
      var f = b.getAttribute('data-filter');
      $$('[data-filter]').forEach(function (x) { x.setAttribute('aria-pressed', String(x === b)); });
      $$('.review-item').forEach(function (it) {
        it.hidden = f === 'wrong' && it.classList.contains('good');
      });
    });
  });

  /* ---- Petit effet de confettis CSS (décoratif, désactivé si mouvement réduit) ---- */
  function burst(host) {
    if (!host || window.matchMedia('(prefers-reduced-motion: reduce)').matches) { return; }
    var colors = ['#7c3aed', '#ec4899', '#22d3ee', '#f59e0b', '#10b981'];
    var layer = EFI.el('div', { 'class': 'confetti-layer', 'aria-hidden': 'true' });
    for (var i = 0; i < 28; i++) {
      var s = EFI.el('span');
      s.style.left = (Math.random() * 100) + '%';
      s.style.background = colors[i % colors.length];
      s.style.animationDelay = (Math.random() * 0.4) + 's';
      s.style.transform = 'rotate(' + Math.round(Math.random() * 360) + 'deg)';
      layer.appendChild(s);
    }
    host.appendChild(layer);
    window.setTimeout(function () { if (layer.parentNode) { layer.parentNode.removeChild(layer); } }, 2600);
  }
})();
