<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Centre de fichiers";
    String pageDescription = "EFI File Center : téléversement, téléchargement et suppression de fichiers pédagogiques avec limites de taille et types autorisés.";
    String appShort = "File Center";
    String[][] navLinks = { {"Fichiers", "/"}, {"API JSON", "/api/files"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = true;
    String bodyClass = "app-files";
    String[] pageScripts = { "/assets/js/files.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="page-hero files-hero" aria-labelledby="page-title">
    <div class="files-hero-grid">
      <div>
        <span class="eyebrow">Application 08 · Upload, répertoires &amp; permissions Linux</span>
        <h1 id="page-title">Centre de fichiers pédagogiques</h1>
        <p>Déposez vos supports de TP : ils sont enregistrés <strong>hors de l'EAR</strong>, sous un nom généré par le serveur,
          et ne sont jamais exécutés. Accès limité au réseau du laboratoire.</p>
      </div>
      <div class="hero-stats glass" aria-live="polite">
        <div><span class="hs-value" id="hs-count">–</span><span class="hs-label">fichiers</span></div>
        <div><span class="hs-value" id="hs-size">–</span><span class="hs-label">utilisés</span></div>
        <div><span class="hs-value" id="hs-limit">–</span><span class="hs-label">max / fichier</span></div>
      </div>
    </div>
  </section>

  <div class="alert" role="status" id="storage-status" hidden>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
    <div>
      <strong id="storage-title">Stockage</strong> <span id="storage-message"></span>
      <div class="small">Répertoire : <code id="storage-dir">–</code> sur <code id="storage-host">–</code></div>
      <details class="fix-details" id="storage-fix" hidden>
        <summary>Comment corriger ? (commandes à exécuter sur le nœud par l'administrateur)</summary>
        <pre class="code-block"><code id="fix-commands"></code></pre>
        <p class="small">Remplacez <code>wasadmin:wasgroup</code> par l'utilisateur et le groupe qui exécutent le serveur d'applications
          (<code>ps -o user= -p &lt;PID du serveur&gt;</code>). Aucun redémarrage n'est nécessaire.</p>
      </details>
    </div>
  </div>

  <div class="files-layout">
    <section class="card upload-card" aria-labelledby="upload-title">
      <h2 id="upload-title" class="card-title"><span class="icon" aria-hidden="true">⬆</span>Téléverser</h2>
      <form id="upload-form" method="post" enctype="multipart/form-data" action="<%= efiCtx %>/api/files">
        <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
        <label class="dropzone" id="dropzone" for="file-input">
          <span class="dz-icon" aria-hidden="true">
            <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 44H14a10 10 0 0 1-1-19.95A16 16 0 0 1 44 20a12 12 0 0 1 6 23"/><path d="M32 30v24M24 38l8-8 8 8"/></svg>
          </span>
          <strong>Glissez-déposez vos fichiers ici</strong>
          <span class="muted">ou <span class="dz-link">parcourez votre poste</span></span>
          <span class="small muted" id="dz-rules">Chargement des règles…</span>
          <input type="file" id="file-input" name="file" multiple class="sr-only">
        </label>
        <noscript><button class="btn btn-gradient" type="submit" style="margin-top:1rem">Envoyer</button></noscript>
      </form>
      <ul class="queue" id="queue" aria-live="polite"></ul>
      <div class="rules">
        <h3>Règles de sécurité appliquées</h3>
        <ul>
          <li>Extensions en liste blanche + contrôle de la signature binaire (PNG, JPEG, GIF, PDF, ZIP/Office).</li>
          <li>Taille vérifiée côté serveur pendant la copie, pas seulement dans le navigateur.</li>
          <li>Nom sur disque : identifiant aléatoire <code>&lt;uuid&gt;.bin</code> → ni collision, ni traversée de chemin.</li>
          <li>Téléchargement forcé en pièce jointe (<code>Content-Disposition: attachment</code>, <code>nosniff</code>).</li>
          <li>Jeton anti-CSRF sur l'envoi et la suppression.</li>
        </ul>
      </div>
    </section>

    <section class="card list-card" aria-labelledby="list-title">
      <div class="list-head">
        <h2 id="list-title" class="card-title" style="margin:0"><span class="icon" aria-hidden="true">🗂</span>Fichiers disponibles</h2>
        <span class="spacer"></span>
        <div class="search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
          <label class="sr-only" for="file-search">Filtrer les fichiers</label>
          <input class="input" id="file-search" type="search" placeholder="Filtrer par nom…">
        </div>
        <button type="button" class="icon-btn" id="refresh-btn" aria-label="Actualiser la liste" title="Actualiser">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></svg>
        </button>
      </div>
      <div class="usage" aria-hidden="true"><span id="usage-bar"></span></div>
      <p class="small muted" id="usage-text">–</p>
      <div class="table-wrap">
        <table class="table files-table">
          <thead><tr><th scope="col">Nom</th><th scope="col">Taille</th><th scope="col">Ajouté le</th><th scope="col"><span class="sr-only">Actions</span></th></tr></thead>
          <tbody id="files-body"><tr><td colspan="4"><span class="spinner"></span> Chargement…</td></tr></tbody>
        </table>
      </div>
    </section>
  </div>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
