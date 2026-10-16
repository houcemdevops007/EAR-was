package fr.efiacademy.lab.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Contrôles communs aux 10 applications, sur un vrai conteneur Servlet 3.0 :
 * page d'accueil (identité EFI, SEO, en-têtes de sécurité), ressources référencées,
 * santé, page 404 personnalisée.
 */
@RunWith(Parameterized.class)
public class CommonPagesIT {

    private static final Pattern LOCAL_REF = Pattern.compile("(?:src|href)=\"(/efi-[a-z-]+/[^\"#?]*)\"");

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> l = new ArrayList<Object[]>();
        for (LabServer.App a : LabServer.apps()) {
            l.add(new Object[] {a});
        }
        return l;
    }

    private final LabServer.App app;

    public CommonPagesIT(LabServer.App app) {
        this.app = app;
    }

    private Browser browser() throws Exception {
        return new Browser(LabServer.base());
    }

    @Test
    public void accueilConformeALIdentiteEfi() throws Exception {
        Browser.Response r = browser().page(app.context + "/");
        assertEquals(app.name + " : " + r, 200, r.status);
        String b = r.body;
        assertTrue("charset UTF-8", r.header("Content-Type").toLowerCase().contains("utf-8"));
        assertTrue("lang=fr", b.contains("<html lang=\"fr\""));
        assertTrue("title", Pattern.compile("<title>[^<]{5,}</title>").matcher(b).find());
        assertTrue("meta description", b.contains("<meta name=\"description\" content=\""));
        assertEquals("un seul <h1>", 1, count(b, "<h1"));
        assertTrue("pied de page", b.contains("<strong>EFI Academy</strong> | Formation IBM WebSphere ND 8.5.5"));
        assertTrue("badge d'hôte", b.contains("host-badge"));
        assertNotNull("CSP", r.header("Content-Security-Policy"));
        assertEquals("nosniff", "nosniff", r.header("X-Content-Type-Options"));
        assertTrue("aucun script inline (CSP)", !Pattern.compile("<script>(?!\\s*</script>)").matcher(b).find());
    }

    @Test
    public void ressourcesReferenceesDisponibles() throws Exception {
        Browser br = browser();
        String b = br.page(app.context + "/").body;
        Set<String> refs = new LinkedHashSet<String>();
        Matcher m = LOCAL_REF.matcher(b);
        while (m.find()) {
            String ref = m.group(1);
            if (ref.contains("/assets/")) {
                refs.add(ref);
            }
        }
        assertTrue(app.name + " : au moins CSS + JS référencés", refs.size() >= 3);
        for (String ref : refs) {
            Browser.Response r = br.get(ref);
            assertEquals(app.name + " : ressource " + ref, 200, r.status);
            assertTrue(ref + " non vide", r.bytes.length > 0);
            if (ref.endsWith(".css")) {
                assertTrue(ref + " text/css", r.header("Content-Type").startsWith("text/css"));
            }
        }
        System.out.println("[HTTP] " + app.name + " : " + refs.size() + " ressources statiques OK");
    }

    @Test
    public void santeUp() throws Exception {
        Browser.Response r = browser().get(app.context + "/api/health");
        assertEquals(r.toString(), 200, r.status);
        assertTrue(r.header("Content-Type").startsWith("application/json"));
        assertTrue(r.body.contains("\"status\":\"UP\""));
        assertTrue(r.body.contains("\"contextRoot\":\"" + app.context + "\""));
    }

    @Test
    public void page404Personnalisee() throws Exception {
        Browser.Response r = browser().get(app.context + "/page-inexistante-" + System.nanoTime());
        assertEquals(404, r.status);
        assertTrue("page 404 EFI", r.body.contains("404") && r.body.contains("Formation IBM WebSphere ND 8.5.5"));
    }

    private static int count(String s, String sub) {
        int n = 0;
        int i = 0;
        while ((i = s.indexOf(sub, i)) >= 0) {
            n++;
            i += sub.length();
        }
        return n;
    }
}
