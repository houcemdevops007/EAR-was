package fr.efiacademy.lab.shop;

/** Formatage monétaire en euros, à la française, à partir de centimes (aucun double). */
public final class Money {

    /** Espace fine insécable utilisée comme séparateur de milliers. */
    private static final char THIN = '\u202f';
    private static final char NBSP = '\u00a0';

    private Money() {
    }

    public static String format(long cents) {
        boolean neg = cents < 0;
        long abs = Math.abs(cents);
        String euros = String.valueOf(abs / 100);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < euros.length(); i++) {
            if (i > 0 && (euros.length() - i) % 3 == 0) {
                b.append(THIN);
            }
            b.append(euros.charAt(i));
        }
        long c = abs % 100;
        b.append(',').append(c < 10 ? "0" : "").append(c).append(NBSP).append('€');
        return (neg ? "-" : "") + b;
    }

    /** Part de TVA contenue dans un montant TTC (taux 20 %), arrondie au centime. */
    public static long vatIncluded(long ttcCents) {
        return Math.round(ttcCents - ttcCents / 1.2);
    }
}
