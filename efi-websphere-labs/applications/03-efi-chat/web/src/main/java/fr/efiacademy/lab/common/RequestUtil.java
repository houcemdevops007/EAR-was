package fr.efiacademy.lab.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Fonctions utilitaires pour les servlets : lecture des paramètres (y compris
 * pour PUT/DELETE), réponses JSON, validation de base. Compatible Java 6.
 */
public final class RequestUtil {

    /** Taille maximale d'un corps de requête de formulaire (64 Ko). */
    public static final int MAX_FORM_BODY = 64 * 1024;

    private RequestUtil() {
    }

    /**
     * Retourne les paramètres de la requête. Pour POST, le conteneur décode
     * lui-même le formulaire ; pour PUT/DELETE/PATCH, le corps
     * application/x-www-form-urlencoded est décodé manuellement.
     */
    public static Map<String, String> params(HttpServletRequest req) throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        String method = req.getMethod();
        if (!"POST".equals(method) && !"GET".equals(method)) {
            String body = readBody(req, MAX_FORM_BODY);
            parseForm(body, map);
        }
        Enumeration<?> names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String n = (String) names.nextElement();
            if (!map.containsKey(n)) {
                map.put(n, req.getParameter(n));
            }
        }
        return map;
    }

    /** Décode une chaîne application/x-www-form-urlencoded. */
    public static void parseForm(String body, Map<String, String> into) {
        if (body == null || body.length() == 0) {
            return;
        }
        String[] pairs = body.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String p = pairs[i];
            if (p.length() == 0) {
                continue;
            }
            int eq = p.indexOf('=');
            String k = eq < 0 ? p : p.substring(0, eq);
            String v = eq < 0 ? "" : p.substring(eq + 1);
            try {
                into.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            } catch (IllegalArgumentException malformed) {
                // paramètre mal encodé : ignoré
            }
        }
    }

    /** Lit le corps de la requête en UTF-8 avec une taille maximale. */
    public static String readBody(HttpServletRequest req, int maxBytes) throws IOException {
        InputStream in = req.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new IOException("Corps de requête trop volumineux");
            }
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    /** Nettoie une saisie : trim, suppression des caractères de contrôle, longueur max. */
    public static String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\t' || c >= 0x20 && c != 0x7F) {
                sb.append(c);
            }
        }
        String s = sb.toString().trim();
        if (s.length() > maxLength) {
            s = s.substring(0, maxLength);
        }
        return s;
    }

    /** Identifiant numérique en fin d'URL (ex. /api/tasks/12 → 12), ou -1. */
    public static long pathId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.length() < 2) {
            return -1;
        }
        String s = info.substring(1);
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static int parseInt(String s, int fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static long parseLong(String s, long fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Envoie une réponse JSON non mise en cache. */
    public static void sendJson(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);
        PrintWriter w = resp.getWriter();
        w.write(json);
        w.flush();
    }

    /** Envoie une erreur JSON {"error": "...", "status": n}. */
    public static void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        String json = new JsonWriter().beginObject()
                .field("error", message)
                .field("status", status)
                .endObject().toString();
        sendJson(resp, status, json);
    }

    /** Refuse la requête si le jeton CSRF est absent ou invalide. */
    public static boolean checkCsrf(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!Csrf.isValid(req)) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Jeton de sécurité invalide ou session expirée. Rechargez la page.");
            return false;
        }
        return true;
    }
}
