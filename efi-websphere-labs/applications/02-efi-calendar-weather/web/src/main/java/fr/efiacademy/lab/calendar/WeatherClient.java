package fr.efiacademy.lab.calendar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * Client HTTP Java 6 vers l'API publique Open-Meteo (sans clé, sans secret).
 * <ul>
 * <li>délais de connexion et de lecture configurables ;</li>
 * <li>cache mémoire (10 minutes par défaut) pour limiter les appels ;</li>
 * <li>respect des propriétés JVM https.proxyHost / https.proxyPort ;</li>
 * <li>erreurs classées pour le diagnostic (DNS, TLS, pare-feu...).</li>
 * </ul>
 */
public class WeatherClient {

    /** Coordonnées fixes de Paris (Hôtel de Ville). */
    public static final String DEFAULT_URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=48.8566&longitude=2.3522"
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,"
            + "wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset"
            + "&timezone=Europe%2FParis&forecast_days=7&wind_speed_unit=kmh";

    static final int MAX_BYTES = 512 * 1024;

    private final String url;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final long cacheTtlMs;

    private String cachedBody;
    private long cachedAt;

    public WeatherClient(String url, int connectTimeoutMs, int readTimeoutMs, long cacheTtlMs) {
        this.url = url;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.cacheTtlMs = cacheTtlMs;
    }

    /** Résultat d'un appel (corps JSON + métadonnées). */
    public static final class Result {
        public final String body;
        public final boolean cached;
        public final long fetchedAt;
        public final long durationMs;

        Result(String body, boolean cached, long fetchedAt, long durationMs) {
            this.body = body;
            this.cached = cached;
            this.fetchedAt = fetchedAt;
            this.durationMs = durationMs;
        }
    }

    public synchronized Result fetch() throws WeatherException {
        long now = System.currentTimeMillis();
        if (cachedBody != null && now - cachedAt < cacheTtlMs) {
            return new Result(cachedBody, true, cachedAt, 0);
        }
        long start = System.currentTimeMillis();
        String body = download();
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new WeatherException("reponse", "Réponse inattendue du service météo (JSON attendu).", null);
        }
        cachedBody = trimmed;
        cachedAt = System.currentTimeMillis();
        return new Result(trimmed, false, cachedAt, cachedAt - start);
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return "?";
        }
    }

    /** Télécharge le document JSON. Méthode protégée pour faciliter les tests. */
    protected String download() throws WeatherException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "EFI-Academy-WebSphere-Lab/1.0 (Java)");
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new WeatherException("http", "Le service météo a répondu HTTP " + code + ".", null);
            }
            in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_BYTES) {
                    throw new WeatherException("reponse", "Réponse du service météo trop volumineuse.", null);
                }
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (WeatherException e) {
            throw e;
        } catch (IOException e) {
            throw classify(e, getHost());
        } catch (RuntimeException e) {
            throw new WeatherException("inconnu", "Erreur inattendue : " + e.getClass().getName(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // fermeture silencieuse
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** Traduit une exception réseau en message de diagnostic pédagogique. */
    static WeatherException classify(IOException e, String host) {
        if (e instanceof UnknownHostException) {
            return new WeatherException("dns", "Résolution DNS impossible pour " + host
                    + " : vérifier /etc/resolv.conf ou le proxy sortant.", e);
        }
        if (e instanceof SocketTimeoutException) {
            return new WeatherException("delai", "Délai d'attente dépassé vers " + host
                    + " : pare-feu, proxy ou réseau lent.", e);
        }
        if (e instanceof SSLException) {
            return new WeatherException("tls", "Échec de la négociation TLS avec " + host
                    + " : importer le certificat signataire dans le truststore WebSphere"
                    + " (CellDefaultTrustStore) ou vérifier le protocole TLS 1.2.", e);
        }
        if (e instanceof ConnectException || e instanceof NoRouteToHostException) {
            return new WeatherException("connexion", "Connexion refusée ou impossible vers " + host
                    + ":443 : pare-feu sortant ou proxy requis (https.proxyHost).", e);
        }
        if (e instanceof MalformedURLException) {
            return new WeatherException("configuration", "URL du service météo invalide.", e);
        }
        return new WeatherException("inconnu", "Erreur réseau : " + e.getClass().getSimpleName(), e);
    }
}
