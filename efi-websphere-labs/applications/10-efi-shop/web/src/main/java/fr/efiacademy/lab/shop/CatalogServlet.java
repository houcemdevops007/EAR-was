package fr.efiacademy.lab.shop;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;

/** GET /api/catalog?q=...&category=... : catalogue filtré (JSON, lecture seule). */
public class CatalogServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String q = RequestUtil.clean(req.getParameter("q"), 80);
        String cat = RequestUtil.clean(req.getParameter("category"), 30);
        if (cat.length() > 0 && !Catalog.categories().containsKey(cat)) {
            RequestUtil.sendError(resp, 400, "Catégorie inconnue.");
            return;
        }
        List<Product> list = Catalog.search(q, cat);
        JsonWriter w = new JsonWriter().beginObject();
        w.name("categories").beginArray();
        for (Map.Entry<String, String> e : Catalog.categories().entrySet()) {
            w.beginObject().field("id", e.getKey()).field("label", e.getValue()).endObject();
        }
        w.endArray();
        w.field("count", list.size());
        w.name("products").beginArray();
        for (Product p : list) {
            ShopSupport.product(w, p);
        }
        w.endArray();
        RequestUtil.sendJson(resp, 200, w.endObject().toString());
    }
}
