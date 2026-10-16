<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Calculatrice";
    String pageDescription = "EFI Calculator : calculatrice moderne avec pourcentages, historique, clavier virtuel et physique, mode clair et sombre.";
    String appShort = "Calculator";
    String[][] navLinks = { {"Calculatrice", "/"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = false;
    String bodyClass = "app-calc";
    String[] pageScripts = { "/assets/js/calc-core.js", "/assets/js/calculator.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <div class="calc-head">
    <div>
      <span class="eyebrow eyebrow-soft">Application 09 · Packaging front-end dans un EAR</span>
      <h1 id="page-title">Calculatrice EFI</h1>
      <p class="muted">Calcul 100 % JavaScript, sans <code>eval()</code> : l'expression est analysée par un parseur dédié.</p>
    </div>
    <button type="button" class="theme-toggle" id="theme-toggle" aria-pressed="false" aria-label="Activer le mode sombre">
      <span class="tt-track"><span class="tt-thumb"><span class="tt-sun" aria-hidden="true">☀️</span><span class="tt-moon" aria-hidden="true">🌙</span></span></span>
      <span class="tt-label" id="theme-label">Mode clair</span>
    </button>
  </div>

  <div class="calc-layout">
    <section class="calculator" aria-labelledby="page-title">
      <div class="display" id="display">
        <div class="expr-line">
          <label class="sr-only" for="expr">Expression</label>
          <input id="expr" class="expr" type="text" inputmode="none" autocomplete="off" spellcheck="false" aria-describedby="calc-error"
                 maxlength="200" placeholder="0">
          <button type="button" class="copy-btn" id="copy-btn" aria-label="Copier le résultat" title="Copier le résultat">⧉</button>
        </div>
        <output class="result" id="result" for="expr" aria-live="polite">0</output>
        <div class="calc-error" id="calc-error" role="alert"></div>
      </div>
      <div class="keypad" role="group" aria-label="Clavier de la calculatrice">
        <button type="button" class="key fn" data-action="clear" aria-label="Tout effacer">AC</button>
        <button type="button" class="key fn" data-action="back" aria-label="Effacer le dernier caractère">⌫</button>
        <button type="button" class="key fn" data-insert="%" aria-label="Pourcentage">%</button>
        <button type="button" class="key op" data-insert="/" aria-label="Diviser">÷</button>

        <button type="button" class="key" data-insert="7">7</button>
        <button type="button" class="key" data-insert="8">8</button>
        <button type="button" class="key" data-insert="9">9</button>
        <button type="button" class="key op" data-insert="*" aria-label="Multiplier">×</button>

        <button type="button" class="key" data-insert="4">4</button>
        <button type="button" class="key" data-insert="5">5</button>
        <button type="button" class="key" data-insert="6">6</button>
        <button type="button" class="key op" data-insert="-" aria-label="Soustraire">−</button>

        <button type="button" class="key" data-insert="1">1</button>
        <button type="button" class="key" data-insert="2">2</button>
        <button type="button" class="key" data-insert="3">3</button>
        <button type="button" class="key op" data-insert="+" aria-label="Additionner">+</button>

        <button type="button" class="key fn small" data-insert="(" aria-label="Parenthèse ouvrante">(</button>
        <button type="button" class="key fn small" data-insert=")" aria-label="Parenthèse fermante">)</button>
        <button type="button" class="key fn small" data-action="sign" aria-label="Changer le signe">±</button>
        <button type="button" class="key eq" data-action="equals" aria-label="Égal">=</button>

        <button type="button" class="key zero" data-insert="0">0</button>
        <button type="button" class="key" data-insert="," aria-label="Virgule décimale">,</button>
      </div>
      <p class="kbd-help small muted">Clavier : <kbd>0–9</kbd> <kbd>+</kbd> <kbd>−</kbd> <kbd>*</kbd> <kbd>/</kbd> <kbd>%</kbd> <kbd>( )</kbd>
        <kbd>Entrée</kbd> calculer · <kbd>⌫</kbd> corriger · <kbd>Échap</kbd> effacer</p>
    </section>

    <aside class="card history" aria-labelledby="history-title">
      <div class="history-head">
        <h2 id="history-title">Historique</h2>
        <span class="spacer"></span>
        <button type="button" class="btn btn-ghost btn-sm" id="history-clear">Effacer</button>
      </div>
      <ol class="history-list" id="history-list" reversed></ol>
      <p class="empty-history muted small" id="history-empty">Vos calculs apparaîtront ici. Cliquez sur une ligne pour la réutiliser.</p>
      <p class="small muted history-note">Historique conservé dans ce navigateur (50 derniers calculs).</p>
    </aside>
  </div>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
