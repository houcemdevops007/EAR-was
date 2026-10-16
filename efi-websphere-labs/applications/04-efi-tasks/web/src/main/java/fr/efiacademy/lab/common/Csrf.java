package fr.efiacademy.lab.common;

import java.security.SecureRandom;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Protection CSRF par jeton de synchronisation stocké en session HTTP.
 * Le jeton est publié dans la page via une balise meta et renvoyé par le
 * JavaScript dans l'en-tête {@value #HEADER}.
 */
public final class Csrf {

    public static final String SESSION_KEY = "efi.csrf.token";
    public static final String HEADER = "X-CSRF-Token";
    public static final String PARAM = "_csrf";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Csrf() {
    }

    /** Retourne (et crée si besoin) le jeton CSRF de la session. */
    public static String token(HttpSession session) {
        synchronized (session) {
            String t = (String) session.getAttribute(SESSION_KEY);
            if (t == null) {
                byte[] b = new byte[24];
                RANDOM.nextBytes(b);
                char[] c = new char[b.length * 2];
                for (int i = 0; i < b.length; i++) {
                    c[i * 2] = HEX[(b[i] >> 4) & 0xF];
                    c[i * 2 + 1] = HEX[b[i] & 0xF];
                }
                t = new String(c);
                session.setAttribute(SESSION_KEY, t);
            }
            return t;
        }
    }

    /** Vérifie le jeton transmis par en-tête ou paramètre de formulaire. */
    public static boolean isValid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return false;
        }
        String expected = (String) s.getAttribute(SESSION_KEY);
        String provided = req.getHeader(HEADER);
        if (provided == null) {
            provided = req.getParameter(PARAM);
        }
        return expected != null && provided != null && constantTimeEquals(expected, provided);
    }

    static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
