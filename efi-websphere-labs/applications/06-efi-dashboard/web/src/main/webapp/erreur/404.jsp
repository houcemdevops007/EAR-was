<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" isErrorPage="true" %><%
    response.setStatus(404);
    String pageTitle = "Page introuvable";
    String pageDescription = "La ressource demandée n'existe pas.";
    String appShort = application.getInitParameter("efi.app.short");
    String[][] navLinks = { {"Accueil", "/"} };
    String navActive = "";
    boolean useCsrf = false;
    String bodyClass = "error";
    String[] pageScripts = {};
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="error-page reveal">
    <div>
      <div class="error-code" aria-hidden="true">404</div>
      <h1>Page introuvable</h1>
      <p>La ressource demandée n'existe pas dans cette application. Vérifiez l'URL et la racine de contexte
        <code><%= Html.esc(efiCtx) %></code> configurée lors du déploiement.</p>
      <div class="row" style="justify-content:center">
        <a class="btn btn-gradient" href="<%= efiCtx %>/">Retour à l'accueil</a>
        <a class="btn btn-ghost" href="<%= efiCtx %>/api/health">Tester /api/health</a>
      </div>
    </div>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
