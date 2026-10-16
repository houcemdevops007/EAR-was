package fr.efiacademy.lab.dashboard;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Compte les requêtes traitées par l'application (toutes URL confondues).
 * Le compteur est stocké dans le contexte de servlet : il est propre à
 * chaque JVM (serveur « test » et « test2 » ont donc des compteurs distincts,
 * ce qui est volontairement pédagogique).
 */
public class RequestCounterFilter implements Filter {

    public static final String ATTR = "efi.dashboard.requests";
    public static final String ATTR_ERRORS = "efi.dashboard.errors";

    private AtomicLong counter;
    private AtomicLong errors;

    public void init(FilterConfig config) throws ServletException {
        ServletContext ctx = config.getServletContext();
        counter = counter(ctx, ATTR);
        errors = counter(ctx, ATTR_ERRORS);
    }

    /** Retourne (en le créant au besoin) le compteur nommé du contexte. */
    public static synchronized AtomicLong counter(ServletContext ctx, String name) {
        Object o = ctx.getAttribute(name);
        if (o instanceof AtomicLong) {
            return (AtomicLong) o;
        }
        AtomicLong c = new AtomicLong();
        ctx.setAttribute(name, c);
        return c;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        counter.incrementAndGet();
        try {
            chain.doFilter(request, response);
        } catch (IOException e) {
            errors.incrementAndGet();
            throw e;
        } catch (ServletException e) {
            errors.incrementAndGet();
            throw e;
        } catch (RuntimeException e) {
            errors.incrementAndGet();
            throw e;
        }
    }

    public void destroy() {
        // rien à libérer
    }
}
