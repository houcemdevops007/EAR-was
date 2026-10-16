<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="java.util.*, fr.efiacademy.lab.shop.*" %><%
    String pageTitle = "Boutique de démonstration";
    String pageDescription = "EFI Mini Shop : catalogue fictif, recherche, filtres, panier en session HTTP et commande simulée — aucun paiement réel.";
    String appShort = "Mini Shop";
    String[][] navLinks = { {"Boutique", "/"}, {"Commander", "/checkout"}, {"API catalogue", "/api/catalog"}, {"Santé", "/api/health"} };
    String navActive = "/";
    boolean useCsrf = true;
    String bodyClass = "app-shop";
    String[] pageScripts = { "/assets/js/shop.js" };
    List<Product> products = Catalog.all();
    Cart cart = ShopSupport.cart(request.getSession(true));
    boolean emptyNotice = "vide".equals(request.getParameter("panier"));
%><%@ include file="/WEB-INF/jspf/top.jspf" %>
<div class="container">
  <section class="page-hero shop-hero" aria-labelledby="page-title">
    <div class="shop-hero-grid">
      <div>
        <span class="eyebrow">Application 10 · Sessions &amp; logique métier</span>
        <h1 id="page-title">La boutique du labo EFI</h1>
        <p>Catalogue <strong>fictif</strong> de <%= products.size() %> produits. Votre panier vit dans votre <strong>session HTTP</strong>
          sur le serveur <code class="code-light"><%= Html.esc(ServerInfo.hostname()) %></code> : changez de serveur sans affinité de session et il disparaît !</p>
        <div class="hero-perks">
          <span>🚚 Livraison offerte dès 100 €</span><span>🔒 Aucun paiement réel</span><span>🧪 Données de démonstration</span>
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <img src="<%= efiCtx %>/assets/img/products/server-rack.svg" alt="" class="hv hv-1" width="200" height="150">
        <img src="<%= efiCtx %>/assets/img/products/book.svg" alt="" class="hv hv-2" width="160" height="120">
        <img src="<%= efiCtx %>/assets/img/products/mug.svg" alt="" class="hv hv-3" width="140" height="105">
      </div>
    </div>
  </section>

<%  if (emptyNotice) { %>
  <div class="alert alert-info" role="status" style="margin-bottom:1.25rem">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
    <div>Votre panier est vide : ajoutez au moins un article avant de commander.</div>
  </div>
<%  } %>

  <section class="toolbar card" aria-label="Recherche et filtres">
    <div class="search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
      <label class="sr-only" for="search">Rechercher un produit</label>
      <input class="input" id="search" type="search" placeholder="Rechercher un produit…" autocomplete="off">
    </div>
    <div class="chips" role="group" aria-label="Catégories">
      <button type="button" class="chip" data-category="" aria-pressed="true">Tout</button>
<%  for (Map.Entry<String, String> c : Catalog.categories().entrySet()) { %>
      <button type="button" class="chip" data-category="<%= Html.esc(c.getKey()) %>" aria-pressed="false"><%= Html.esc(c.getValue()) %></button>
<%  } %>
    </div>
    <label class="sr-only" for="sort">Trier</label>
    <select class="select" id="sort">
      <option value="">Pertinence</option>
      <option value="price-asc">Prix croissant</option>
      <option value="price-desc">Prix décroissant</option>
      <option value="name">Nom (A → Z)</option>
      <option value="rating">Mieux notés</option>
    </select>
  </section>
  <p class="small muted" id="result-count" aria-live="polite"><%= products.size() %> produit(s)</p>

  <section class="product-grid" id="product-grid" aria-label="Catalogue">
<%  int idx = 0;
    for (Product p : products) { idx++; %>
    <article class="product" data-id="<%= p.getId() %>" data-category="<%= Html.esc(p.getCategory()) %>"
             data-name="<%= Html.esc(Catalog.fold(p.getName() + " " + p.getDescription())) %>" data-price="<%= p.getPriceCents() %>"
             data-rating="<%= p.getRating() %>" data-order="<%= idx %>">
      <div class="product-media">
        <img src="<%= efiCtx %>/assets/img/products/<%= Html.esc(p.getImage()) %>.svg" alt="Illustration : <%= Html.esc(p.getName()) %>" width="400" height="300" loading="lazy">
<%      if (p.getBadge() != null) { %><span class="product-badge"><%= Html.esc(p.getBadge()) %></span><% } %>
      </div>
      <div class="product-body">
        <span class="product-cat"><%= Html.esc(Catalog.categoryLabel(p.getCategory())) %></span>
        <h2 class="product-name"><%= Html.esc(p.getName()) %></h2>
        <p class="product-desc"><%= Html.esc(p.getDescription()) %></p>
        <div class="product-meta">
          <span class="stars" aria-label="Note : <%= String.valueOf(p.getRating()).replace('.', ',') %> sur 5"><span style="--r:<%= p.getRating() %>"></span></span>
          <span class="small muted"><%= p.getStock() < 10 ? "Plus que " + p.getStock() + " en stock" : "En stock" %></span>
        </div>
        <div class="product-foot">
          <strong class="price"><%= Html.esc(Money.format(p.getPriceCents())) %></strong>
          <button type="button" class="btn btn-gradient add-btn" data-add="<%= p.getId() %>" aria-label="Ajouter « <%= Html.esc(p.getName()) %> » au panier">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6"/></svg>
            Ajouter
          </button>
        </div>
      </div>
    </article>
<%  } %>
  </section>
  <div class="card empty-state" id="no-results" hidden>Aucun produit ne correspond à votre recherche.</div>
</div>

<button type="button" class="cart-fab" id="cart-fab" aria-controls="cart-drawer" aria-expanded="false">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="20" r="1.5"/><circle cx="18" cy="20" r="1.5"/><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6"/></svg>
  <span class="sr-only">Ouvrir le panier, articles : </span><span class="cart-count" id="cart-count"><%= cart.getCount() %></span>
</button>

<div class="drawer-backdrop" id="drawer-backdrop" hidden></div>
<aside class="cart-drawer" id="cart-drawer" aria-labelledby="cart-title" aria-hidden="true" tabindex="-1">
  <div class="drawer-head">
    <h2 id="cart-title">Mon panier</h2>
    <span class="badge" id="drawer-count">0 article</span>
    <span class="spacer"></span>
    <button type="button" class="icon-btn" id="drawer-close" aria-label="Fermer le panier"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg></button>
  </div>
  <div class="free-ship" id="free-ship" hidden></div>
  <ul class="cart-lines" id="cart-lines"></ul>
  <div class="cart-empty" id="cart-empty">
    <span aria-hidden="true">🛒</span>
    <p>Votre panier est vide.</p>
  </div>
  <div class="drawer-foot" id="drawer-foot">
    <dl class="totals">
      <dt>Sous-total</dt><dd id="t-subtotal">–</dd>
      <dt>Livraison</dt><dd id="t-shipping">–</dd>
      <dt class="grand">Total TTC</dt><dd class="grand" id="t-total">–</dd>
    </dl>
    <p class="small muted" id="t-vat"></p>
    <a class="btn btn-gradient btn-lg checkout-btn" href="<%= efiCtx %>/checkout" id="checkout-btn">Valider ma commande →</a>
    <button type="button" class="btn btn-ghost btn-sm" id="clear-cart">Vider le panier</button>
  </div>
</aside>
<%@ include file="/WEB-INF/jspf/bottom.jspf" %>
