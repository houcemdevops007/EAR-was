package fr.efiacademy.lab.common;

/**
 * Échappement HTML pour se protéger des injections XSS dans les JSP.
 * Compatible Java 6.
 */
public final class Html {

    private Html() {
    }

    /** Échappe une valeur pour un contenu HTML ou un attribut entre guillemets. */
    public static String esc(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '/':
                    sb.append("&#47;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Retourne la valeur ou un texte par défaut si elle est vide. */
    public static String orDefault(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }
}
