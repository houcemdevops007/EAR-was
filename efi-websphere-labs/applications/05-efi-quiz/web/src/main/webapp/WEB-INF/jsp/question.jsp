<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="fr.efiacademy.lab.quiz.*" %><%
    QuizSession quiz = (QuizSession) request.getAttribute("quiz");
    String flash = (String) request.getAttribute("flash");
    Question q = quiz.getCurrentQuestion();
    int idx = quiz.getCurrentIndex();
    int total = quiz.getTotal();
    boolean fb = quiz.isFeedback();
    int given = quiz.getCurrentAnswer();
    boolean ok = fb && given == q.getAnswer();
    int done = idx + (fb ? 1 : 0);
    int pct = total == 0 ? 0 : Math.round(done * 100f / total);
    String letters = "ABCDEFGH";

    String pageTitle = "Question " + (idx + 1) + " sur " + total;
    String pageDescription = "Quiz WebSphere ND : question " + (idx + 1) + " sur " + total + ".";
    String appShort = "Quiz WebSphere";
    String[][] navLinks = { {"Accueil", "/"}, {"Quiz en cours", "/quiz"}, {"Santé", "/api/health"} };
    String navActive = "/quiz";
    boolean useCsrf = true;
    String bodyClass = "app-quiz page-question";
    String[] pageScripts = { "/assets/js/quiz.js" };
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container quiz-container">
  <div class="quiz-top">
    <div class="row">
      <span class="badge badge-info"><%= QuestionBank.icon(q.getTheme()) %> <%= Html.esc(QuestionBank.label(q.getTheme())) %></span>
      <span class="spacer"></span>
      <span class="small muted">Score provisoire : <strong id="live-score"><%= quiz.getScore() %></strong> / <%= done %></span>
    </div>
    <div class="quiz-progress" role="progressbar" aria-label="Progression du quiz" aria-valuemin="0" aria-valuemax="<%= total %>" aria-valuenow="<%= done %>">
      <span style="width:<%= pct %>%"></span>
    </div>
    <div class="row small muted"><span>Question <strong><%= idx + 1 %></strong> sur <%= total %></span><span class="spacer"></span><span><%= pct %> % effectué</span></div>
  </div>

<%  if (flash != null) { %>
  <div class="alert alert-warning" role="alert" style="margin-bottom:1rem">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 9v4M12 17h.01"/><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/></svg>
    <div><%= Html.esc(flash) %></div>
  </div>
<%  } %>

  <form class="card question-card<%= fb ? (ok ? " is-correct" : " is-wrong") : "" %>" method="post" action="<%= efiCtx %>/quiz" id="question-form" novalidate>
    <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
    <input type="hidden" name="action" value="<%= fb ? "next" : "answer" %>">
    <fieldset class="choices-fieldset"<%= fb ? " disabled" : "" %>>
      <legend><h1 class="question-text" id="question-text"><%= Html.esc(q.getText()) %></h1></legend>
      <div class="choices">
<%  for (int i = 0; i < q.getChoiceCount(); i++) {
        String cls = "choice";
        if (fb && i == q.getAnswer()) { cls += " correct"; }
        else if (fb && i == given) { cls += " wrong"; } %>
        <label class="<%= cls %>" data-key="<%= i + 1 %>">
          <input type="radio" name="choice" value="<%= i %>"<%= i == given ? " checked" : "" %> required>
          <span class="choice-letter" aria-hidden="true"><%= letters.charAt(i) %></span>
          <span class="choice-text"><%= Html.esc(q.getChoice(i)) %></span>
<%      if (fb && i == q.getAnswer()) { %><span class="choice-mark" aria-label="Bonne réponse">✓</span><% }
        else if (fb && i == given) { %><span class="choice-mark" aria-label="Votre réponse, incorrecte">✗</span><% } %>
        </label>
<%  } %>
      </div>
    </fieldset>

<%  if (fb) { %>
    <div class="feedback <%= ok ? "ok" : "ko" %>" role="status" aria-live="polite">
      <div class="feedback-head"><span class="feedback-icon" aria-hidden="true"><%= ok ? "🎉" : "💡" %></span>
        <strong><%= ok ? "Bonne réponse !" : "Réponse incorrecte" %></strong>
<%      if (!ok) { %><span class="small">La bonne réponse était <strong><%= letters.charAt(q.getAnswer()) %></strong>.</span><% } %>
      </div>
      <p><%= Html.esc(q.getExplanation()) %></p>
    </div>
<%  } %>

    <div class="row question-actions">
      <span class="small muted kbd-hint">Astuce : touches <kbd>1</kbd>–<kbd><%= q.getChoiceCount() %></kbd> puis <kbd>Entrée</kbd></span>
      <span class="spacer"></span>
<%  if (fb) { %>
      <button class="btn btn-gradient btn-lg" type="submit" id="btn-next" autofocus><%= idx + 1 >= total ? "Voir mon score 🏁" : "Question suivante →" %></button>
<%  } else { %>
      <button class="btn btn-gradient btn-lg" type="submit" id="btn-answer">Valider ma réponse</button>
<%  } %>
    </div>
  </form>

  <form method="post" action="<%= efiCtx %>/quiz" class="abandon-form">
    <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
    <input type="hidden" name="action" value="restart">
    <button class="btn btn-ghost btn-sm" type="submit" data-confirm="Abandonner ce quiz ? Votre progression sera perdue.">Abandonner le quiz</button>
  </form>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
