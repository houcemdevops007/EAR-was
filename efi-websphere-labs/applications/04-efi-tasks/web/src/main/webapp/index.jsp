<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Tableau Kanban";
    String pageDescription = "EFI Task Manager : gestion de tâches Kanban (À faire, En cours, Terminé) — opérations CRUD et méthodes HTTP sur WebSphere.";
    String appShort = "Task Manager";
    String[][] navLinks = { {"Tableau", "/"}, {"API JSON", "/api/tasks"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = true;
    String bodyClass = "app-tasks";
    String[] pageScripts = { "/assets/js/tasks.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="page-hero tasks-hero" aria-labelledby="page-title">
    <div class="row" style="align-items:flex-end">
      <div>
        <span class="eyebrow">Application 04 · CRUD &amp; méthodes HTTP</span>
        <h1 id="page-title">Tableau Kanban</h1>
        <p>Créez, modifiez, déplacez et supprimez des tâches. Chaque action appelle l'API REST
          <code class="code-light">/api/tasks</code> (GET, POST, PUT, DELETE) et l'état est conservé côté serveur.</p>
      </div>
      <span class="spacer"></span>
      <button type="button" class="btn btn-light btn-lg" id="new-task-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
        Nouvelle tâche
      </button>
    </div>
  </section>

  <section class="stats-bar grid grid-4" aria-label="Statistiques">
    <div class="card stat-card"><div class="stat"><span class="stat-label">Total</span><span class="stat-value" id="st-total">–</span></div>
      <div class="progress" aria-hidden="true"><span id="st-progress"></span></div><span class="small muted"><span id="st-pct">0</span> % terminées</span></div>
    <div class="card stat-card st-todo"><div class="stat"><span class="stat-label">À faire</span><span class="stat-value" id="st-todo">–</span></div></div>
    <div class="card stat-card st-doing"><div class="stat"><span class="stat-label">En cours</span><span class="stat-value" id="st-doing">–</span></div></div>
    <div class="card stat-card st-alert"><div class="stat"><span class="stat-label">En retard · Haute priorité</span>
      <span class="stat-value"><span id="st-overdue">–</span> · <span id="st-high">–</span></span></div></div>
  </section>

  <section class="toolbar card" aria-label="Recherche et filtres">
    <div class="search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
      <label class="sr-only" for="search">Rechercher une tâche</label>
      <input class="input" id="search" type="search" placeholder="Rechercher par titre ou description…">
    </div>
    <label class="sr-only" for="filter-priority">Filtrer par priorité</label>
    <select class="select" id="filter-priority">
      <option value="">Toutes priorités</option><option value="haute">Haute</option><option value="moyenne">Moyenne</option><option value="basse">Basse</option>
    </select>
    <label class="check"><input type="checkbox" id="filter-overdue"> En retard uniquement</label>
    <span class="spacer"></span>
    <button type="button" class="btn btn-ghost btn-sm" id="reset-btn" title="Restaure les tâches de démonstration">↺ Données de démo</button>
  </section>

  <section class="board" aria-label="Tableau Kanban">
    <div class="column" data-status="todo">
      <header class="col-head"><span class="col-dot"></span><h2>À faire</h2><span class="badge" data-count="todo">0</span></header>
      <div class="col-body" data-drop="todo" aria-label="Colonne À faire"></div>
    </div>
    <div class="column" data-status="doing">
      <header class="col-head"><span class="col-dot"></span><h2>En cours</h2><span class="badge" data-count="doing">0</span></header>
      <div class="col-body" data-drop="doing" aria-label="Colonne En cours"></div>
    </div>
    <div class="column" data-status="done">
      <header class="col-head"><span class="col-dot"></span><h2>Terminé</h2><span class="badge" data-count="done">0</span></header>
      <div class="col-body" data-drop="done" aria-label="Colonne Terminé"></div>
    </div>
  </section>
  <p class="small muted center">Astuce : glissez-déposez les cartes entre les colonnes ou utilisez les flèches ← → de chaque carte.
    Données stockées en mémoire sur <code><%= Html.esc(ServerInfo.hostname()) %></code> (perdues au redémarrage).</p>
</div>

<dialog class="modal" id="task-dialog" aria-labelledby="task-dialog-title">
  <form id="task-form" novalidate>
    <div class="modal-head"><h2 id="task-dialog-title">Nouvelle tâche</h2><span class="spacer"></span>
      <button type="button" class="icon-btn" data-close aria-label="Fermer"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg></button></div>
    <div class="modal-body">
      <input type="hidden" id="task-id">
      <div class="field">
        <label for="task-title">Titre <span aria-hidden="true">*</span></label>
        <input class="input" id="task-title" name="title" required minlength="3" maxlength="100" aria-describedby="err-title">
        <span class="error-text" id="err-title" role="alert"></span>
      </div>
      <div class="field">
        <label for="task-desc">Description</label>
        <textarea class="textarea" id="task-desc" name="description" maxlength="1000" aria-describedby="err-description"></textarea>
        <span class="error-text" id="err-description" role="alert"></span>
      </div>
      <div class="grid grid-3">
        <div class="field">
          <label for="task-priority">Priorité</label>
          <select class="select" id="task-priority" name="priority"><option value="basse">Basse</option><option value="moyenne" selected>Moyenne</option><option value="haute">Haute</option></select>
          <span class="error-text" id="err-priority" role="alert"></span>
        </div>
        <div class="field">
          <label for="task-status">Statut</label>
          <select class="select" id="task-status" name="status"><option value="todo">À faire</option><option value="doing">En cours</option><option value="done">Terminé</option></select>
          <span class="error-text" id="err-status" role="alert"></span>
        </div>
        <div class="field">
          <label for="task-due">Échéance</label>
          <input class="input" id="task-due" name="dueDate" type="date" aria-describedby="err-dueDate">
          <span class="error-text" id="err-dueDate" role="alert"></span>
        </div>
      </div>
    </div>
    <div class="modal-foot">
      <button type="button" class="btn btn-danger" id="task-delete" hidden>Supprimer</button>
      <span class="spacer"></span>
      <button type="button" class="btn btn-ghost" data-close>Annuler</button>
      <button type="submit" class="btn btn-gradient" id="task-submit">Créer la tâche</button>
    </div>
  </form>
</dialog>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
