<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Mes notes";
    String pageDescription = "EFI Notes : notes personnelles avec catégories, favoris, recherche et export JSON, stockées localement dans le navigateur.";
    String appShort = "Notes";
    String[][] navLinks = { {"Mes notes", "/"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = false;
    String bodyClass = "app-notes";
    String[] pageScripts = { "/assets/js/notes-core.js", "/assets/js/notes.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="page-hero notes-hero" aria-labelledby="page-title">
    <div class="row" style="align-items:flex-end">
      <div>
        <span class="eyebrow">Application 07 · Ressources statiques &amp; JavaScript</span>
        <h1 id="page-title">Mes notes de formation</h1>
        <p>Une application 100 % côté navigateur : le serveur WebSphere ne fait que livrer la page, le CSS et le JavaScript.
          Vos notes restent dans le <strong>localStorage</strong> de ce navigateur.</p>
      </div>
      <span class="spacer"></span>
      <button type="button" class="btn btn-light btn-lg" id="new-note-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        Nouvelle note
      </button>
    </div>
  </section>

  <noscript><div class="alert alert-warning" role="alert"><div>Cette application nécessite JavaScript : activez-le pour gérer vos notes.</div></div></noscript>
  <div class="alert alert-danger" role="alert" id="storage-error" hidden>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
    <div><strong>Stockage local indisponible.</strong> Navigation privée ou stockage désactivé : les notes ne seront pas conservées après fermeture de l'onglet.</div>
  </div>

  <div class="notes-layout">
    <aside class="notes-side" aria-label="Catégories et statistiques">
      <div class="card side-card">
        <h2 class="side-title">Catégories</h2>
        <ul class="cat-list" id="cat-list" role="list"></ul>
      </div>
      <div class="card side-card">
        <h2 class="side-title">Statistiques</h2>
        <div class="mini-stats">
          <div><span class="stat-value" id="st-total">0</span><span class="stat-label">notes</span></div>
          <div><span class="stat-value" id="st-fav">0</span><span class="stat-label">favorites</span></div>
        </div>
        <p class="small muted" id="st-storage">–</p>
      </div>
      <div class="card side-card">
        <h2 class="side-title">Sauvegarde</h2>
        <div class="stack">
          <button type="button" class="btn btn-gradient btn-block" id="export-btn">⬇ Exporter en JSON</button>
          <label class="btn btn-ghost btn-block file-btn" for="import-file">⬆ Importer un JSON</label>
          <input type="file" id="import-file" accept="application/json,.json" class="sr-only">
          <button type="button" class="btn btn-ghost btn-sm btn-block" id="samples-btn">✨ Ajouter des exemples</button>
        </div>
      </div>
    </aside>

    <section class="notes-main" aria-label="Liste des notes">
      <div class="toolbar card">
        <div class="search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
          <label class="sr-only" for="search">Rechercher par titre</label>
          <input class="input" id="search" type="search" placeholder="Rechercher par titre…  ( / )" autocomplete="off">
        </div>
        <label class="check"><input type="checkbox" id="in-content"> Inclure le contenu</label>
        <label class="check"><input type="checkbox" id="fav-only"> ★ Favorites</label>
        <label class="sr-only" for="sort">Trier</label>
        <select class="select" id="sort">
          <option value="recent">Favorites puis récentes</option>
          <option value="created">Date de création</option>
          <option value="title">Titre (A → Z)</option>
        </select>
      </div>
      <p class="small muted result-line" id="result-line" aria-live="polite"></p>
      <div class="notes-grid" id="notes-grid"></div>
      <div class="card empty-state" id="empty-state" hidden>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><path d="M14 3v6h6M8 13h8M8 17h5"/></svg>
        <p id="empty-text">Aucune note pour l'instant.</p>
        <button type="button" class="btn btn-gradient" id="empty-new">Créer ma première note</button>
      </div>
    </section>
  </div>
</div>

<dialog class="modal" id="note-dialog" aria-labelledby="note-dialog-title">
  <form id="note-form" novalidate>
    <div class="modal-head"><h2 id="note-dialog-title">Nouvelle note</h2><span class="spacer"></span>
      <button type="button" class="icon-btn" data-close aria-label="Fermer"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg></button></div>
    <div class="modal-body">
      <div class="field">
        <label for="note-title">Titre <span aria-hidden="true">*</span></label>
        <input class="input" id="note-title" name="title" required maxlength="120" aria-describedby="err-title">
        <span class="error-text" id="err-title" role="alert"></span>
      </div>
      <div class="field">
        <label for="note-content">Contenu</label>
        <textarea class="textarea" id="note-content" name="content" maxlength="5000" rows="8" aria-describedby="err-content content-count"></textarea>
        <span class="hint" id="content-count">0 / 5000</span>
        <span class="error-text" id="err-content" role="alert"></span>
      </div>
      <div class="grid grid-2">
        <div class="field">
          <label for="note-category">Catégorie</label>
          <select class="select" id="note-category" name="category"></select>
          <span class="error-text" id="err-category" role="alert"></span>
        </div>
        <div class="field">
          <span class="label">Favori</span>
          <label class="check fav-check"><input type="checkbox" id="note-favorite" name="favorite"> ★ Marquer comme favorite</label>
        </div>
      </div>
    </div>
    <div class="modal-foot">
      <button type="button" class="btn btn-danger" id="note-delete" hidden>Supprimer</button>
      <span class="spacer"></span>
      <button type="button" class="btn btn-ghost" data-close>Annuler</button>
      <button type="submit" class="btn btn-gradient" id="note-submit">Enregistrer</button>
    </div>
  </form>
</dialog>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
