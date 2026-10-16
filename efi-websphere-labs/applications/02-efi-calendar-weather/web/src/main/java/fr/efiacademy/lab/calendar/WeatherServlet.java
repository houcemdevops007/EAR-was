package fr.efiacademy.lab.calendar;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * GET /api/weather : météo actuelle et prévisions de Paris via le serveur.
 * <p>
 * Configuration (propriété système JVM prioritaire, sinon context-param) :
 * efi.weather.url, efi.weather.connectTimeoutMs (4000),
 * efi.weather.readTimeoutMs (6000), efi.weather.cacheSeconds (600).
 */
public class WeatherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient WeatherClient client;

    @Override
    public void init() throws ServletException {
        String url = setting("efi.weather.url", WeatherClient.DEFAULT_URL);
        int connect = RequestUtil.parseInt(setting("efi.weather.connectTimeoutMs", "4000"), 4000);
        int read = RequestUtil.parseInt(setting("efi.weather.readTimeoutMs", "6000"), 6000);
        long cache = RequestUtil.parseLong(setting("efi.weather.cacheSeconds", "600"), 600) * 1000L;
        client = new WeatherClient(url, connect, read, cache);
        log("[EFI Calendar] Service météo : " + client.getHost() + " (connexion " + connect
                + " ms, lecture " + read + " ms, cache " + cache / 1000 + " s)");
    }

    private String setting(String key, String fallback) {
        String v = System.getProperty(key);
        if (v == null || v.trim().length() == 0) {
            v = getServletContext().getInitParameter(key);
        }
        return v == null || v.trim().length() == 0 ? fallback : v.trim();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            WeatherClient.Result r = client.fetch();
            String json = new JsonWriter().beginObject()
                    .field("source", "server")
                    .field("hostname", ServerInfo.hostname())
                    .field("cached", r.cached)
                    .field("fetchedAt", ServerInfo.iso(new Date(r.fetchedAt)))
                    .field("durationMs", r.durationMs)
                    .name("data").raw(r.body)
                    .endObject().toString();
            RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, json);
        } catch (WeatherException e) {
            log("[EFI Calendar] Météo indisponible (" + e.getCategory() + ") : " + e.getMessage()
                    + (e.getCause() != null ? " / " + e.getCause() : ""));
            int status = "delai".equals(e.getCategory())
                    ? HttpServletResponse.SC_GATEWAY_TIMEOUT : HttpServletResponse.SC_BAD_GATEWAY;
            String json = new JsonWriter().beginObject()
                    .field("error", e.getMessage())
                    .field("category", e.getCategory())
                    .field("host", client.getHost())
                    .field("hostname", ServerInfo.hostname())
                    .field("status", status)
                    .endObject().toString();
            RequestUtil.sendJson(resp, status, json);
        }
    }
}
