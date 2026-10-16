package fr.efiacademy.lab.dashboard;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.efiacademy.lab.common.RequestUtil;

/**
 * GET /api/metrics : instantané JSON des métriques de la JVM et de l'application.
 * Lecture seule, aucun paramètre, aucune donnée sensible.
 */
public class MetricsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String ATTR_STARTED = "efi.dashboard.startedAt";

    @Override
    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        if (ctx.getAttribute(ATTR_STARTED) == null) {
            ctx.setAttribute(ATTR_STARTED, Long.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext ctx = getServletContext();
        MetricsCollector.AppCounters app = new MetricsCollector.AppCounters();
        app.requests = RequestCounterFilter.counter(ctx, RequestCounterFilter.ATTR).get();
        app.errors = RequestCounterFilter.counter(ctx, RequestCounterFilter.ATTR_ERRORS).get();
        app.activeSessions = SessionCounterListener.active();
        app.createdSessions = SessionCounterListener.created();
        Object started = ctx.getAttribute(ATTR_STARTED);
        app.appStartedAt = started instanceof Long ? ((Long) started).longValue() : System.currentTimeMillis();
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, MetricsCollector.toJson(app, System.currentTimeMillis()));
    }
}
