package fr.efiacademy.lab.shop;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catalogue en lecture seule de produits fictifs (aucune donnée réelle, aucun paiement). */
public final class Catalog {

    private static final Map<String, Product> PRODUCTS = new LinkedHashMap<String, Product>();
    private static final Map<String, String> CATEGORIES = new LinkedHashMap<String, String>();

    static {
        CATEGORIES.put("serveurs", "Serveurs & matériel");
        CATEGORIES.put("reseau", "Réseau");
        CATEGORIES.put("formation", "Formation");
        CATEGORIES.put("goodies", "Goodies EFI");

        add("p01", "Serveur rack EFI R-220", "serveurs", 249900, "Serveur 2U de laboratoire, 2 processeurs, 128 Go de RAM : idéal pour héberger un DMGR et deux nœuds.", "server-rack", "Best-seller", 4.8, 6);
        add("p02", "Mini-PC de TP NUC-Lab", "serveurs", 54900, "Poste compact pour installer WebSphere ND en autonomie et réviser chez soi.", "mini-pc", null, 4.5, 14);
        add("p03", "SSD NVMe 2 To", "serveurs", 15990, "Stockage rapide pour les profils WebSphere et les journaux volumineux.", "ssd", "-15 %", 4.7, 30);
        add("p04", "Écran 27\" QHD", "serveurs", 32900, "Deux consoles d'administration côte à côte, sans compromis.", "monitor", null, 4.6, 9);
        add("p05", "Switch 24 ports manageable", "reseau", 38900, "Segmentez votre cellule : VLAN, agrégation de liens et supervision SNMP.", "switch", null, 4.4, 11);
        add("p06", "Kit de câbles RJ45 Cat. 6", "reseau", 2490, "Lot de 10 câbles colorés pour relier DMGR, nœuds et serveur HTTP.", "cables", null, 4.3, 120);
        add("p07", "Routeur Wi-Fi 6 Lab", "reseau", 12900, "Réseau sans fil isolé pour les salles de formation.", "router", "Nouveau", 4.2, 18);
        add("p08", "Livre « WebSphere ND en pratique »", "formation", 4500, "Le guide de référence des TP EFI : cellule, fédération, déploiement et dépannage.", "book", "Coup de cœur", 4.9, 50);
        add("p09", "Bon de certification (fictif)", "formation", 19900, "Bon de démonstration pour une session d'examen — produit purement pédagogique.", "voucher", null, 4.6, 99);
        add("p10", "Atelier « wsadmin & Jython »", "formation", 69000, "Une journée d'atelier pour automatiser installations et mises à jour d'applications.", "workshop", null, 4.8, 12);
        add("p11", "Mug « Synchronisé ! »", "goodies", 1490, "Pour les pauses café pendant la synchronisation des nœuds.", "mug", null, 4.7, 200);
        add("p12", "Sweat EFI Academy", "goodies", 3990, "Sweat à capuche bleu nuit, logo EFI brodé.", "hoodie", "Édition limitée", 4.5, 40);
        add("p13", "Pack d'autocollants WebSphere", "goodies", 590, "12 autocollants : DMGR, Node Agent, EAR, WAR, cellule…", "stickers", null, 4.4, 500);
        add("p14", "Casque audio anti-bruit", "goodies", 8990, "Concentration maximale pendant la lecture de SystemOut.log.", "headset", null, 4.3, 25);
    }

    private Catalog() {
    }

    private static void add(String id, String name, String cat, long price, String desc, String img, String badge,
            double rating, int stock) {
        PRODUCTS.put(id, new Product(id, name, cat, price, desc, img, badge, rating, stock));
    }

    public static List<Product> all() {
        return Collections.unmodifiableList(new ArrayList<Product>(PRODUCTS.values()));
    }

    public static Product get(String id) {
        return id == null ? null : PRODUCTS.get(id);
    }

    public static Map<String, String> categories() {
        return Collections.unmodifiableMap(CATEGORIES);
    }

    public static String categoryLabel(String id) {
        String l = CATEGORIES.get(id);
        return l == null ? id : l;
    }

    /** Recherche insensible à la casse et aux accents sur le nom et la description. */
    public static List<Product> search(String query, String category) {
        String q = fold(query);
        List<Product> out = new ArrayList<Product>();
        for (Product p : PRODUCTS.values()) {
            if (category != null && category.length() > 0 && !category.equals(p.getCategory())) {
                continue;
            }
            if (q.length() > 0 && fold(p.getName() + " " + p.getDescription()).indexOf(q) < 0) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    /** Minuscules sans accents (java.text.Normalizer est disponible depuis Java 6). */
    public static String fold(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s.trim().toLowerCase(Locale.FRANCE), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }
}
