package fr.efiacademy.lab.shop;

import java.io.Serializable;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Commande simulée : instantané figé du panier + coordonnées validées.
 * Aucune donnée n'est transmise ni persistée : la commande n'existe que dans la session.
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String SESSION_KEY = "shop.lastOrder";
    private static final Random RANDOM = new SecureRandom();
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s<>\"']{1,64}@[A-Za-z0-9.-]{1,190}\\.[A-Za-z]{2,24}$");
    private static final Pattern ZIP = Pattern.compile("^\\d{5}$");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final String reference;
    private final long createdAt;
    private final String name;
    private final String email;
    private final String address;
    private final String zip;
    private final String city;
    private final String shippingMode;
    private final List<String[]> lines;
    private final long subtotalCents;
    private final long shippingCents;
    private final long totalCents;
    private final int itemCount;

    private Order(Map<String, String> f, Cart cart, long now) {
        this.reference = reference(now);
        this.createdAt = now;
        this.name = f.get("name");
        this.email = f.get("email");
        this.address = f.get("address");
        this.zip = f.get("zip");
        this.city = f.get("city");
        this.shippingMode = f.get("shipping");
        List<String[]> l = new ArrayList<String[]>();
        for (Cart.Line line : cart.getLines()) {
            l.add(new String[] {line.getProduct().getName(), String.valueOf(line.getQuantity()),
                Money.format(line.getProduct().getPriceCents()), Money.format(line.getTotalCents()),
                line.getProduct().getImage()});
        }
        this.lines = Collections.unmodifiableList(l);
        this.subtotalCents = cart.getSubtotalCents();
        long ship = cart.getShippingCents();
        if ("express".equals(shippingMode)) {
            ship += 900;
        }
        this.shippingCents = ship;
        this.totalCents = subtotalCents + ship;
        this.itemCount = cart.getCount();
    }

    /** Champs du formulaire nettoyés. */
    public static Map<String, String> clean(Map<String, String> raw) {
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("name", trim(raw.get("name"), 80));
        f.put("email", trim(raw.get("email"), 254));
        f.put("address", trim(raw.get("address"), 120));
        f.put("zip", trim(raw.get("zip"), 5));
        f.put("city", trim(raw.get("city"), 60));
        String s = trim(raw.get("shipping"), 10);
        f.put("shipping", "express".equals(s) ? "express" : "standard");
        f.put("terms", "on".equals(raw.get("terms")) || "true".equals(raw.get("terms")) ? "on" : "");
        return f;
    }

    /** Valide les champs ; retourne une map champ → message (vide si tout est correct). */
    public static Map<String, String> validate(Map<String, String> f) {
        Map<String, String> e = new LinkedHashMap<String, String>();
        if (f.get("name").length() < 2) {
            e.put("name", "Indiquez votre nom (2 caractères minimum).");
        }
        if (!EMAIL.matcher(f.get("email")).matches()) {
            e.put("email", "Adresse e-mail invalide.");
        }
        if (f.get("address").length() < 5) {
            e.put("address", "Indiquez une adresse de livraison.");
        }
        if (!ZIP.matcher(f.get("zip")).matches()) {
            e.put("zip", "Code postal à 5 chiffres attendu.");
        }
        if (f.get("city").length() < 2) {
            e.put("city", "Indiquez la ville.");
        }
        if (!"on".equals(f.get("terms"))) {
            e.put("terms", "Vous devez confirmer qu'il s'agit d'une commande fictive.");
        }
        return e;
    }

    public static Order place(Map<String, String> cleaned, Cart cart, long now) {
        if (cart.isEmpty()) {
            throw new IllegalStateException("Panier vide.");
        }
        return new Order(cleaned, cart, now);
    }

    static String reference(long now) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
        df.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        StringBuilder b = new StringBuilder("EFI-").append(df.format(new Date(now))).append('-');
        for (int i = 0; i < 5; i++) {
            b.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return b.toString();
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c != 0x7f) {
                b.append(c);
            }
        }
        String t = b.toString().trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    public String getReference() {
        return reference;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    public String getShippingMode() {
        return shippingMode;
    }

    /** Lignes : {nom, quantité, prix unitaire, total, image}. */
    public List<String[]> getLines() {
        return lines;
    }

    public long getSubtotalCents() {
        return subtotalCents;
    }

    public long getShippingCents() {
        return shippingCents;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public int getItemCount() {
        return itemCount;
    }
}
