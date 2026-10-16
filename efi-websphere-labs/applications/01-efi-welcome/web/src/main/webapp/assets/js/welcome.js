/* EFI Welcome — horloge synchronisée avec le serveur et présentation interactive */
(function () {
  'use strict';
  var $ = EFI.$;

  /* ---- Horloge : décalage entre l'heure du serveur et celle du navigateur ---- */
  var dateEl = $('#clock-date');
  var timeEl = $('#clock-time');
  var offset = 0;
  if (dateEl) {
    var serverEpoch = parseInt(dateEl.getAttribute('data-server-epoch'), 10);
    if (!isNaN(serverEpoch)) { offset = serverEpoch - Date.now(); }
  }
  function tick() {
    var now = new Date(Date.now() + offset);
    try {
      timeEl.textContent = now.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone: 'Europe/Paris' });
      dateEl.textContent = now.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Europe/Paris' });
    } catch (e) {
      timeEl.textContent = EFI.pad(now.getHours()) + ':' + EFI.pad(now.getMinutes()) + ':' + EFI.pad(now.getSeconds());
    }
  }
  if (timeEl && dateEl) {
    tick();
    setInterval(tick, 1000);
    // Resynchronisation périodique avec l'API serveur
    setInterval(function () {
      EFI.ajax({ url: EFI.url('/api/server-time'), timeout: 5000 }).then(function (d) {
        if (d && d.epochMillis) { offset = d.epochMillis - Date.now(); }
      }, function () { /* serveur indisponible : on garde le dernier décalage */ });
    }, 60000);
  }

  /* ---- Présentation « Découvrir WebSphere » ---- */
  var dialog = $('#discover-dialog');
  var openBtn = $('#btn-discover');
  if (!dialog || !openBtn) { return; }
  var slides = EFI.$$('.slide', dialog);
  var dots = $('.slide-dots', dialog);
  var title = $('#slide-title');
  var counter = $('#slide-counter');
  var prev = $('#slide-prev');
  var next = $('#slide-next');
  var index = 0;

  slides.forEach(function (s, i) {
    var b = EFI.el('button', { type: 'button', role: 'tab', 'aria-label': 'Diapositive ' + (i + 1) + ' : ' + s.getAttribute('data-title') });
    b.addEventListener('click', function () { show(i); });
    dots.appendChild(b);
  });

  function show(i) {
    index = Math.max(0, Math.min(slides.length - 1, i));
    slides.forEach(function (s, k) { s.hidden = k !== index; });
    EFI.$$('button', dots).forEach(function (b, k) { b.setAttribute('aria-selected', k === index ? 'true' : 'false'); });
    title.textContent = slides[index].getAttribute('data-title');
    counter.textContent = (index + 1) + ' / ' + slides.length;
    prev.disabled = index === 0;
    next.textContent = index === slides.length - 1 ? 'Terminer ✓' : 'Suivant →';
  }

  function open() {
    show(0);
    if (typeof dialog.showModal === 'function') { dialog.showModal(); }
    else { dialog.setAttribute('open', ''); }
  }
  function close() {
    if (typeof dialog.close === 'function') { dialog.close(); } else { dialog.removeAttribute('open'); }
    openBtn.focus();
  }

  openBtn.addEventListener('click', open);
  prev.addEventListener('click', function () { show(index - 1); });
  next.addEventListener('click', function () {
    if (index === slides.length - 1) { close(); EFI.toast('Présentation terminée — bonne formation !', 'success'); }
    else { show(index + 1); }
  });
  $('[data-close]', dialog).addEventListener('click', close);
  dialog.addEventListener('click', function (e) { if (e.target === dialog) { close(); } });
  dialog.addEventListener('keydown', function (e) {
    if (e.key === 'ArrowRight') { show(index + 1); }
    if (e.key === 'ArrowLeft') { show(index - 1); }
  });
})();
