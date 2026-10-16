package fr.efiacademy.lab.shop;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fr.efiacademy.lab.common.RequestUtil;

/**
 * Panier en session HTTP.
 * <pre>
 * GET    /api/cart               contenu du panier
 * POST   /api/cart               productId, quantity  → ajout
 * PUT    /api/cart/{productId}   quantity             → modification (0 = suppression)
 * DELETE /api/cart/{productId}                        → suppression d'une ligne
 * DELETE /api/cart                                    → vidage
 * </pre>
 * Toutes les modifications exigent le jeton CSRF (en-tête X-CSRF-Token).
 */
public class CartServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession s = req.getSession(true);
        RequestUtil.sendJson(resp, 200, ShopSupport.cartJson(ShopSupport.cart(s)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> p = RequestUtil.params(req);
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        HttpSession s = req.getSession(true);
        Cart cart = ShopSupport.cart(s);
        try {
            cart.add(p.get("productId"), RequestUtil.parseInt(p.get("quantity"), 1));
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(resp, 400, e.getMessage());
            return;
        } catch (IllegalStateException e) {
            RequestUtil.sendError(resp, 409, e.getMessage());
            return;
        }
        ShopSupport.touch(s, cart);
        RequestUtil.sendJson(resp, 200, ShopSupport.cartJson(cart));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> p = RequestUtil.params(req);
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        String id = productId(req);
        if (id == null) {
            RequestUtil.sendError(resp, 400, "Produit manquant dans l'URL.");
            return;
        }
        HttpSession s = req.getSession(true);
        Cart cart = ShopSupport.cart(s);
        try {
            cart.set(id, RequestUtil.parseInt(p.get("quantity"), -1));
        } catch (IllegalArgumentException e) {
            RequestUtil.sendError(resp, 400, e.getMessage());
            return;
        } catch (IllegalStateException e) {
            RequestUtil.sendError(resp, 409, e.getMessage());
            return;
        }
        ShopSupport.touch(s, cart);
        RequestUtil.sendJson(resp, 200, ShopSupport.cartJson(cart));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        HttpSession s = req.getSession(true);
        Cart cart = ShopSupport.cart(s);
        String id = productId(req);
        if (id == null) {
            cart.clear();
        } else if (!cart.remove(id)) {
            RequestUtil.sendError(resp, 404, "Cet article n'est pas dans le panier.");
            return;
        }
        ShopSupport.touch(s, cart);
        RequestUtil.sendJson(resp, 200, ShopSupport.cartJson(cart));
    }

    static String productId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.length() < 2) {
            return null;
        }
        String id = info.substring(1);
        return id.matches("p\\d{2}") ? id : "invalide";
    }
}
