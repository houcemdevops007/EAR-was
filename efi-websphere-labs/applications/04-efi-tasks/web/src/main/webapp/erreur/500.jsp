<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" isErrorPage="true" %><%
    response.setStatus(500);
    // Référence courte pour retrouver l'erreur dans SystemOut.log / SystemErr.log
    String errRef = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    Throwable errCause = exception;
    if (errCause == null) {
        Object o = request.getAttribute("javax.servlet.error.exception");
        if (o instanceof Throwable) {
            errCause = (Throwable) o;
        }
    }
    application.log("[EFI] Erreur interne ref=" + errRef + " uri="
            + request.getAttribute("javax.servlet.error.request_uri"), errCause);
    String pageTitle = "Erreur interne";
    String pageDescription = "Une erreur inattendue est survenue.";
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
      <div class="error-code" aria-hidden="true">500</div>
      <h1>Erreur interne du serveur</h1>
      <p>Une erreur inattendue est survenue. Aucun détail technique n'est affiché pour des raisons de sécurité.
        Communiquez la référence ci-dessous à votre formateur : elle figure dans les journaux
        <code>SystemOut.log</code> / <code>SystemErr.log</code> du serveur.</p>
      <p><span class="badge badge-danger">Référence <%= Html.esc(errRef) %></span></p>
      <a class="btn btn-gradient" href="<%= efiCtx %>/">Retour à l'accueil</a>
    </div>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
