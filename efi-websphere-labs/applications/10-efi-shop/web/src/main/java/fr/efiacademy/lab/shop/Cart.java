package fr.efiacademy.lab.shop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panier stocké dans la session HTTP (Serializable : compatible avec la
 * persistance / réplication de session WebSphere). Ne contient que des
 * identifiants et des quantités ; les prix sont relus dans le catalogue.
 */
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "shop.cart";
    public static final int MAX_QTY = 10;
    public static final int MAX_LINES = 20;
    public static final long FREE_SHIPPING_CENTS = 10000;
    public static final long SHIPPING_CENTS = 490;

    private final LinkedHashMap<String, Integer> items = new LinkedHashMap<String, Integer>();

    /** Ligne calculée du panier. */
    public static final class Line implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Product product;
        private final int quantity;

        Line(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getTotalCents() {
            return product.getPriceCents() * quantity;
        }
    }

    /** Ajoute une quantité (plafonnée) ; retourne la nouvelle quantité. */
    public synchronized int add(String productId, int qty) {
        Product p = require(productId);
        if (qty < 1) {
            throw new IllegalArgumentException("Quantité invalide.");
        }
        Integer cur = items.get(p.getId());
        if (cur == null && items.size() >= MAX_LINES) {
            throw new IllegalStateException("Panier plein (" + MAX_LINES + " articles différents maximum).");
        }
        int n = Math.min(maxFor(p), (cur == null ? 0 : cur.intValue()) + qty);
        items.put(p.getId(), Integer.valueOf(n));
        return n;
    }

    /** Fixe la quantité (0 = suppression). */
    public synchronized int set(String productId, int qty) {
        Product p = require(productId);
        if (qty < 0) {
            throw new IllegalArgumentException("Quantité invalide.");
        }
        if (qty == 0) {
            items.remove(p.getId());
            return 0;
        }
        if (!items.containsKey(p.getId()) && items.size() >= MAX_LINES) {
            throw new IllegalStateException("Panier plein (" + MAX_LINES + " articles différents maximum).");
        }
        int n = Math.min(maxFor(p), qty);
        items.put(p.getId(), Integer.valueOf(n));
        return n;
    }

    public synchronized boolean remove(String productId) {
        return items.remove(productId) != null;
    }

    public synchronized void clear() {
        items.clear();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized int getCount() {
        int c = 0;
        for (Integer q : items.values()) {
            c += q.intValue();
        }
        return c;
    }

    public synchronized List<Line> getLines() {
        List<Line> out = new ArrayList<Line>();
        for (Map.Entry<String, Integer> e : items.entrySet()) {
            Product p = Catalog.get(e.getKey());
            if (p != null) {
                out.add(new Line(p, e.getValue().intValue()));
            }
        }
        return Collections.unmodifiableList(out);
    }

    public long getSubtotalCents() {
        long t = 0;
        for (Line l : getLines()) {
            t += l.getTotalCents();
        }
        return t;
    }

    public long getShippingCents() {
        long s = getSubtotalCents();
        return s == 0 || s >= FREE_SHIPPING_CENTS ? 0 : SHIPPING_CENTS;
    }

    public long getTotalCents() {
        return getSubtotalCents() + getShippingCents();
    }

    private static int maxFor(Product p) {
        return Math.max(0, Math.min(MAX_QTY, p.getStock()));
    }

    private static Product require(String id) {
        Product p = Catalog.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Produit inconnu.");
        }
        return p;
    }
}
