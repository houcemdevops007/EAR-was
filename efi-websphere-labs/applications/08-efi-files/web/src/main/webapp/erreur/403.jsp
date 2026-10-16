<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" session="false" isErrorPage="true" %><%
    response.setStatus(403);
    String pageTitle = "Accès refusé";
    String pageDescription = "L'accès à cette ressource est réservé au réseau du laboratoire ou aux utilisateurs autorisés.";
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
      <div class="error-code" aria-hidden="true">403</div>
      <h1>Accès refusé</h1>
      <p>Le centre de fichiers est réservé à l'<strong>environnement de laboratoire</strong> : votre adresse réseau
        ne fait pas partie des réseaux autorisés, ou votre compte ne possède pas le rôle <code>FileCenterUser</code>.</p>
      <p class="small muted">Formateur : vérifiez le paramètre <code>efi.files.allowedNetworks</code> (web.xml ou propriété JVM)
        et le mappage des rôles de sécurité de l'application dans la console d'administration.</p>
      <div class="row" style="justify-content:center">
        <a class="btn btn-ghost" href="<%= efiCtx %>/api/health">Tester /api/health</a>
      </div>
    </div>
  </section>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
