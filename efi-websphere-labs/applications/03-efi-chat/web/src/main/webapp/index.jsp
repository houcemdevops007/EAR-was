<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Salon de discussion";
    String pageDescription = "EFI Chat Room : messagerie interne des candidats — sessions HTTP, servlets et polling AJAX sur WebSphere.";
    String appShort = "Chat Room";
    String[][] navLinks = { {"Salon", "/"}, {"API messages", "/api/chat/messages"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = true;
    String bodyClass = "app-chat";
    String[] pageScripts = { "/assets/js/chat.js" };
    String currentNick = (String) session.getAttribute("chat.nick");
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <div id="chat-app" class="chat-app" data-nick="<%= Html.esc(currentNick == null ? "" : currentNick) %>">

    <section id="join-screen" class="join-screen" <%= currentNick != null ? "hidden" : "" %> aria-labelledby="join-title">
      <div class="join-card card">
        <div class="join-illu" aria-hidden="true">
          <span class="bubble b1">Bonjour 👋</span><span class="bubble b2">Prêt pour le TP ?</span><span class="bubble b3">Oui !</span>
        </div>
        <span class="badge">Application 03 · Sessions HTTP &amp; AJAX</span>
        <h1 id="join-title">Rejoindre le salon EFI</h1>
        <p class="muted">Choisissez un pseudonyme : il sera mémorisé dans votre <strong>session HTTP</strong> côté serveur.</p>
        <form id="join-form" novalidate>
          <div class="field">
            <label for="nickname">Pseudonyme</label>
            <input class="input" id="nickname" name="nickname" type="text" minlength="2" maxlength="20" required
                   autocomplete="nickname" placeholder="ex. Candidat_07" aria-describedby="nick-hint nick-error">
            <span id="nick-hint" class="hint">2 à 20 caractères : lettres, chiffres, espace, point, tiret, souligné.</span>
            <span id="nick-error" class="error-text" role="alert"></span>
          </div>
          <button class="btn btn-gradient btn-lg" type="submit" style="width:100%">Entrer dans le salon →</button>
        </form>
      </div>
    </section>

    <section id="chat-screen" class="chat-screen" <%= currentNick == null ? "hidden" : "" %> aria-label="Salon de discussion">
      <aside class="chat-side card">
        <div class="side-head">
          <div class="room-avatar" aria-hidden="true">#</div>
          <div><h2 class="room-name">Salon général</h2><span class="small muted">Formation WebSphere</span></div>
        </div>
        <div class="me-box">
          <span class="avatar" id="me-avatar" aria-hidden="true">?</span>
          <div><div class="small muted">Connecté en tant que</div><strong id="me-nick">—</strong></div>
        </div>
        <h3 class="side-title">En ligne <span id="user-count" class="badge">0</span></h3>
        <ul id="user-list" class="user-list" aria-live="polite"></ul>
        <div class="side-actions">
          <button type="button" class="btn btn-ghost btn-sm" id="admin-btn">🔐 Mode administrateur</button>
          <button type="button" class="btn btn-danger btn-sm" id="clear-btn" hidden>🧹 Vider le salon</button>
          <button type="button" class="btn btn-ghost btn-sm" id="leave-btn">Quitter le salon</button>
        </div>
        <div class="alert alert-info small memory-note">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
          <div>Stockage <strong>en mémoire</strong> (100 derniers messages) : perdu au redémarrage et propre à chaque JVM
            (<code><%= Html.esc(ServerInfo.hostname()) %></code>).</div>
        </div>
      </aside>

      <div class="chat-main card">
        <header class="chat-head">
          <div>
            <h1 class="chat-title">💬 Salon général</h1>
            <span class="small muted"><span id="conn-state" class="badge badge-success badge-live">Connecté</span> · actualisation automatique toutes les 2 s</span>
          </div>
        </header>
        <div id="messages" class="messages" role="log" aria-live="polite" aria-label="Messages du salon">
          <div class="empty-state" id="empty-state">Aucun message pour l'instant. Lancez la conversation !</div>
        </div>
        <form id="send-form" class="composer" novalidate>
          <label class="sr-only" for="message">Votre message</label>
          <textarea id="message" name="text" class="textarea composer-input" rows="1" maxlength="500" required
                    placeholder="Écrire un message… (Entrée pour envoyer, Maj+Entrée pour un saut de ligne)"></textarea>
          <span id="char-count" class="char-count small muted" aria-live="polite">0 / 500</span>
          <button class="btn btn-gradient send-btn" type="submit" aria-label="Envoyer le message">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m22 2-7 20-4-9-9-4z"/><path d="M22 2 11 13"/></svg>
          </button>
        </form>
      </div>
    </section>
  </div>
</div>

<dialog class="modal" id="admin-dialog" aria-labelledby="admin-title">
  <form id="admin-form" method="dialog" novalidate>
    <div class="modal-head"><h2 id="admin-title">Mode administrateur de démonstration</h2></div>
    <div class="modal-body">
      <p class="muted">Le code est généré au démarrage de l'application et écrit dans le journal
        <code>SystemOut.log</code> du serveur (ou défini par la propriété JVM <code>efi.chat.adminCode</code>).</p>
      <div class="field">
        <label for="admin-code">Code administrateur</label>
        <input class="input" id="admin-code" type="password" autocomplete="off" required>
        <span id="admin-error" class="error-text" role="alert"></span>
      </div>
    </div>
    <div class="modal-foot">
      <button type="button" class="btn btn-ghost" id="admin-cancel">Annuler</button>
      <button type="submit" class="btn btn-gradient">Valider</button>
    </div>
  </form>
</dialog>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
