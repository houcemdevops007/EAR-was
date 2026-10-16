package fr.efiacademy.lab.tasks;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * API REST des tâches — illustre les méthodes HTTP :
 * <pre>
 * GET    /api/tasks              liste + statistiques
 * GET    /api/tasks/{id}         détail
 * POST   /api/tasks              création            (201 Created)
 * POST   /api/tasks/reset        réinitialisation des données de démonstration
 * PUT    /api/tasks/{id}         modification complète
 * PUT    /api/tasks/{id}/status  déplacement entre colonnes
 * DELETE /api/tasks/{id}         suppression
 * </pre>
 * Les corps PUT sont encodés en application/x-www-form-urlencoded.
 */
public class TaskServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    static final String STORE = "efi.tasks.store";

    @Override
    public void init() throws ServletException {
        TaskStore store = new TaskStore();
        store.seed(System.currentTimeMillis());
        getServletContext().setAttribute(STORE, store);
        log("[EFI Tasks] Magasin de tâches initialisé en mémoire (" + store.list().size() + " tâches de démonstration).");
    }

    private TaskStore store() {
        return (TaskStore) getServletContext().getAttribute(STORE);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id = RequestUtil.pathId(req);
        if (id > 0) {
            Task t = store().get(id);
            if (t == null) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Tâche introuvable.");
                return;
            }
            JsonWriter w = new JsonWriter();
            write(w, t, TaskStore.today());
            RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, w.toString());
            return;
        }
        String today = TaskStore.today();
        List<Task> list = store().list();
        JsonWriter w = new JsonWriter().beginObject()
                .field("today", today)
                .field("hostname", ServerInfo.hostname());
        w.name("stats").beginObject();
        for (Map.Entry<String, Integer> e : store().stats(today).entrySet()) {
            w.field(e.getKey(), e.getValue().intValue());
        }
        w.endObject();
        w.name("tasks").beginArray();
        for (Task t : list) {
            write(w, t, today);
        }
        w.endArray().endObject();
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, w.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        if ("/reset".equals(req.getPathInfo())) {
            store().seed(System.currentTimeMillis());
            log("[EFI Tasks] Données de démonstration réinitialisées depuis " + req.getRemoteAddr());
            RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, "{\"ok\":true}");
            return;
        }
        Map<String, String> p = RequestUtil.params(req);
        String title = RequestUtil.clean(p.get("title"), 200);
        String desc = RequestUtil.clean(p.get("description"), 2000);
        String prio = def(p.get("priority"), "moyenne");
        String status = def(p.get("status"), "todo");
        String due = RequestUtil.clean(p.get("dueDate"), 10);
        TaskStore.Validation v = TaskStore.validate(title, desc, prio, status, due);
        if (!v.isValid()) {
            validationError(resp, v);
            return;
        }
        try {
            Task t = store().create(title, desc, prio, status, due, System.currentTimeMillis());
            JsonWriter w = new JsonWriter();
            write(w, t, TaskStore.today());
            resp.setHeader("Location", req.getContextPath() + "/api/tasks/" + t.getId());
            RequestUtil.sendJson(resp, HttpServletResponse.SC_CREATED, w.toString());
        } catch (IllegalStateException full) {
            RequestUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, full.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> p = RequestUtil.params(req);
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        long id = RequestUtil.pathId(req);
        if (id <= 0) {
            RequestUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Identifiant de tâche manquant.");
            return;
        }
        Task t;
        String info = req.getPathInfo();
        if (info != null && info.endsWith("/status")) {
            String status = p.get("status");
            if (!Task.isValidStatus(status)) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Statut invalide.");
                return;
            }
            t = store().move(id, status, System.currentTimeMillis());
        } else {
            String title = RequestUtil.clean(p.get("title"), 200);
            String desc = RequestUtil.clean(p.get("description"), 2000);
            String prio = p.get("priority");
            String status = p.get("status");
            String due = RequestUtil.clean(p.get("dueDate"), 10);
            TaskStore.Validation v = TaskStore.validate(title, desc, prio, status, due);
            if (!v.isValid()) {
                validationError(resp, v);
                return;
            }
            t = store().update(id, title, desc, prio, status, due, System.currentTimeMillis());
        }
        if (t == null) {
            RequestUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Tâche introuvable (peut-être supprimée).");
            return;
        }
        JsonWriter w = new JsonWriter();
        write(w, t, TaskStore.today());
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, w.toString());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        long id = RequestUtil.pathId(req);
        if (id <= 0 || !store().delete(id)) {
            RequestUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Tâche introuvable.");
            return;
        }
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, "{\"deleted\":" + id + "}");
    }

    private static String def(String v, String fallback) {
        return v == null || v.length() == 0 ? fallback : v;
    }

    private static void validationError(HttpServletResponse resp, TaskStore.Validation v) throws IOException {
        JsonWriter w = new JsonWriter().beginObject()
                .field("error", "Certains champs sont invalides.")
                .field("status", 400);
        w.name("fields").beginObject();
        for (Map.Entry<String, String> e : v.errors.entrySet()) {
            w.field(e.getKey(), e.getValue());
        }
        w.endObject().endObject();
        RequestUtil.sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, w.toString());
    }

    static void write(JsonWriter w, Task t, String today) {
        w.beginObject()
                .field("id", t.getId())
                .field("title", t.getTitle())
                .field("description", t.getDescription())
                .field("priority", t.getPriority())
                .field("status", t.getStatus())
                .field("dueDate", t.getDueDate())
                .field("overdue", TaskStore.isOverdue(t, today))
                .field("createdAt", ServerInfo.iso(new Date(t.getCreatedAt())))
                .field("updatedAt", ServerInfo.iso(new Date(t.getUpdatedAt())))
                .endObject();
    }
}
