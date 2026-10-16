<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %><%
    String pageTitle = "Bienvenue dans la formation WebSphere";
    String pageDescription = "EFI Welcome : première application EAR déployée sur IBM WebSphere ND 8.5.5 — présentation du DMGR, des Node Agents et des serveurs d'applications.";
    String appShort = "Welcome";
    String[][] navLinks = { {"Accueil", "/"}, {"Architecture", "/#architecture"}, {"Concepts", "/#concepts"}, {"Santé API", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = false;
    String bodyClass = "app-welcome";
    String[] pageScripts = { "/assets/js/welcome.js" };
    String titleText = "Bienvenue dans la formation WebSphere EFI-Academy";
    String[] titleWords = titleText.split(" ");
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">

  <section class="welcome-hero page-hero" aria-labelledby="hero-title">
    <div class="hero-grid">
      <div>
        <span class="eyebrow">Application 01 · Premier déploiement EAR</span>
        <h1 id="hero-title" class="hero-title" aria-label="<%= Html.esc(titleText) %>">
<%  for (int i = 0; i < titleWords.length; i++) { %><span class="word" aria-hidden="true" style="--i:<%= i %>"><%= Html.esc(titleWords[i]) %></span> <% } %>
        </h1>
        <p class="hero-lead reveal reveal-3">Vous lisez cette page : votre archive <code class="code-light">efi-welcome.ear</code>
          a été installée par le Deployment Manager, synchronisée par le Node Agent et démarrée sur un serveur d'applications.</p>
        <div class="row hero-actions reveal reveal-4">
          <button type="button" class="btn btn-light btn-lg" id="btn-discover">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="m10 8 6 4-6 4z"/></svg>
            Découvrir WebSphere
          </button>
          <a class="btn btn-outline-light btn-lg" href="<%= efiCtx %>/api/health">Vérifier /api/health</a>
        </div>
      </div>

      <aside class="status-card glass reveal reveal-2" aria-label="État de l'application">
        <div class="status-head">
          <span class="badge badge-success badge-live">Application opérationnelle</span>
          <span class="muted small">HTTP 200</span>
        </div>
        <dl class="status-list">
          <div><dt>Nom d'hôte</dt><dd id="info-host"><%= Html.esc(ServerInfo.hostname()) %></dd></div>
          <div><dt>Serveur d'applications</dt><dd><%= Html.esc(Html.orDefault(ServerInfo.serverName(), "hors WebSphere")) %></dd></div>
          <div><dt>Nœud</dt><dd><%= Html.esc(Html.orDefault(ServerInfo.nodeName(), "—")) %></dd></div>
          <div><dt>Cellule</dt><dd><%= Html.esc(Html.orDefault(ServerInfo.cellName(), "—")) %></dd></div>
          <div><dt>Racine de contexte</dt><dd><code><%= Html.esc(efiCtx) %></code></dd></div>
          <div class="clock-row"><dt>Date et heure (serveur, Paris)</dt>
            <dd><span id="clock-date" data-server-epoch="<%= System.currentTimeMillis() %>"><%= Html.esc(ServerInfo.nowParis("EEEE d MMMM yyyy")) %></span>
              <strong id="clock-time" class="clock-time"><%= Html.esc(ServerInfo.nowParis("HH:mm:ss")) %></strong></dd></div>
        </dl>
      </aside>
    </div>
  </section>

  <section id="architecture" class="section" aria-labelledby="archi-title">
    <div class="section-head">
      <div>
        <h2 id="archi-title">L'infrastructure du laboratoire</h2>
        <p>Une cellule WebSphere ND composée d'un Deployment Manager et de deux nœuds fédérés.</p>
      </div>
      <span class="badge">Topologie EFI Academy</span>
    </div>
    <div class="card infra-card">
      <svg class="infra" viewBox="0 0 900 430" role="img" aria-labelledby="infra-title infra-desc">
        <title id="infra-title">Schéma de la cellule vm-was-efiCell02</title>
        <desc id="infra-desc">Le Deployment Manager vm-was-efi pilote deux nœuds : nodesa1 avec le serveur test, nodesa2 avec le serveur test2. Les Node Agents synchronisent la configuration.</desc>
        <defs>
          <linearGradient id="gDmgr" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#4f46e5"/><stop offset="1" stop-color="#2563eb"/></linearGradient>
          <linearGradient id="gNode" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff"/><stop offset="1" stop-color="#eef2ff"/></linearGradient>
          <linearGradient id="gSrv" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#14b8a6"/><stop offset="1" stop-color="#2dd4bf"/></linearGradient>
          <filter id="shadow" x="-10%" y="-10%" width="120%" height="140%"><feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#0b1437" flood-opacity=".18"/></filter>
        </defs>
        <rect class="cell" x="14" y="14" width="872" height="402" rx="26"/>
        <text class="cell-label" x="38" y="44">CELLULE · vm-was-efiCell02</text>

        <path class="link" d="M450 150 C450 190 230 180 230 236"/>
        <path class="link" d="M450 150 C450 190 670 180 670 236"/>
        <text class="link-label" x="262" y="196">synchronisation</text>
        <text class="link-label" x="560" y="196">synchronisation</text>

        <g filter="url(#shadow)">
          <rect x="318" y="58" width="264" height="92" rx="18" fill="url(#gDmgr)"/>
          <text class="t-white t-strong" x="450" y="90" text-anchor="middle">Deployment Manager</text>
          <text class="t-white t-small" x="450" y="112" text-anchor="middle">vm-was-efi · 172.16.3.20</text>
          <text class="t-white t-small t-dim" x="450" y="132" text-anchor="middle">Dmgr01 · SOAP 8880 · Console 9044</text>
        </g>

        <g filter="url(#shadow)">
          <rect class="node" x="56" y="236" width="348" height="156" rx="18" fill="url(#gNode)"/>
          <text class="t-strong" x="78" y="266">nodesa1 · 172.16.3.21</text>
          <text class="t-small t-muted" x="78" y="286">Profil Custom01 · nodesa1Node01</text>
          <rect x="78" y="304" width="132" height="66" rx="12" class="agent"/>
          <text class="t-strong t-agent" x="144" y="332" text-anchor="middle">Node Agent</text>
          <text class="t-small t-agent" x="144" y="352" text-anchor="middle">nodeagent</text>
          <rect x="224" y="304" width="160" height="66" rx="12" fill="url(#gSrv)"/>
          <text class="t-white t-strong" x="304" y="332" text-anchor="middle">Serveur « test »</text>
          <text class="t-white t-small" x="304" y="352" text-anchor="middle">efi-*.ear</text>
        </g>

        <g filter="url(#shadow)">
          <rect class="node" x="496" y="236" width="348" height="156" rx="18" fill="url(#gNode)"/>
          <text class="t-strong" x="518" y="266">nodesa2</text>
          <text class="t-small t-muted" x="518" y="286">nodesa2Node01</text>
          <rect x="518" y="304" width="132" height="66" rx="12" class="agent"/>
          <text class="t-strong t-agent" x="584" y="332" text-anchor="middle">Node Agent</text>
          <text class="t-small t-agent" x="584" y="352" text-anchor="middle">nodeagent</text>
          <rect x="664" y="304" width="160" height="66" rx="12" fill="url(#gSrv)"/>
          <text class="t-white t-strong" x="744" y="332" text-anchor="middle">Serveur « test2 »</text>
          <text class="t-white t-small" x="744" y="352" text-anchor="middle">efi-*.ear</text>
        </g>
        <circle class="packet" r="6"><animateMotion dur="3.2s" repeatCount="indefinite" path="M450 150 C450 190 230 180 230 236"/></circle>
        <circle class="packet" r="6"><animateMotion dur="3.2s" begin="1.6s" repeatCount="indefinite" path="M450 150 C450 190 670 180 670 236"/></circle>
      </svg>
    </div>

    <div class="grid grid-3 roles">
      <article class="card hoverable reveal reveal-1">
        <div class="card-title"><span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="7" rx="2"/><rect x="3" y="14" width="18" height="7" rx="2"/><path d="M7 6.5h.01M7 17.5h.01"/></svg></span>Deployment Manager (DMGR)</div>
        <p>Point central d'administration de la cellule. Il héberge la <strong>console d'administration</strong>
          (port 9044) et le <strong>référentiel maître</strong> de configuration. Toute installation d'EAR passe par lui.</p>
        <p class="muted small">Processus : <code>dmgr</code> · Profil <code>Dmgr01</code></p>
      </article>
      <article class="card hoverable reveal reveal-2">
        <div class="card-title"><span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/></svg></span>Node Agent</div>
        <p>Agent présent sur chaque nœud fédéré. Il <strong>synchronise</strong> la configuration locale avec le DMGR,
          démarre et surveille les serveurs d'applications du nœud.</p>
        <p class="muted small">Commandes : <code>startNode.sh</code> · <code>syncNode.sh</code></p>
      </article>
      <article class="card hoverable reveal reveal-3">
        <div class="card-title"><span class="icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 3 7v10l9 5 9-5V7z"/><path d="M3 7l9 5 9-5M12 12v10"/></svg></span>Serveur d'applications</div>
        <p>JVM qui exécute réellement vos applications Java EE : conteneur web (servlets, JSP), EJB, JDBC, JMS.
          Dans le lab : <strong>test</strong> sur nodesa1 et <strong>test2</strong> sur nodesa2.</p>
        <p class="muted small">Commandes : <code>startServer.sh test</code> · <code>serverStatus.sh -all</code></p>
      </article>
    </div>
  </section>

  <section id="concepts" class="section" aria-labelledby="concepts-title">
    <div class="section-head">
      <div>
        <h2 id="concepts-title">Les concepts clés</h2>
        <p>Quatre notions indispensables avant de déployer vos applications.</p>
      </div>
    </div>
    <div class="grid grid-4 concepts">
      <article class="concept card hoverable">
        <span class="concept-tag">EAR</span>
        <h3>Enterprise Archive</h3>
        <p>Archive d'entreprise qui regroupe un ou plusieurs modules (WAR, JAR EJB) et le descripteur
          <code>META-INF/application.xml</code> qui définit les racines de contexte.</p>
      </article>
      <article class="concept card hoverable">
        <span class="concept-tag">WAR</span>
        <h3>Web Archive</h3>
        <p>Module web : pages JSP, servlets compilées dans <code>WEB-INF/classes</code>, bibliothèques dans
          <code>WEB-INF/lib</code> et descripteur <code>WEB-INF/web.xml</code>.</p>
      </article>
      <article class="concept card hoverable">
        <span class="concept-tag">Cellule</span>
        <h3>Domaine d'administration</h3>
        <p>Regroupement logique du DMGR et des nœuds qu'il administre. Ici : <code>vm-was-efiCell02</code>,
          avec une configuration et une sécurité communes.</p>
      </article>
      <article class="concept card hoverable">
        <span class="concept-tag">Fédération</span>
        <h3>Ajout d'un nœud</h3>
        <p>Opération <code>addNode.sh vm-was-efi 8880</code> qui rattache un profil au DMGR : le nœud
          reçoit un Node Agent et devient administrable depuis la console.</p>
      </article>
    </div>

    <div class="grid grid-2 section">
      <div class="card">
        <h3>Structure de cette application</h3>
        <pre class="tree" aria-label="Arborescence de l'EAR">efi-welcome.ear
├── META-INF/
│   ├── MANIFEST.MF
│   └── application.xml   → context-root /efi-welcome
└── efi-welcome.war
    ├── index.jsp
    ├── assets/ (css, js, img)
    └── WEB-INF/
        ├── web.xml
        ├── ibm-web-ext.xml
        └── classes/fr/efiacademy/lab/…</pre>
      </div>
      <div class="card">
        <h3>Ce que vous venez de réaliser</h3>
        <ol class="steps">
          <li><strong>Installer</strong> l'EAR sur le DMGR (console ou <code>wsadmin</code>).</li>
          <li><strong>Mapper</strong> le module web sur le serveur cible.</li>
          <li><strong>Enregistrer</strong> la configuration dans le référentiel maître.</li>
          <li><strong>Synchroniser</strong> le nœud via le Node Agent.</li>
          <li><strong>Démarrer</strong> l'application puis la tester dans le navigateur.</li>
        </ol>
      </div>
    </div>
  </section>
</div>

<dialog class="modal" id="discover-dialog" aria-labelledby="slide-title">
  <div class="modal-head">
    <span class="badge" id="slide-counter">1 / 6</span>
    <h2 id="slide-title">Découvrir WebSphere</h2>
    <span class="spacer"></span>
    <button type="button" class="icon-btn" data-close aria-label="Fermer la présentation">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
    </button>
  </div>
  <div class="modal-body">
    <div class="slides">
      <section class="slide" data-title="IBM WebSphere ND en bref">
        <div class="slide-icon">🏢</div>
        <p><strong>IBM WebSphere Application Server Network Deployment</strong> est un serveur d'applications
          Java EE destiné aux environnements de production : administration centralisée, clustering,
          haute disponibilité et répartition de charge.</p>
        <ul><li>Version du lab : <strong>8.5.5</strong> — Java EE 6, Servlet 3.0, JSP 2.2.</li>
          <li>Installation : <code>/opt/IBM/WebSphere/AppServer_2</code>.</li></ul>
      </section>
      <section class="slide" data-title="La cellule et le DMGR" hidden>
        <div class="slide-icon">🧭</div>
        <p>La <strong>cellule</strong> est le domaine d'administration. Le <strong>Deployment Manager</strong>
          détient le référentiel maître (<code>config/cells/…</code>) et expose la console d'administration.</p>
        <ul><li>Console : <code>https://vm-was-efi:9044/ibm/console</code></li>
          <li>Scripting : <code>wsadmin.sh -lang jython -port 8880</code></li></ul>
      </section>
      <section class="slide" data-title="Node Agent et synchronisation" hidden>
        <div class="slide-icon">🔄</div>
        <p>Chaque nœud possède un <strong>Node Agent</strong> qui récupère les modifications du référentiel maître.
          Après un « Enregistrer », la synchronisation peut être automatique ou manuelle.</p>
        <ul><li>Console : Administration système › Nœuds › Synchroniser.</li>
          <li>Ligne de commande (Node Agent arrêté) : <code>syncNode.sh vm-was-efi 8880</code></li></ul>
      </section>
      <section class="slide" data-title="Serveurs d'applications et clusters" hidden>
        <div class="slide-icon">⚙️</div>
        <p>Un <strong>serveur d'applications</strong> est une JVM. Plusieurs serveurs identiques peuvent former un
          <strong>cluster</strong> pour la tolérance de panne ; le plugin du serveur HTTP répartit alors la charge.</p>
        <ul><li>Lab : serveur <code>test</code> (nodesa1Node01) et <code>test2</code> (nodesa2Node01).</li></ul>
      </section>
      <section class="slide" data-title="Packaging : WAR et EAR" hidden>
        <div class="slide-icon">📦</div>
        <p>Le <strong>WAR</strong> contient le module web ; l'<strong>EAR</strong> l'encapsule avec
          <code>application.xml</code>. WebSphere installe des EAR (un WAR seul est encapsulé automatiquement).</p>
        <ul><li>Construction : <code>mvn clean package</code> (JDK 8, bytecode Java 6).</li></ul>
      </section>
      <section class="slide" data-title="Le cycle de déploiement" hidden>
        <div class="slide-icon">🚀</div>
        <ol class="steps compact">
          <li>Installer l'EAR (<code>AdminApp.install</code>).</li>
          <li>Mapper les modules sur <code>WebSphere:cell=…,node=…,server=…</code>.</li>
          <li>Enregistrer (<code>AdminConfig.save()</code>).</li>
          <li>Synchroniser le nœud.</li>
          <li>Démarrer l'application et vérifier <code>/api/health</code>.</li>
        </ol>
      </section>
    </div>
    <div class="slide-dots" role="tablist" aria-label="Diapositives"></div>
  </div>
  <div class="modal-foot">
    <button type="button" class="btn btn-ghost" id="slide-prev">← Précédent</button>
    <button type="button" class="btn btn-gradient" id="slide-next">Suivant →</button>
  </div>
</dialog>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
