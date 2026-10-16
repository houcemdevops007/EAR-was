package fr.efiacademy.lab.common;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Informations non sensibles sur le serveur qui exécute l'application.
 * <p>
 * Le nom du serveur WebSphere est obtenu par réflexion via l'API d'exécution
 * {@code com.ibm.websphere.runtime.ServerName} (lecture seule, aucun accès
 * administrateur). Hors WebSphere, la méthode retourne {@code null}.
 * Compatible Java 6.
 */
public final class ServerInfo {

    public static final String PARIS = "Europe/Paris";

    private static volatile String hostname;
    private static volatile String[] wasNames;

    private ServerInfo() {
    }

    /** Nom d'hôte de la machine (mis en cache). */
    public static String hostname() {
        String h = hostname;
        if (h == null) {
            h = resolveHostname();
            hostname = h;
        }
        return h;
    }

    private static String resolveHostname() {
        try {
            String n = InetAddress.getLocalHost().getHostName();
            if (n != null && n.length() > 0) {
                return n;
            }
        } catch (Throwable ignored) {
            // résolution DNS impossible : on tente la variable HOSTNAME
        }
        try {
            String env = System.getenv("HOSTNAME");
            if (env != null && env.length() > 0) {
                return env;
            }
        } catch (Throwable ignored) {
            // Java 2 Security peut interdire getenv
        }
        return "hôte inconnu";
    }

    /**
     * Retourne {cellule, nœud, serveur} si l'application tourne dans WebSphere
     * traditionnel, sinon un tableau de trois {@code null}.
     */
    private static String[] wasNames() {
        String[] names = wasNames;
        if (names == null) {
            names = new String[3];
            try {
                Class<?> c = Class.forName("com.ibm.websphere.runtime.ServerName");
                Method full = c.getMethod("getFullName", new Class<?>[0]);
                Object v = full.invoke(null, new Object[0]);
                if (v != null) {
                    // Format WebSphere : cellule\nœud\serveur
                    String[] parts = v.toString().split("\\\\");
                    if (parts.length == 3) {
                        names = parts;
                    }
                }
                if (names[2] == null) {
                    Method disp = c.getMethod("getDisplayName", new Class<?>[0]);
                    Object d = disp.invoke(null, new Object[0]);
                    if (d != null) {
                        names[2] = d.toString();
                    }
                }
            } catch (Throwable notWebSphere) {
                String liberty = System.getProperty("wlp.server.name");
                if (liberty != null) {
                    names[2] = liberty;
                }
            }
            wasNames = names;
        }
        return names;
    }

    /** Nom du serveur d'applications WebSphere (ex. « test »), ou null. */
    public static String serverName() {
        return wasNames()[2];
    }

    /** Nom du nœud WebSphere (ex. « nodesa1Node01 »), ou null. */
    public static String nodeName() {
        return wasNames()[1];
    }

    /** Nom de la cellule WebSphere, ou null. */
    public static String cellName() {
        return wasNames()[0];
    }

    /** Libellé court pour le badge d'en-tête. */
    public static String badgeLabel() {
        String srv = serverName();
        String node = nodeName();
        StringBuilder sb = new StringBuilder(hostname());
        if (srv != null) {
            sb.append(" · ");
            if (node != null) {
                sb.append(node).append('/');
            }
            sb.append(srv);
        }
        return sb.toString();
    }

    /** Date courante formatée dans le fuseau Europe/Paris. */
    public static String nowParis(String pattern) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.FRANCE);
        f.setTimeZone(TimeZone.getTimeZone(PARIS));
        return f.format(new Date());
    }

    /** Horodatage ISO-8601 (UTC) pour les API JSON. */
    public static String isoNow() {
        return iso(new Date());
    }

    public static String iso(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(d);
    }
}
