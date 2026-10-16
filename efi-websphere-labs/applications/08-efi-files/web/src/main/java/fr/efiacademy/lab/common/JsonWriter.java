package fr.efiacademy.lab.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur JSON minimaliste (aucune dépendance externe, compatible Java 6).
 * Les caractères &lt; &gt; &amp; ' sont encodés en \\uXXXX pour éviter toute
 * interprétation HTML si la réponse était affichée par erreur dans une page.
 */
public final class JsonWriter {

    private final StringBuilder sb = new StringBuilder(256);
    private final List<boolean[]> stack = new ArrayList<boolean[]>();
    private boolean afterName;

    public JsonWriter beginObject() {
        prefix();
        sb.append('{');
        stack.add(new boolean[] {true});
        return this;
    }

    public JsonWriter endObject() {
        stack.remove(stack.size() - 1);
        sb.append('}');
        return this;
    }

    public JsonWriter beginArray() {
        prefix();
        sb.append('[');
        stack.add(new boolean[] {true});
        return this;
    }

    public JsonWriter endArray() {
        stack.remove(stack.size() - 1);
        sb.append(']');
        return this;
    }

    public JsonWriter name(String name) {
        comma();
        sb.append(quote(name)).append(':');
        afterName = true;
        return this;
    }

    public JsonWriter value(String v) {
        prefix();
        sb.append(v == null ? "null" : quote(v));
        return this;
    }

    public JsonWriter value(long v) {
        prefix();
        sb.append(v);
        return this;
    }

    public JsonWriter value(double v) {
        prefix();
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            sb.append("null");
        } else {
            sb.append(v);
        }
        return this;
    }

    public JsonWriter value(boolean v) {
        prefix();
        sb.append(v ? "true" : "false");
        return this;
    }

    public JsonWriter nullValue() {
        prefix();
        sb.append("null");
        return this;
    }

    /** Insère un fragment JSON déjà valide (ex. réponse d'une API de confiance). */
    public JsonWriter raw(String json) {
        prefix();
        sb.append(json == null ? "null" : json);
        return this;
    }

    public JsonWriter field(String name, String v) {
        return name(name).value(v);
    }

    public JsonWriter field(String name, long v) {
        return name(name).value(v);
    }

    public JsonWriter field(String name, double v) {
        return name(name).value(v);
    }

    public JsonWriter field(String name, boolean v) {
        return name(name).value(v);
    }

    private void prefix() {
        if (afterName) {
            afterName = false;
            return;
        }
        comma();
    }

    private void comma() {
        if (!stack.isEmpty()) {
            boolean[] first = stack.get(stack.size() - 1);
            if (first[0]) {
                first[0] = false;
            } else {
                sb.append(',');
            }
        }
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    /** Encode une chaîne au format JSON (guillemets inclus). */
    public static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '<':
                case '>':
                case '&':
                case '\'':
                case '\u2028':
                case '\u2029':
                    unicode(out, c);
                    break;
                default:
                    if (c < 0x20) {
                        unicode(out, c);
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
        return out.toString();
    }

    private static void unicode(StringBuilder out, char c) {
        String hex = Integer.toHexString(c);
        out.append("\\u");
        for (int k = hex.length(); k < 4; k++) {
            out.append('0');
        }
        out.append(hex);
    }
}
