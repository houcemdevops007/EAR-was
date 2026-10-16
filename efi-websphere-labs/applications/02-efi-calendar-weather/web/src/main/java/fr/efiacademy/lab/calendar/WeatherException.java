package fr.efiacademy.lab.calendar;

/**
 * Erreur d'appel au service météo, classée par catégorie pour faciliter le
 * diagnostic réseau pendant le TP (DNS, pare-feu, proxy, TLS, délai...).
 */
public class WeatherException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String category;

    public WeatherException(String category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    /** dns | connexion | delai | tls | http | reponse | configuration | inconnu */
    public String getCategory() {
        return category;
    }
}
