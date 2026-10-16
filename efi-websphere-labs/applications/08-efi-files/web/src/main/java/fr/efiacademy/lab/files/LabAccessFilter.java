package fr.efiacademy.lab.files;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Restreint l'accès au centre de fichiers aux réseaux du laboratoire.
 * <p>
 * Liste CIDR lue dans le paramètre de contexte {@code efi.files.allowedNetworks},
 * surchargeable par la propriété JVM {@code -Defi.files.allowedNetworks=...}.
 * La valeur « * » désactive le filtrage (déconseillé). {@code /api/health} reste public.
 * <p>
 * Derrière IBM HTTP Server + plug-in WebSphere, {@code getRemoteAddr()} renvoie
 * l'adresse du client transmise par le plug-in.
 */
public class LabAccessFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(LabAccessFilter.class.getName());
    public static final String PARAM = "efi.files.allowedNetworks";

    private final List<Cidr> networks = new ArrayList<Cidr>();
    private boolean allowAll;

    public void init(FilterConfig config) throws ServletException {
        String value = System.getProperty(PARAM);
        if (value == null || value.trim().length() == 0) {
            value = config.getServletContext().getInitParameter(PARAM);
        }
        configure(value);
        LOG.info("EFI File Center : réseaux autorisés = " + (allowAll ? "*" : value));
    }

    void configure(String csv) {
        networks.clear();
        allowAll = false;
        if (csv == null) {
            return;
        }
        for (String part : csv.split(",")) {
            String p = part.trim();
            if ("*".equals(p)) {
                allowAll = true;
            } else if (p.length() > 0) {
                try {
                    networks.add(Cidr.parse(p));
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "Réseau ignoré (format invalide) : " + p);
                }
            }
        }
    }

    boolean isAllowed(String remoteAddr) {
        if (allowAll) {
            return true;
        }
        byte[] addr = Cidr.literal(remoteAddr);
        if (addr == null) {
            return false;
        }
        for (Cidr c : networks) {
            if (c.contains(addr)) {
                return true;
            }
        }
        return false;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if ("/api/health".equals(path) || path.startsWith("/assets/") || path.startsWith("/erreur/")
                || isAllowed(req.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }
        LOG.warning("EFI File Center : accès refusé pour " + req.getRemoteAddr());
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN,
                "Accès réservé au réseau du laboratoire.");
    }

    public void destroy() {
        networks.clear();
    }

    /** Bloc CIDR IPv4 ou IPv6 (adresses littérales uniquement : aucune résolution DNS). */
    static final class Cidr {
        private final byte[] network;
        private final int prefix;

        private Cidr(byte[] network, int prefix) {
            this.network = network;
            this.prefix = prefix;
        }

        static Cidr parse(String s) {
            String ip = s;
            int prefix = -1;
            int slash = s.indexOf('/');
            if (slash >= 0) {
                ip = s.substring(0, slash);
                try {
                    prefix = Integer.parseInt(s.substring(slash + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(s);
                }
            }
            byte[] a = literal(ip);
            if (a == null) {
                throw new IllegalArgumentException(s);
            }
            int bits = a.length * 8;
            if (prefix < 0) {
                prefix = bits;
            }
            if (prefix > bits) {
                throw new IllegalArgumentException(s);
            }
            return new Cidr(a, prefix);
        }

        /** Convertit une adresse IP littérale en octets (null si ce n'est pas un littéral). */
        static byte[] literal(String ip) {
            if (ip == null) {
                return null;
            }
            String s = ip.trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                s = s.substring(1, s.length() - 1);
            }
            int zone = s.indexOf('%');
            if (zone >= 0) {
                s = s.substring(0, zone);
            }
            boolean v4 = s.matches("\\d{1,3}(\\.\\d{1,3}){3}");
            boolean v6 = s.indexOf(':') >= 0 && s.matches("[0-9a-fA-F:.]+");
            if (!v4 && !v6) {
                return null;
            }
            if (v4) {
                for (String o : s.split("\\.")) {
                    if (Integer.parseInt(o) > 255) {
                        return null;
                    }
                }
            }
            try {
                byte[] b = InetAddress.getByName(s).getAddress();
                // IPv4 mappée en IPv6 (::ffff:a.b.c.d) → IPv4
                if (b.length == 16 && isMapped(b)) {
                    byte[] v = new byte[4];
                    System.arraycopy(b, 12, v, 0, 4);
                    return v;
                }
                return b;
            } catch (UnknownHostException e) {
                return null;
            }
        }

        private static boolean isMapped(byte[] b) {
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) {
                    return false;
                }
            }
            return (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF;
        }

        boolean contains(byte[] addr) {
            if (addr.length != network.length) {
                return false;
            }
            int full = prefix / 8;
            for (int i = 0; i < full; i++) {
                if (addr[i] != network[i]) {
                    return false;
                }
            }
            int rem = prefix % 8;
            if (rem == 0) {
                return true;
            }
            int mask = (0xFF << (8 - rem)) & 0xFF;
            return (addr[full] & mask) == (network[full] & mask);
        }
    }
}
