package fr.efiacademy.lab.shop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fr.efiacademy.lab.common.Csrf;

/**
 * Validation de commande simulée (MVC + Post/Redirect/Get).
 * <pre>
 * GET  /checkout      formulaire de livraison + récapitulatif (JSP)
 * POST /checkout      validation → /confirmation, ou réaffichage avec erreurs
 * GET  /confirmation  page de confirmation de la dernière commande
 * </pre>
 * Aucun paiement, aucune transmission : la commande n'existe que dans la session.
 */
public class CheckoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(CheckoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(true);
        String ctx = req.getContextPath();
        if ("/confirmation".equals(req.getServletPath())) {
            Order o = (Order) s.getAttribute(Order.SESSION_KEY);
            if (o == null) {
                resp.sendRedirect(ctx + "/");
                return;
            }
            req.setAttribute("order", o);
            req.getRequestDispatcher("/WEB-INF/jsp/confirmation.jsp").forward(req, resp);
            return;
        }
        Cart cart = ShopSupport.cart(s);
        if (cart.isEmpty()) {
            resp.sendRedirect(ctx + "/?panier=vide");
            return;
        }
        req.setAttribute("cart", cart);
        req.setAttribute("form", Order.clean(new HashMap<String, String>()));
        req.setAttribute("errors", new HashMap<String, String>());
        req.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession s = req.getSession(true);
        String ctx = req.getContextPath();
        if ("/confirmation".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Cart cart = ShopSupport.cart(s);
        if (cart.isEmpty()) {
            resp.sendRedirect(ctx + "/?panier=vide");
            return;
        }
        Map<String, String> raw = new HashMap<String, String>();
        for (String k : new String[] {"name", "email", "address", "zip", "city", "shipping", "terms"}) {
            raw.put(k, req.getParameter(k));
        }
        Map<String, String> form = Order.clean(raw);
        Map<String, String> errors = Order.validate(form);
        if (!Csrf.isValid(req)) {
            errors.put("_csrf", "Session expirée ou formulaire invalide : veuillez renvoyer le formulaire.");
        }
        if (!errors.isEmpty()) {
            resp.setStatus(422);
            req.setAttribute("cart", cart);
            req.setAttribute("form", form);
            req.setAttribute("errors", errors);
            req.getRequestDispatcher("/WEB-INF/jsp/checkout.jsp").forward(req, resp);
            return;
        }
        Order order = Order.place(form, cart, System.currentTimeMillis());
        s.setAttribute(Order.SESSION_KEY, order);
        cart.clear();
        ShopSupport.touch(s, cart);
        LOG.info("EFI Mini Shop : commande simulée " + order.getReference() + " (" + order.getItemCount() + " article(s))");
        resp.sendRedirect(ctx + "/confirmation");
    }
}
