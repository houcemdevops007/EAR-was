package fr.efiacademy.lab.shop;

import javax.servlet.http.HttpSession;

import fr.efiacademy.lab.common.JsonWriter;

/** Accès au panier de session et sérialisation JSON. */
public final class ShopSupport {

    private ShopSupport() {
    }

    public static Cart cart(HttpSession session) {
        synchronized (session) {
            Cart c = (Cart) session.getAttribute(Cart.SESSION_KEY);
            if (c == null) {
                c = new Cart();
                session.setAttribute(Cart.SESSION_KEY, c);
            }
            return c;
        }
    }

    /** Ré-enregistre l'attribut pour signaler la modification (réplication de session). */
    public static void touch(HttpSession session, Cart cart) {
        session.setAttribute(Cart.SESSION_KEY, cart);
    }

    public static void product(JsonWriter w, Product p) {
        w.beginObject()
                .field("id", p.getId())
                .field("name", p.getName())
                .field("category", p.getCategory())
                .field("categoryLabel", Catalog.categoryLabel(p.getCategory()))
                .field("priceCents", p.getPriceCents())
                .field("price", Money.format(p.getPriceCents()))
                .field("description", p.getDescription())
                .field("image", "assets/img/products/" + p.getImage() + ".svg")
                .field("badge", p.getBadge())
                .field("rating", p.getRating())
                .field("stock", p.getStock())
                .endObject();
    }

    public static String cartJson(Cart cart) {
        JsonWriter w = new JsonWriter().beginObject();
        w.name("lines").beginArray();
        for (Cart.Line l : cart.getLines()) {
            w.beginObject()
                    .field("productId", l.getProduct().getId())
                    .field("name", l.getProduct().getName())
                    .field("image", "assets/img/products/" + l.getProduct().getImage() + ".svg")
                    .field("quantity", l.getQuantity())
                    .field("maxQuantity", Math.min(Cart.MAX_QTY, l.getProduct().getStock()))
                    .field("unitPrice", Money.format(l.getProduct().getPriceCents()))
                    .field("totalCents", l.getTotalCents())
                    .field("total", Money.format(l.getTotalCents()))
                    .endObject();
        }
        w.endArray();
        long sub = cart.getSubtotalCents();
        long ship = cart.getShippingCents();
        w.field("count", cart.getCount())
                .field("subtotalCents", sub)
                .field("subtotal", Money.format(sub))
                .field("shippingCents", ship)
                .field("shipping", ship == 0 ? (sub == 0 ? Money.format(0) : "Offerte") : Money.format(ship))
                .field("freeShippingRemaining", sub > 0 && sub < Cart.FREE_SHIPPING_CENTS
                        ? Money.format(Cart.FREE_SHIPPING_CENTS - sub) : "")
                .field("totalCents", cart.getTotalCents())
                .field("total", Money.format(cart.getTotalCents()))
                .field("vat", Money.format(Money.vatIncluded(cart.getTotalCents())));
        return w.endObject().toString();
    }
}
