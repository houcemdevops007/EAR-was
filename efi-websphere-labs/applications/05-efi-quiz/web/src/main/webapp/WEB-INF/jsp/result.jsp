<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, fr.efiacademy.lab.quiz.*" %><%
    QuizSession quiz = (QuizSession) request.getAttribute("quiz");
    int score = quiz.getScore();
    int total = quiz.getTotal();
    int pct = quiz.getPercent();
    long dur = quiz.getDurationSeconds();
    String letters = "ABCDEFGH";
    String tone = pct >= 70 ? "good" : (pct >= 50 ? "mid" : "low");
    // Cercle de score : circonférence 2πr avec r = 54
    double circ = 2 * Math.PI * 54;
    double dash = circ * pct / 100.0;

    String pageTitle = "Résultat du quiz : " + score + " / " + total;
    String pageDescription = "Résultat et corrections détaillées du quiz WebSphere ND.";
    String appShort = "Quiz WebSphere";
    String[][] navLinks = { {"Accueil", "/"}, {"Quiz en cours", "/quiz"}, {"Santé", "/api/health"} };
    String navActive = "/quiz";
    boolean useCsrf = true;
    String bodyClass = "app-quiz page-result result-" + tone;
    String[] pageScripts = { "/assets/js/quiz.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container quiz-container">
  <section class="card result-hero" aria-labelledby="result-title">
    <div class="score-ring" aria-hidden="true">
      <svg viewBox="0 0 120 120">
        <defs><linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1"><stop offset="0" style="stop-color:var(--accent)"/><stop offset="1" style="stop-color:var(--accent-2)"/></linearGradient></defs>
        <circle cx="60" cy="60" r="54" class="ring-bg"/>
        <circle cx="60" cy="60" r="54" class="ring-fg" stroke="url(#ringGrad)"
                stroke-dasharray="<%= String.format(Locale.ROOT, "%.2f %.2f", dash, circ) %>" transform="rotate(-90 60 60)"/>
      </svg>
      <div class="ring-label"><strong data-count="<%= pct %>"><%= pct %></strong><span>%</span></div>
    </div>
    <div class="result-text">
      <span class="eyebrow eyebrow-dark"><%= QuestionBank.icon(quiz.getTheme()) %> <%= Html.esc(QuestionBank.label(quiz.getTheme())) %></span>
      <h1 id="result-title">Score : <%= score %> / <%= total %></h1>
      <p class="verdict"><%= Html.esc(QuizSession.verdict(pct)) %></p>
      <div class="row small muted">
        <span>⏱ Durée : <%= dur / 60 %> min <%= dur % 60 %> s</span>
        <span>✅ <%= score %> bonnes réponses</span>
        <span>❌ <%= total - score %> erreurs</span>
      </div>
      <div class="row result-actions">
        <form method="post" action="<%= efiCtx %>/quiz">
          <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
          <input type="hidden" name="action" value="start">
          <input type="hidden" name="theme" value="<%= Html.esc(quiz.getTheme()) %>">
          <input type="hidden" name="count" value="<%= total == QuestionBank.byTheme(quiz.getTheme()).size() ? 0 : (total <= 10 ? 10 : 20) %>">
          <button class="btn btn-gradient" type="submit" id="btn-retry">🔁 Recommencer ce thème</button>
        </form>
        <form method="post" action="<%= efiCtx %>/quiz">
          <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
          <input type="hidden" name="action" value="restart">
          <button class="btn btn-ghost" type="submit" id="btn-home">Choisir un autre thème</button>
        </form>
      </div>
    </div>
  </section>

  <section class="section" aria-labelledby="themes-title">
    <h2 id="themes-title">Réussite par thème</h2>
    <div class="grid theme-scores">
<%  for (Map.Entry<String, int[]> e : quiz.getScoreByTheme().entrySet()) {
        int[] v = e.getValue(); int tp = Math.round(v[0] * 100f / v[1]); %>
      <div class="card theme-score">
        <div class="row"><span class="theme-icon-sm" aria-hidden="true"><%= QuestionBank.icon(e.getKey()) %></span>
          <strong><%= Html.esc(QuestionBank.label(e.getKey())) %></strong><span class="spacer"></span>
          <span class="small"><strong><%= v[0] %></strong> / <%= v[1] %></span></div>
        <div class="quiz-progress slim" role="img" aria-label="<%= tp %> % de réussite"><span style="width:<%= tp %>%"></span></div>
      </div>
<%  } %>
    </div>
  </section>

  <section class="section" aria-labelledby="review-title">
    <div class="section-head">
      <div><h2 id="review-title">Correction détaillée</h2><p>Relisez chaque explication : elles reprennent les notions clés du guide formateur.</p></div>
      <div class="row" role="group" aria-label="Filtrer la correction">
        <button class="btn btn-ghost btn-sm" type="button" data-filter="all" aria-pressed="true">Toutes</button>
        <button class="btn btn-ghost btn-sm" type="button" data-filter="wrong" aria-pressed="false">Erreurs seulement</button>
      </div>
    </div>
    <ol class="review-list">
<%  for (int i = 0; i < total; i++) {
        Question q = quiz.getQuestionAt(i);
        int a = quiz.getAnswerAt(i);
        boolean good = a == q.getAnswer(); %>
      <li class="card review-item <%= good ? "good" : "bad" %>">
        <div class="review-head">
          <span class="review-num"><%= i + 1 %></span>
          <strong class="review-q"><%= Html.esc(q.getText()) %></strong>
          <span class="badge <%= good ? "badge-success" : "badge-danger" %>"><%= good ? "Correct" : "Incorrect" %></span>
        </div>
        <ul class="review-choices">
<%      for (int c = 0; c < q.getChoiceCount(); c++) {
            String cls = c == q.getAnswer() ? "correct" : (c == a ? "wrong" : ""); %>
          <li class="<%= cls %>"><span class="choice-letter sm"><%= letters.charAt(c) %></span><%= Html.esc(q.getChoice(c)) %>
<%          if (c == a) { %><em class="small"> — votre réponse</em><% } %></li>
<%      } %>
        </ul>
        <p class="review-expl">💡 <%= Html.esc(q.getExplanation()) %></p>
      </li>
<%  } %>
    </ol>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
