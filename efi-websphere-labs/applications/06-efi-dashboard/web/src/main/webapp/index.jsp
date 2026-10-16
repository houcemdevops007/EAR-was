<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %><%
    String pageTitle = "Tableau de bord serveur";
    String pageDescription = "EFI Server Dashboard : hostname, version Java, système, mémoire JVM, processeurs, uptime et requêtes traitées, rafraîchis toutes les 10 secondes.";
    String appShort = "Server Dashboard";
    String[][] navLinks = { {"Tableau de bord", "/"}, {"API métriques", "/api/metrics"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = false;
    String bodyClass = "app-dashboard";
    String[] pageScripts = { "/assets/js/dashboard.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %><%
    String srv = ServerInfo.serverName();
    String node = ServerInfo.nodeName();
    String cell = ServerInfo.cellName();
%>
<div class="container">
  <section class="page-hero dash-hero" aria-labelledby="page-title">
    <div class="dash-hero-grid">
      <div>
        <span class="eyebrow">Application 06 · Observation de la JVM</span>
        <h1 id="page-title">Tableau de bord serveur</h1>
        <p>Instantané en direct de la JVM qui exécute cette application. Ouvrez-la sur <code class="code-light">test</code> puis sur
          <code class="code-light">test2</code> : chaque serveur possède sa propre JVM, sa mémoire et ses compteurs.</p>
        <dl class="identity">
          <div><dt>Hôte</dt><dd id="id-host"><%= Html.esc(ServerInfo.hostname()) %></dd></div>
          <div><dt>Serveur</dt><dd id="id-server"><%= Html.esc(Html.orDefault(srv, "hors WebSphere")) %></dd></div>
          <div><dt>Nœud</dt><dd id="id-node"><%= Html.esc(Html.orDefault(node, "—")) %></dd></div>
          <div><dt>Cellule</dt><dd id="id-cell"><%= Html.esc(Html.orDefault(cell, "—")) %></dd></div>
        </dl>
      </div>
      <div class="refresh-panel glass">
        <div class="refresh-ring" aria-hidden="true">
          <svg viewBox="0 0 44 44"><circle cx="22" cy="22" r="19" class="rr-bg"/><circle cx="22" cy="22" r="19" class="rr-fg" id="rr-fg" transform="rotate(-90 22 22)"/></svg>
          <span id="rr-count">10</span>
        </div>
        <div>
          <div class="refresh-status"><span class="badge badge-success badge-live" id="live-badge">En direct</span></div>
          <div class="small" id="last-update">Chargement des métriques…</div>
        </div>
        <div class="row refresh-actions">
          <button class="btn btn-light btn-sm" type="button" id="btn-refresh">↻ Actualiser</button>
          <button class="btn btn-outline-light btn-sm" type="button" id="btn-pause" aria-pressed="false">⏸ Pause</button>
        </div>
      </div>
    </div>
  </section>

  <div class="alert alert-danger" role="alert" id="metrics-error" hidden>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
    <div><strong>Métriques indisponibles.</strong> <span id="metrics-error-text"></span> Nouvelle tentative au prochain cycle.</div>
  </div>

  <section class="grid kpis" aria-label="Indicateurs clés">
    <article class="card kpi kpi-heap">
      <div class="gauge" aria-hidden="true">
        <svg viewBox="0 0 120 70"><path d="M10 64 A50 50 0 0 1 110 64" class="g-bg"/><path d="M10 64 A50 50 0 0 1 110 64" class="g-fg" id="gauge-fg" pathLength="100"/></svg>
        <div class="gauge-val"><strong id="k-heap-pct">–</strong><span>%</span></div>
      </div>
      <div class="kpi-label">Heap JVM utilisé</div>
      <div class="kpi-sub" id="k-heap-sub">–</div>
    </article>
    <article class="card kpi">
      <span class="kpi-icon" aria-hidden="true">⏱</span>
      <div class="kpi-value" id="k-uptime">–</div>
      <div class="kpi-label">Uptime de la JVM</div>
      <div class="kpi-sub" id="k-uptime-sub">–</div>
    </article>
    <article class="card kpi">
      <span class="kpi-icon" aria-hidden="true">📨</span>
      <div class="kpi-value" id="k-req">–</div>
      <div class="kpi-label">Requêtes traitées</div>
      <div class="kpi-sub" id="k-req-sub">–</div>
    </article>
    <article class="card kpi">
      <span class="kpi-icon" aria-hidden="true">🧠</span>
      <div class="kpi-value" id="k-cpu">–</div>
      <div class="kpi-label">Processeurs disponibles</div>
      <div class="kpi-sub" id="k-cpu-sub">–</div>
    </article>
    <article class="card kpi">
      <span class="kpi-icon" aria-hidden="true">🧵</span>
      <div class="kpi-value" id="k-threads">–</div>
      <div class="kpi-label">Threads actifs</div>
      <div class="kpi-sub" id="k-threads-sub">–</div>
    </article>
    <article class="card kpi">
      <span class="kpi-icon" aria-hidden="true">👥</span>
      <div class="kpi-value" id="k-sessions">–</div>
      <div class="kpi-label">Sessions HTTP actives</div>
      <div class="kpi-sub" id="k-sessions-sub">–</div>
    </article>
  </section>

  <section class="grid grid-2 charts" aria-label="Graphiques">
    <article class="card chart-card">
      <div class="card-title"><span class="icon" aria-hidden="true">📈</span>Mémoire heap (Mo) <span class="spacer"></span><span class="small muted" id="heap-legend"></span></div>
      <svg class="chart" id="chart-heap" viewBox="0 0 600 220" role="img" aria-label="Évolution de la mémoire heap utilisée"></svg>
      <div class="chart-legend small"><span class="lg lg-used"></span>Utilisée <span class="lg lg-committed"></span>Réservée (committed) <span class="lg lg-max"></span>Maximum</div>
    </article>
    <article class="card chart-card">
      <div class="card-title"><span class="icon" aria-hidden="true">📊</span>Requêtes par intervalle de 10 s <span class="spacer"></span><span class="small muted" id="req-legend"></span></div>
      <svg class="chart" id="chart-req" viewBox="0 0 600 220" role="img" aria-label="Nombre de requêtes par intervalle"></svg>
      <div class="chart-legend small">Historique local des 30 derniers relevés (5 minutes), conservé dans le navigateur.</div>
    </article>
  </section>

  <section class="grid grid-3 info-grid" aria-label="Détails techniques">
    <article class="card">
      <div class="card-title"><span class="icon" aria-hidden="true">☕</span>Java</div>
      <dl class="kv">
        <dt>Version</dt><dd id="j-version">–</dd>
        <dt>Éditeur</dt><dd id="j-vendor">–</dd>
        <dt>Machine virtuelle</dt><dd id="j-vm">–</dd>
        <dt>Spécification</dt><dd id="j-spec">–</dd>
        <dt>Classes chargées</dt><dd id="j-classes">–</dd>
      </dl>
    </article>
    <article class="card">
      <div class="card-title"><span class="icon" aria-hidden="true">🖥</span>Système d'exploitation</div>
      <dl class="kv">
        <dt>Système</dt><dd id="o-name">–</dd>
        <dt>Version</dt><dd id="o-version">–</dd>
        <dt>Architecture</dt><dd id="o-arch">–</dd>
        <dt>Charge moyenne (1 min)</dt><dd id="o-load">–</dd>
        <dt>Heure du serveur</dt><dd id="o-time">–</dd>
      </dl>
    </article>
    <article class="card">
      <div class="card-title"><span class="icon" aria-hidden="true">♻️</span>Ramasse-miettes (GC)</div>
      <div class="table-wrap slim"><table class="table" id="gc-table">
        <thead><tr><th scope="col">Collecteur</th><th scope="col">Cycles</th><th scope="col">Temps</th></tr></thead>
        <tbody><tr><td colspan="3" class="muted">–</td></tr></tbody>
      </table></div>
      <p class="small muted" style="margin-top:.75rem">Démarrage JVM : <span id="j-start">–</span><br>Démarrage application : <span id="a-start">–</span></p>
    </article>
  </section>

  <section class="card note-card" aria-labelledby="note-title">
    <h2 id="note-title">🔒 Ce que ce tableau de bord n'affiche pas</h2>
    <p>Par conception, aucune variable d'environnement, aucun argument de la JVM, aucun classpath, chemin d'installation, mot de passe
      ou paramètre de sécurité n'est exposé. Les données proviennent uniquement des MXBeans standard
      <code>java.lang.management</code> en lecture seule. Pour une supervision complète, utilisez la console d'administration
      (<em>Surveillance et réglage › Performance Viewer</em>) ou PMI.</p>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
