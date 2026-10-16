<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, fr.efiacademy.lab.shop.*" %><%
    Cart cart = (Cart) request.getAttribute("cart");
    @SuppressWarnings("unchecked") Map<String, String> form = (Map<String, String>) request.getAttribute("form");
    @SuppressWarnings("unchecked") Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    String pageTitle = "Validation de la commande";
    String pageDescription = "EFI Mini Shop : saisie de l'adresse de livraison et récapitulatif de la commande simulée.";
    String appShort = "Mini Shop";
    String[][] navLinks = { {"Boutique", "/"}, {"Commander", "/checkout"}, {"Santé", "/api/health"} };
    String navActive = "/checkout";
    boolean useCsrf = true;
    String bodyClass = "app-shop page-checkout";
    String[] pageScripts = { "/assets/js/shop.js" };
    long sub = cart.getSubtotalCents();
    long ship = cart.getShippingCents();
    boolean express = "express".equals(form.get("shipping"));
%><%@ include file="/WEB-INF/jspf/top.jspf" %><%!
    /** Attributs ARIA et message d'erreur d'un champ. */
    private static String invalid(Map<String, String> errors, String field) {
        return errors.containsKey(field) ? " aria-invalid=\"true\"" : "";
    }
%>
<div class="container">
  <ol class="steps" aria-label="Étapes de la commande">
    <li class="done"><span>1</span>Panier</li>
    <li class="current" aria-current="step"><span>2</span>Livraison</li>
    <li><span>3</span>Confirmation</li>
  </ol>

  <div class="checkout-layout">
    <form class="card checkout-form" method="post" action="<%= efiCtx %>/checkout" novalidate id="checkout-form">
      <input type="hidden" name="_csrf" value="<%= Html.esc(efiCsrf) %>">
      <h1>Adresse de livraison</h1>
      <p class="muted">Commande <strong>entièrement simulée</strong> : aucune donnée n'est transmise ni conservée après la fin de votre session.</p>
<%  if (!errors.isEmpty()) { %>
      <div class="alert alert-danger" role="alert" tabindex="-1" id="error-summary">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
        <div><strong>Le formulaire contient <%= errors.size() %> erreur(s) :</strong>
          <ul class="error-list">
<%      for (Map.Entry<String, String> e : errors.entrySet()) { %>
            <li><a href="#f-<%= Html.esc(e.getKey()) %>"><%= Html.esc(e.getValue()) %></a></li>
<%      } %>
          </ul></div>
      </div>
<%  } %>
      <div class="field">
        <label for="f-name">Nom complet <span aria-hidden="true">*</span></label>
        <input class="input" id="f-name" name="name" autocomplete="name" required maxlength="80" value="<%= Html.esc(form.get("name")) %>"<%= invalid(errors, "name") %> aria-describedby="e-name">
        <span class="error-text" id="e-name"><%= Html.esc(Html.orDefault(errors.get("name"), "")) %></span>
      </div>
      <div class="field">
        <label for="f-email">E-mail <span aria-hidden="true">*</span></label>
        <input class="input" id="f-email" name="email" type="email" autocomplete="email" required maxlength="254" value="<%= Html.esc(form.get("email")) %>"<%= invalid(errors, "email") %> aria-describedby="e-email">
        <span class="error-text" id="e-email"><%= Html.esc(Html.orDefault(errors.get("email"), "")) %></span>
      </div>
      <div class="field">
        <label for="f-address">Adresse <span aria-hidden="true">*</span></label>
        <input class="input" id="f-address" name="address" autocomplete="street-address" required maxlength="120" value="<%= Html.esc(form.get("address")) %>"<%= invalid(errors, "address") %> aria-describedby="e-address">
        <span class="error-text" id="e-address"><%= Html.esc(Html.orDefault(errors.get("address"), "")) %></span>
      </div>
      <div class="grid grid-2">
        <div class="field">
          <label for="f-zip">Code postal <span aria-hidden="true">*</span></label>
          <input class="input" id="f-zip" name="zip" inputmode="numeric" autocomplete="postal-code" required maxlength="5" pattern="\d{5}" value="<%= Html.esc(form.get("zip")) %>"<%= invalid(errors, "zip") %> aria-describedby="e-zip">
          <span class="error-text" id="e-zip"><%= Html.esc(Html.orDefault(errors.get("zip"), "")) %></span>
        </div>
        <div class="field">
          <label for="f-city">Ville <span aria-hidden="true">*</span></label>
          <input class="input" id="f-city" name="city" autocomplete="address-level2" required maxlength="60" value="<%= Html.esc(form.get("city")) %>"<%= invalid(errors, "city") %> aria-describedby="e-city">
          <span class="error-text" id="e-city"><%= Html.esc(Html.orDefault(errors.get("city"), "")) %></span>
        </div>
      </div>
      <fieldset class="ship-options">
        <legend>Mode de livraison</legend>
        <label class="ship-option"><input type="radio" name="shipping" value="standard"<%= express ? "" : " checked" %>>
          <span><strong>Standard</strong> · 3 à 5 jours <em><%= ship == 0 ? "offerte" : Html.esc(Money.format(ship)) %></em></span></label>
        <label class="ship-option"><input type="radio" name="shipping" value="express"<%= express ? " checked" : "" %>>
          <span><strong>Express</strong> · 24 h <em>+ <%= Html.esc(Money.format(900)) %></em></span></label>
      </fieldset>
      <div class="field">
        <label class="check terms"><input type="checkbox" name="terms" id="f-terms" value="on"<%= "on".equals(form.get("terms")) ? " checked" : "" %><%= invalid(errors, "terms") %> aria-describedby="e-terms">
          Je comprends qu'il s'agit d'une <strong>commande fictive</strong> sans paiement.</label>
        <span class="error-text" id="e-terms"><%= Html.esc(Html.orDefault(errors.get("terms"), "")) %></span>
      </div>
      <div class="row">
        <a class="btn btn-ghost" href="<%= efiCtx %>/">← Continuer mes achats</a>
        <span class="spacer"></span>
        <button class="btn btn-gradient btn-lg" type="submit" id="place-order">Confirmer la commande</button>
      </div>
    </form>

    <aside class="card summary" aria-labelledby="summary-title">
      <h2 id="summary-title">Récapitulatif</h2>
      <ul class="summary-lines">
<%  for (Cart.Line l : cart.getLines()) { %>
        <li>
          <img src="<%= efiCtx %>/assets/img/products/<%= Html.esc(l.getProduct().getImage()) %>.svg" alt="" width="64" height="48">
          <div><strong><%= Html.esc(l.getProduct().getName()) %></strong><span class="small muted">Quantité : <%= l.getQuantity() %> × <%= Html.esc(Money.format(l.getProduct().getPriceCents())) %></span></div>
          <span class="line-total"><%= Html.esc(Money.format(l.getTotalCents())) %></span>
        </li>
<%  } %>
      </ul>
      <dl class="totals">
        <dt>Sous-total</dt><dd><%= Html.esc(Money.format(sub)) %></dd>
        <dt>Livraison standard</dt><dd><%= ship == 0 ? "Offerte" : Html.esc(Money.format(ship)) %></dd>
        <dt class="grand">Total TTC</dt><dd class="grand"><%= Html.esc(Money.format(cart.getTotalCents())) %></dd>
      </dl>
      <p class="small muted">Dont TVA (20 %) : <%= Html.esc(Money.format(Money.vatIncluded(cart.getTotalCents()))) %>. Option express ajoutée à la confirmation.</p>
    </aside>
  </div>
</div>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
