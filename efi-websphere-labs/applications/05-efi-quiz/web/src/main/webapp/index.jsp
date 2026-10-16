<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, java.text.SimpleDateFormat, fr.efiacademy.lab.quiz.*" %><%
    String pageTitle = "Quiz WebSphere ND";
    String pageDescription = "EFI Quiz WebSphere : plus de 30 questions sur le DMGR, les Node Agents, la fédération, EAR/WAR, SSL, la synchronisation et le déploiement.";
    String appShort = "Quiz WebSphere";
    String[][] navLinks = { {"Accueil", "/"}, {"Quiz en cours", "/quiz"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = true;
    String bodyClass = "app-quiz";
    String[] pageScripts = { "/assets/js/quiz.js" };
    String flash = QuizServlet.consumeFlashPublic(session);
    QuizSession running = (QuizSession) session.getAttribute(QuizServlet.CURRENT);
    @SuppressWarnings("unchecked")
    List<HistoryEntry> history = (List<HistoryEntry>) session.getAttribute(QuizServlet.HISTORY);
    Map<String, String[]> themes = QuestionBank.themes();
    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
    df.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="page-hero quiz-hero" aria-labelledby="page-title">
    <div class="quiz-hero-grid">
      <div>
        <span class="eyebrow">Application 05 · Sessions &amp; formulaires</span>
        <h1 id="page-title">Testez vos connaissances WebSphere ND</h1>
        <p><strong><%= QuestionBank.all().size() %> questions</strong> réparties sur <%= themes.size() %> thèmes, une question à la fois,
          avec correction expliquée. Votre progression est conservée dans votre <strong>session HTTP</strong>.</p>
      </div>
      <div class="hero-badges" aria-hidden="true">
<%  for (Map.Entry<String, String[]> t : themes.entrySet()) { %>
        <span class="floating-badge"><%= t.getValue()[1] %> <%= Html.esc(t.getValue()[0]) %></span>
<%  } %>
      </div>
    </div>
  </section>

<%  if (flash != null) { %>
  <div class="alert alert-warning" role="alert" style="margin-bottom:1.25rem">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/></svg>
    <div><%= Html.esc(flash) %></div>
  </div>
<%  } %>

<%  if (running != null && !running.isFinished()) { %>
  <div class="card resume-card">
    <div class="row">
      <span class="resume-icon" aria-hidden="true"><%= QuestionBank.icon(running.getTheme()) %></span>
      <div>
        <strong>Quiz en cours : <%= Html.esc(QuestionBank.label(running.getTheme())) %></strong>
        <div class="small muted">Question <%= running.getCurrentIndex() + 1 %> sur <%= running.getTotal() %> · score provisoire <%= running.getScore() %></div>
      </div>
      <span class="spacer"></span>
      <a class="btn btn-gradient" href="<%= efiCtx %>/quiz">Reprendre →</a>
    </div>
  </div>
<%  } %>

  <form class="card start-form" method="post" action="<%= efiCtx %>/quiz" id="start-form" novalidate>
    <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
    <input type="hidden" name="action" value="start">
    <fieldset class="theme-fieldset">
      <legend><h2>1. Choisissez un thème</h2></legend>
      <div class="theme-grid">
        <label class="theme-card">
          <input type="radio" name="theme" value="all" checked>
          <span class="theme-body"><span class="theme-icon">🎯</span><strong>Tous les thèmes</strong>
            <span class="small muted"><%= QuestionBank.all().size() %> questions</span></span>
        </label>
<%  for (Map.Entry<String, String[]> t : themes.entrySet()) { %>
        <label class="theme-card">
          <input type="radio" name="theme" value="<%= Html.esc(t.getKey()) %>">
          <span class="theme-body"><span class="theme-icon"><%= t.getValue()[1] %></span><strong><%= Html.esc(t.getValue()[0]) %></strong>
            <span class="small muted"><%= QuestionBank.byTheme(t.getKey()).size() %> questions</span></span>
        </label>
<%  } %>
      </div>
    </fieldset>
    <div class="row start-row">
      <div class="field" style="margin:0">
        <label for="count"><h2 style="margin:0">2. Nombre de questions</h2></label>
        <select class="select" id="count" name="count">
          <option value="10" selected>10 questions</option>
          <option value="20">20 questions</option>
          <option value="0">Toutes les questions du thème</option>
        </select>
      </div>
      <span class="spacer"></span>
      <button class="btn btn-gradient btn-lg" type="submit">Commencer le quiz 🚀</button>
    </div>
  </form>

  <section class="section" aria-labelledby="hist-title">
    <div class="section-head">
      <div><h2 id="hist-title">Historique de la session</h2>
        <p>Les tentatives sont conservées dans votre session HTTP (perdues à son expiration ou à la fermeture du navigateur).</p></div>
<%  if (history != null && !history.isEmpty()) { %>
      <form method="post" action="<%= efiCtx %>/quiz">
        <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
        <input type="hidden" name="action" value="clear-history">
        <button class="btn btn-ghost btn-sm" type="submit">Effacer l'historique</button>
      </form>
<%  } %>
    </div>
<%  if (history == null || history.isEmpty()) { %>
    <div class="card empty-state">Aucune tentative pour l'instant. Lancez votre premier quiz !</div>
<%  } else { %>
    <div class="table-wrap">
      <table class="table">
        <thead><tr><th scope="col">Date</th><th scope="col">Thème</th><th scope="col">Score</th><th scope="col">Réussite</th><th scope="col">Durée</th></tr></thead>
        <tbody>
<%      for (HistoryEntry h : history) { int pct = h.getPercent(); %>
          <tr>
            <td><%= df.format(new Date(h.getDate())) %></td>
            <td><%= QuestionBank.icon(h.getTheme()) %> <%= Html.esc(QuestionBank.label(h.getTheme())) %></td>
            <td><strong><%= h.getScore() %></strong> / <%= h.getTotal() %></td>
            <td><span class="badge <%= pct >= 70 ? "badge-success" : (pct >= 50 ? "badge-warning" : "badge-danger") %>"><%= pct %> %</span></td>
            <td><%= h.getDurationSeconds() / 60 %> min <%= h.getDurationSeconds() % 60 %> s</td>
          </tr>
<%      } %>
        </tbody>
      </table>
    </div>
<%  } %>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
