package fr.efiacademy.lab.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mini navigateur HTTP pour les tests : cookies de session, jeton CSRF, formulaires,
 * multipart. Les redirections ne sont PAS suivies automatiquement (on les vérifie).
 */
final class Browser {

    static final Pattern META_CSRF = Pattern.compile("<meta name=\"csrf-token\" content=\"([^\"]*)\"");

    private final String base;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private String csrf;

    Browser(String base) {
        this.base = base;
    }

    static final class Response {
        final int status;
        final Map<String, List<String>> headers;
        final byte[] bytes;
        final String body;

        Response(int status, Map<String, List<String>> headers, byte[] bytes) {
            this.status = status;
            this.headers = headers;
            this.bytes = bytes;
            this.body = new String(bytes, StandardCharsets.UTF_8);
        }

        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                    return e.getValue().get(0);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "HTTP " + status + " " + (body.length() > 300 ? body.substring(0, 300) + "…" : body);
        }
    }

    /** Charge une page HTML et mémorise le jeton CSRF publié dans la balise meta. */
    Response page(String path) throws IOException {
        Response r = get(path);
        Matcher m = META_CSRF.matcher(r.body);
        if (m.find()) {
            csrf = m.group(1);
        }
        return r;
    }

    String csrf() {
        return csrf;
    }

    Response get(String path) throws IOException {
        return send("GET", path, null, null, true);
    }

    Response post(String path, Map<String, String> form) throws IOException {
        return send("POST", path, encode(form), "application/x-www-form-urlencoded; charset=UTF-8", true);
    }

    Response postNoCsrf(String path, Map<String, String> form) throws IOException {
        return send("POST", path, encode(form), "application/x-www-form-urlencoded; charset=UTF-8", false);
    }

    Response put(String path, Map<String, String> form) throws IOException {
        return send("PUT", path, encode(form), "application/x-www-form-urlencoded; charset=UTF-8", true);
    }

    Response delete(String path) throws IOException {
        return send("DELETE", path, null, null, true);
    }

    /** Envoi multipart/form-data d'un fichier (champ « file »). */
    Response upload(String path, String fileName, byte[] content, boolean withCsrf) throws IOException {
        String boundary = "----efi" + System.nanoTime();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        b.write(head.getBytes(StandardCharsets.UTF_8));
        b.write(content);
        b.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send("POST", path, b.toByteArray(), "multipart/form-data; boundary=" + boundary, withCsrf);
    }

    static Map<String, String> form(String... kv) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static byte[] encode(Map<String, String> form) throws IOException {
        if (form == null) {
            return new byte[0];
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Response send(String method, String path, byte[] body, String contentType, boolean withCsrf) throws IOException {
        URL url = new URL(base + path);
        URI uri = URI.create(base + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestMethod(method);
        c.setConnectTimeout(5000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "text/html,application/json");
        c.setRequestProperty("Accept-Language", "fr-FR");
        Map<String, List<String>> ck = cookies.get(uri, Collections.<String, List<String>>emptyMap());
        for (Map.Entry<String, List<String>> e : ck.entrySet()) {
            for (String v : e.getValue()) {
                c.addRequestProperty(e.getKey(), v);
            }
        }
        if (withCsrf && csrf != null && !"GET".equals(method)) {
            c.setRequestProperty("X-CSRF-Token", csrf);
        }
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType);
            OutputStream out = c.getOutputStream();
            out.write(body);
            out.close();
        }
        int status = c.getResponseCode();
        cookies.put(uri, c.getHeaderFields());
        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (in != null) {
            byte[] tmp = new byte[8192];
            int n;
            while ((n = in.read(tmp)) != -1) {
                buf.write(tmp, 0, n);
            }
            in.close();
        }
        return new Response(status, c.getHeaderFields(), buf.toByteArray());
    }
}
