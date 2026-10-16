package fr.efiacademy.lab.common;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Point de contrôle de santé : GET /api/health.
 * Utilisé par les scripts de vérification après déploiement et par le TP
 * pour identifier le serveur qui a répondu.
 */
public class HealthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long STARTED = System.currentTimeMillis();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String app = getServletContext().getInitParameter("efi.app.name");
        String version = getServletContext().getInitParameter("efi.app.version");
        String json = new JsonWriter().beginObject()
                .field("status", "UP")
                .field("application", app == null ? getServletContext().getServletContextName() : app)
                .field("version", version == null ? "1.0.0" : version)
                .field("contextRoot", req.getContextPath())
                .field("hostname", ServerInfo.hostname())
                .field("server", ServerInfo.serverName())
                .field("node", ServerInfo.nodeName())
                .field("startedAt", ServerInfo.iso(new java.util.Date(STARTED)))
                .field("timestamp", ServerInfo.isoNow())
                .endObject().toString();
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, json);
    }
}
