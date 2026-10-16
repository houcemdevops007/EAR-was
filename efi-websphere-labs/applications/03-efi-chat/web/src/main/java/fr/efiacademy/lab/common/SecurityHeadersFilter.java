package fr.efiacademy.lab.common;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Filtre appliqué à toutes les requêtes :
 * <ul>
 * <li>encodage UTF-8 des paramètres de requête ;</li>
 * <li>en-têtes de sécurité HTTP (CSP, nosniff, anti-clickjacking).</li>
 * </ul>
 * La politique CSP peut être surchargée par le paramètre d'init « csp ».
 */
public class SecurityHeadersFilter implements Filter {

    static final String DEFAULT_CSP = "default-src 'self'; img-src 'self' data:; "
            + "style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; "
            + "font-src 'self' data:; object-src 'none'; frame-ancestors 'self'; "
            + "base-uri 'self'; form-action 'self'";

    private String csp = DEFAULT_CSP;

    public void init(FilterConfig config) throws ServletException {
        String c = config.getInitParameter("csp");
        if (c != null && c.trim().length() > 0) {
            csp = c.trim().replaceAll("\\s+", " ");
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }
        if (response instanceof HttpServletResponse) {
            HttpServletResponse r = (HttpServletResponse) response;
            r.setHeader("X-Content-Type-Options", "nosniff");
            r.setHeader("X-Frame-Options", "SAMEORIGIN");
            r.setHeader("Referrer-Policy", "same-origin");
            r.setHeader("Content-Security-Policy", csp);
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
        // rien à libérer
    }
}
