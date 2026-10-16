package fr.efiacademy.lab.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * API du centre de fichiers.
 * <pre>
 * GET    /api/files        configuration, état du stockage et liste des fichiers (JSON)
 * GET    /api/files/{id}   téléchargement (toujours en pièce jointe, jamais exécuté ni affiché)
 * POST   /api/files        upload multipart (champ « file », plusieurs fichiers possibles) — CSRF
 * DELETE /api/files/{id}   suppression — CSRF
 * </pre>
 * Configuration : propriétés JVM {@code -Defi.files.dir}, {@code -Defi.files.maxBytes}
 * prioritaires sur les paramètres de contexte du web.xml.
 */
public class FileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(FileServlet.class.getName());
    public static final String ATTR_STORE = "efi.files.store";
    static final String DEFAULT_DIR = "/var/efi-academy/files";

    private transient FileStore store;

    @Override
    public void init() throws ServletException {
        ServletContext ctx = getServletContext();
        String dir = config(ctx, "efi.files.dir", DEFAULT_DIR);
        long maxBytes = RequestUtil.parseLong(config(ctx, "efi.files.maxBytes", "5242880"), 5242880L);
        int maxCount = RequestUtil.parseInt(config(ctx, "efi.files.maxCount", "200"), 200);
        String ext = config(ctx, "efi.files.allowedExtensions", "pdf,txt,md,csv,log,json,xml,png,jpg,jpeg,gif,zip");
        store = new FileStore(new File(dir), maxBytes, maxCount, FileStore.parseExtensions(ext));
        ctx.setAttribute(ATTR_STORE, store);
        FileStore.Status st = store.status();
        LOG.info("EFI File Center : stockage " + store.getBaseDir() + " — " + st.message
                + " (limite " + FileStore.human(maxBytes) + ", " + maxCount + " fichiers)");
    }

    static String config(ServletContext ctx, String key, String def) {
        String v = null;
        try {
            v = System.getProperty(key);
        } catch (SecurityException ignored) {
            v = null;
        }
        if (v == null || v.trim().length() == 0) {
            v = ctx.getInitParameter(key);
        }
        return v == null || v.trim().length() == 0 ? def : v.trim();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = pathId(req);
        if (id == null) {
            RequestUtil.sendJson(resp, 200, listJson());
            return;
        }
        download(id, resp);
    }

    private String listJson() {
        FileStore.Status st = store.status();
        List<FileMeta> files = st.exists ? store.list() : java.util.Collections.<FileMeta>emptyList();
        long total = 0;
        for (FileMeta m : files) {
            total += m.getSize();
        }
        JsonWriter w = new JsonWriter().beginObject();
        w.name("config").beginObject()
                .field("directory", store.getBaseDir().getPath())
                .field("maxBytes", store.getMaxBytes())
                .field("maxBytesText", FileStore.human(store.getMaxBytes()))
                .field("maxCount", store.getMaxCount());
        w.name("allowedExtensions").beginArray();
        for (String e : store.getAllowedExtensions()) {
            w.value(e);
        }
        w.endArray().endObject();
        w.name("storage").beginObject()
                .field("exists", st.exists)
                .field("writable", st.writable)
                .field("message", st.message)
                .field("count", files.size())
                .field("totalBytes", total)
                .field("host", ServerInfo.hostname())
                .endObject();
        w.name("files").beginArray();
        for (FileMeta m : files) {
            meta(w, m);
        }
        w.endArray();
        return w.endObject().toString();
    }

    private static void meta(JsonWriter w, FileMeta m) {
        w.beginObject()
                .field("id", m.getId())
                .field("name", m.getName())
                .field("extension", m.getExtension())
                .field("size", m.getSize())
                .field("uploadedAt", ServerInfo.iso(new Date(m.getUploadedAt())))
                .field("sha256", m.getSha256())
                .endObject();
    }

    private void download(String id, HttpServletResponse resp) throws IOException {
        FileMeta m = store.get(id);
        File f;
        try {
            f = m == null ? null : store.open(id);
        } catch (FileStoreException e) {
            f = null;
        }
        if (m == null || f == null) {
            RequestUtil.sendError(resp, 404, "Fichier introuvable.");
            return;
        }
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", contentDisposition(m.getName()));
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy", "sandbox; default-src 'none'");
        resp.setHeader("Cache-Control", "private, no-store");
        if (f.length() <= Integer.MAX_VALUE) {
            resp.setContentLength((int) f.length());
        }
        InputStream in = new FileInputStream(f);
        try {
            OutputStream out = resp.getOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    /** En-tête Content-Disposition compatible RFC 6266 (nom ASCII + nom UTF-8 encodé). */
    static String contentDisposition(String name) {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            ascii.append(c >= 0x20 && c < 0x7f && c != '"' && c != '\\' && c != ';' ? c : '_');
        }
        String encoded;
        try {
            encoded = URLEncoder.encode(name, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = ascii.toString();
        }
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        String ct = req.getContentType();
        if (ct == null || !ct.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
            RequestUtil.sendError(resp, 400, "Requête multipart/form-data attendue.");
            return;
        }
        Collection<Part> parts;
        try {
            parts = req.getParts();
        } catch (IllegalStateException e) {
            RequestUtil.sendError(resp, 413, "Requête trop volumineuse : limite de "
                    + FileStore.human(store.getMaxBytes()) + " par fichier.");
            return;
        } catch (ServletException e) {
            RequestUtil.sendError(resp, 400, "Requête multipart invalide.");
            return;
        }
        JsonWriter ok = new JsonWriter().beginArray();
        JsonWriter ko = new JsonWriter().beginArray();
        int okCount = 0;
        int koCount = 0;
        int lastStatus = 400;
        long now = System.currentTimeMillis();
        for (Part p : parts) {
            String fileName = submittedFileName(p);
            if (!"file".equals(p.getName()) || fileName == null) {
                continue;
            }
            InputStream in = p.getInputStream();
            try {
                FileMeta m = store.store(fileName, in, now);
                meta(ok, m);
                okCount++;
                LOG.info("EFI File Center : fichier reçu " + m.getId() + " (" + m.getSize() + " o)");
            } catch (FileStoreException e) {
                ko.beginObject().field("name", FileStore.sanitizeName(fileName)).field("error", e.getMessage())
                        .field("status", e.getStatus()).endObject();
                koCount++;
                lastStatus = e.getStatus();
                if (e.getStatus() >= 500) {
                    LOG.log(Level.WARNING, "EFI File Center : échec d'enregistrement", e);
                }
            } finally {
                in.close();
                try {
                    p.delete();
                } catch (IOException ignored) {
                    // fichier temporaire du conteneur
                }
            }
        }
        if (okCount == 0 && koCount == 0) {
            RequestUtil.sendError(resp, 400, "Aucun fichier reçu (champ « file » attendu).");
            return;
        }
        String json = new JsonWriter().beginObject()
                .name("uploaded").raw(ok.endArray().toString())
                .name("errors").raw(ko.endArray().toString())
                .endObject().toString();
        RequestUtil.sendJson(resp, okCount > 0 ? 201 : lastStatus, json);
    }

    /**
     * Nom de fichier soumis : {@code Part.getSubmittedFileName()} n'existe qu'à partir de
     * Servlet 3.1, on analyse donc l'en-tête Content-Disposition (Servlet 3.0 / WAS 8.5.5).
     */
    static String submittedFileName(Part part) {
        return fileNameFromDisposition(part.getHeader("content-disposition"));
    }

    static String fileNameFromDisposition(String cd) {
        if (cd == null) {
            return null;
        }
        String result = null;
        for (String token : cd.split(";")) {
            String t = token.trim();
            String lower = t.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("filename*=")) {
                String v = t.substring(10).trim();
                int q = v.indexOf("''");
                if (q >= 0) {
                    try {
                        return java.net.URLDecoder.decode(v.substring(q + 2).replace("+", "%2B"), "UTF-8");
                    } catch (Exception e) {
                        // on retombe sur filename=
                    }
                }
            } else if (lower.startsWith("filename=")) {
                String v = t.substring(9).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1).replace("\\\"", "\"");
                }
                result = v;
            }
        }
        return result;
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        String id = pathId(req);
        if (id == null || !store.delete(id)) {
            RequestUtil.sendError(resp, 404, "Fichier introuvable.");
            return;
        }
        LOG.info("EFI File Center : fichier supprimé " + id);
        RequestUtil.sendJson(resp, 200, new JsonWriter().beginObject().field("deleted", id).endObject().toString());
    }

    /** Identifiant (32 caractères hexadécimaux) en fin d'URL, sinon null. */
    static String pathId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.length() < 2) {
            return null;
        }
        String id = info.substring(1);
        return FileStore.ID.matcher(id).matches() ? id : "invalid";
    }
}
