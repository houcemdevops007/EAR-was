<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" %><%
    String pageTitle = "Calendrier et météo de Paris";
    String pageDescription = "EFI Calendar & Weather : calendrier interactif, horloge Europe/Paris et météo réelle de Paris via Open-Meteo.";
    String appShort = "Calendar & Weather";
    String[][] navLinks = { {"Tableau de bord", "/"}, {"Diagnostic", "/#diagnostic"}, {"API météo", "/api/weather"} };
    String navActive = "/";
    boolean useCsrf = false;
    String bodyClass = "app-calendar";
    String[] pageScripts = { "/assets/js/calendar-core.js", "/assets/js/calendar.js", "/assets/js/weather.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">

  <section class="page-hero cal-hero" aria-labelledby="page-title">
    <div class="cal-hero-grid">
      <div>
        <span class="eyebrow">Application 02 · Service externe</span>
        <h1 id="page-title">Calendrier &amp; météo de Paris</h1>
        <p>Données météo réelles fournies par l'API publique Open-Meteo, interrogée par le serveur WebSphere
          (proxy Java) ou, à défaut, directement par votre navigateur.</p>
      </div>
      <div class="clock-card glass" aria-live="off">
        <div class="clock-label">Heure de Paris <span id="tz-label" class="badge">Europe/Paris</span></div>
        <div id="big-clock" class="big-clock" data-server-epoch="<%= System.currentTimeMillis() %>">--:--:--</div>
        <div id="big-date" class="big-date"><%= Html.esc(ServerInfo.nowParis("EEEE d MMMM yyyy")) %></div>
      </div>
    </div>
  </section>

  <div class="dash-grid">
    <section class="card calendar-card" aria-labelledby="cal-title">
      <div class="cal-toolbar">
        <h2 id="cal-title" class="cal-title"><span id="cal-month">Mois</span> <span id="cal-year" class="muted">Année</span></h2>
        <div class="row cal-nav">
          <button type="button" class="icon-btn" id="prev-year" aria-label="Année précédente">«</button>
          <button type="button" class="icon-btn" id="prev-month" aria-label="Mois précédent">‹</button>
          <button type="button" class="btn btn-sm btn-gradient" id="today-btn">Aujourd'hui</button>
          <button type="button" class="icon-btn" id="next-month" aria-label="Mois suivant">›</button>
          <button type="button" class="icon-btn" id="next-year" aria-label="Année suivante">»</button>
        </div>
      </div>
      <div class="row cal-selects">
        <label class="sr-only" for="sel-month">Choisir le mois</label>
        <select class="select" id="sel-month"></select>
        <label class="sr-only" for="sel-year">Choisir l'année</label>
        <select class="select" id="sel-year"></select>
      </div>
      <div class="cal-grid" role="grid" aria-labelledby="cal-title">
        <div class="cal-row cal-head" role="row">
          <span role="columnheader" class="wk">Sem.</span>
          <span role="columnheader" abbr="lundi">Lun</span><span role="columnheader" abbr="mardi">Mar</span>
          <span role="columnheader" abbr="mercredi">Mer</span><span role="columnheader" abbr="jeudi">Jeu</span>
          <span role="columnheader" abbr="vendredi">Ven</span><span role="columnheader" abbr="samedi">Sam</span>
          <span role="columnheader" abbr="dimanche">Dim</span>
        </div>
        <div id="cal-body"></div>
      </div>
      <div class="cal-legend small muted">
        <span><i class="lg lg-today"></i>Aujourd'hui</span><span><i class="lg lg-sel"></i>Sélection</span>
        <span><i class="lg lg-hol"></i>Jour férié</span><span><i class="lg lg-wx"></i>Prévision disponible</span>
      </div>
    </section>

    <aside class="side-col">
      <section class="card weather-card" aria-labelledby="wx-title" aria-busy="true" id="weather-card">
        <div class="row">
          <h2 id="wx-title" class="card-heading">Météo actuelle · Paris</h2>
          <span class="spacer"></span>
          <span id="wx-source" class="badge">Chargement…</span>
        </div>
        <div id="wx-loading" class="wx-loading">
          <span class="spinner" aria-hidden="true"></span> <span>Interrogation du service météo…</span>
        </div>
        <div id="wx-content" class="wx-content" hidden>
          <div class="wx-main">
            <div id="wx-icon" class="wx-icon" aria-hidden="true">☀️</div>
            <div>
              <div class="wx-temp"><span id="wx-temp">--</span><span class="unit">°C</span></div>
              <div id="wx-cond" class="wx-cond">—</div>
              <div class="small muted">Ressenti <strong id="wx-feel">--</strong> °C</div>
            </div>
          </div>
          <dl class="wx-metrics">
            <div><dt>💧 Humidité</dt><dd><span id="wx-hum">--</span> %</dd></div>
            <div><dt>💨 Vent</dt><dd><span id="wx-wind">--</span> km/h <span id="wx-dir" class="muted"></span></dd></div>
            <div><dt>🌬️ Rafales</dt><dd><span id="wx-gust">--</span> km/h</dd></div>
            <div><dt>🧭 Pression</dt><dd><span id="wx-press">--</span> hPa</dd></div>
            <div><dt>🌅 Lever</dt><dd id="wx-sunrise">--:--</dd></div>
            <div><dt>🌇 Coucher</dt><dd id="wx-sunset">--:--</dd></div>
          </dl>
          <p class="small muted wx-updated">Mise à jour : <span id="wx-time">—</span></p>
        </div>
        <div id="wx-error" class="alert alert-danger" role="alert" hidden>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
          <div><strong>Service météo indisponible.</strong> <span id="wx-error-msg"></span>
            <div><button type="button" class="btn btn-sm btn-danger" id="wx-retry">Réessayer</button></div></div>
        </div>
      </section>

      <section class="card day-card" aria-labelledby="day-title">
        <h2 id="day-title" class="card-heading">Jour sélectionné</h2>
        <div id="day-details" class="day-details"></div>
      </section>
    </aside>
  </div>

  <section class="section" aria-labelledby="fc-title">
    <div class="section-head">
      <div><h2 id="fc-title">Prévisions sur 7 jours</h2><p>Températures minimales / maximales et probabilité de précipitations.</p></div>
    </div>
    <div id="forecast" class="forecast" aria-live="polite">
      <div class="fc-day skeleton"></div><div class="fc-day skeleton"></div><div class="fc-day skeleton"></div>
      <div class="fc-day skeleton"></div><div class="fc-day skeleton"></div><div class="fc-day skeleton"></div><div class="fc-day skeleton"></div>
    </div>
  </section>

  <section id="diagnostic" class="section" aria-labelledby="diag-title">
    <div class="card">
      <h2 id="diag-title" class="card-heading">🩺 Diagnostic réseau</h2>
      <p class="muted">Ce panneau indique comment les données ont été obtenues. Il aide à diagnostiquer les problèmes
        de DNS, de pare-feu, de proxy ou de certificat TLS côté serveur WebSphere.</p>
      <div class="table-wrap">
        <table class="table">
          <caption class="sr-only">Résultats des tentatives d'accès au service météo</caption>
          <thead><tr><th scope="col">Chemin</th><th scope="col">Résultat</th><th scope="col">Détail</th></tr></thead>
          <tbody>
            <tr><td>Serveur WebSphere → api.open-meteo.com</td><td id="diag-server-status"><span class="badge">en attente</span></td><td id="diag-server-detail" class="small">—</td></tr>
            <tr><td>Navigateur → api.open-meteo.com</td><td id="diag-browser-status"><span class="badge">non utilisé</span></td><td id="diag-browser-detail" class="small">Utilisé uniquement si le serveur échoue.</td></tr>
          </tbody>
        </table>
      </div>
      <p class="small muted">Commandes utiles sur le nœud : <code>curl -v https://api.open-meteo.com</code>,
        <code>nslookup api.open-meteo.com</code>, puis console › Sécurité › Gestion des certificats SSL et des clés › Magasins de clés › CellDefaultTrustStore › Certificats de signataire › Récupérer du port.</p>
    </div>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
