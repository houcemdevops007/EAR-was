<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, java.text.SimpleDateFormat, fr.efiacademy.lab.shop.*" %><%
    Order order = (Order) request.getAttribute("order");
    String pageTitle = "Commande confirmée";
    String pageDescription = "EFI Mini Shop : confirmation de la commande simulée, aucun paiement réel.";
    String appShort = "Mini Shop";
    String[][] navLinks = { {"Boutique", "/"}, {"Commander", "/checkout"}, {"Santé", "/api/health"} };
    String navActive = "";
    boolean useCsrf = true;
    String bodyClass = "app-shop page-confirmation";
    String[] pageScripts = { "/assets/js/shop.js" };
    SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy 'à' HH:mm:ss", Locale.FRANCE);
    df.setTimeZone(TimeZone.getTimeZone(ServerInfo.PARIS));
    boolean express = "express".equals(order.getShippingMode());
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <ol class="steps" aria-label="Étapes de la commande">
    <li class="done"><span>1</span>Panier</li>
    <li class="done"><span>2</span>Livraison</li>
    <li class="current" aria-current="step"><span>3</span>Confirmation</li>
  </ol>

  <section class="card confirm-hero" aria-labelledby="page-title">
    <div class="success-mark" aria-hidden="true">
      <svg viewBox="0 0 52 52"><circle class="sm-circle" cx="26" cy="26" r="24" fill="none"/><path class="sm-check" fill="none" d="M14 27l8 8 16-17"/></svg>
    </div>
    <h1 id="page-title">Merci <%= Html.esc(order.getName()) %>, votre commande est enregistrée !</h1>
    <p class="muted">Référence <strong class="order-ref"><%= Html.esc(order.getReference()) %></strong> ·
      passée le <%= Html.esc(df.format(new Date(order.getCreatedAt()))) %> (heure de Paris).</p>
    <div class="alert alert-warning simulation-note" role="note">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/><path d="M12 9v4M12 17h.01"/></svg>
      <div><strong>Simulation pédagogique :</strong> aucun paiement n'a été effectué, aucun e-mail n'a été envoyé
        et cette commande n'existe que dans votre session HTTP sur <code><%= Html.esc(ServerInfo.hostname()) %></code>.</div>
    </div>
  </section>

  <div class="checkout-layout">
    <section class="card" aria-labelledby="ship-title">
      <h2 id="ship-title">Livraison</h2>
      <dl class="info-list">
        <dt>Destinataire</dt><dd><%= Html.esc(order.getName()) %></dd>
        <dt>E-mail</dt><dd><%= Html.esc(order.getEmail()) %></dd>
        <dt>Adresse</dt><dd><%= Html.esc(order.getAddress()) %><br><%= Html.esc(order.getZip()) %> <%= Html.esc(order.getCity()) %></dd>
        <dt>Mode</dt><dd><%= express ? "Express · 24 h" : "Standard · 3 à 5 jours" %></dd>
        <dt>Articles</dt><dd><%= order.getItemCount() %></dd>
      </dl>
      <div class="row confirm-actions">
        <a class="btn btn-gradient btn-lg" href="<%= efiCtx %>/" id="back-to-shop">← Retour à la boutique</a>
        <button type="button" class="btn btn-ghost" id="print-order">Imprimer le récapitulatif</button>
      </div>
    </section>

    <aside class="card summary" aria-labelledby="summary-title">
      <h2 id="summary-title">Récapitulatif</h2>
      <ul class="summary-lines">
<%  for (String[] l : order.getLines()) { %>
        <li>
          <img src="<%= efiCtx %>/assets/img/products/<%= Html.esc(l[4]) %>.svg" alt="" width="64" height="48">
          <div><strong><%= Html.esc(l[0]) %></strong><span class="small muted">Quantité : <%= Html.esc(l[1]) %> × <%= Html.esc(l[2]) %></span></div>
          <span class="line-total"><%= Html.esc(l[3]) %></span>
        </li>
<%  } %>
      </ul>
      <dl class="totals">
        <dt>Sous-total</dt><dd><%= Html.esc(Money.format(order.getSubtotalCents())) %></dd>
        <dt>Livraison<%= express ? " express" : "" %></dt><dd><%= order.getShippingCents() == 0 ? "Offerte" : Html.esc(Money.format(order.getShippingCents())) %></dd>
        <dt class="grand">Total TTC</dt><dd class="grand"><%= Html.esc(Money.format(order.getTotalCents())) %></dd>
      </dl>
      <p class="small muted">Dont TVA (20 %) : <%= Html.esc(Money.format(Money.vatIncluded(order.getTotalCents()))) %>.</p>
    </aside>
  </div>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
