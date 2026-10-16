package fr.efiacademy.lab.shop;

import java.io.Serializable;

/** Produit fictif du catalogue de démonstration (prix en centimes, TTC). */
public final class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String category;
    private final long priceCents;
    private final String description;
    private final String image;
    private final String badge;
    private final double rating;
    private final int stock;

    public Product(String id, String name, String category, long priceCents, String description, String image,
            String badge, double rating, int stock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priceCents = priceCents;
        this.description = description;
        this.image = image;
        this.badge = badge;
        this.rating = rating;
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public long getPriceCents() {
        return priceCents;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getBadge() {
        return badge;
    }

    public double getRating() {
        return rating;
    }

    public int getStock() {
        return stock;
    }
}
