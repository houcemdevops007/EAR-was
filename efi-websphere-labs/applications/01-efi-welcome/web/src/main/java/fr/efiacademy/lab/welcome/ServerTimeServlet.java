package fr.efiacademy.lab.welcome;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * GET /api/server-time : heure du serveur (epoch ms + libellé Europe/Paris).
 * Permet au navigateur de synchroniser l'horloge affichée avec le serveur.
 */
public class ServerTimeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, buildJson(System.currentTimeMillis()));
    }

    static String buildJson(long now) {
        return new JsonWriter().beginObject()
                .field("epochMillis", now)
                .field("paris", ServerInfo.nowParis("EEEE d MMMM yyyy 'à' HH:mm:ss"))
                .field("timezone", ServerInfo.PARIS)
                .field("hostname", ServerInfo.hostname())
                .field("server", ServerInfo.serverName())
                .endObject().toString();
    }
}
