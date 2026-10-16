package fr.efiacademy.lab.it;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import fr.efiacademy.lab.it.Browser.Response;

/**
 * Scénarios fonctionnels de bout en bout, application par application,
 * rejoués en HTTP sur les WAR réellement produits (dist/war).
 */
public class AppScenariosIT {

    private static Browser open(String ctx) throws Exception {
        Browser b = new Browser(LabServer.base());
        Response r = b.page(ctx + "/");
        assertEquals(r.toString(), 200, r.status);
        assertNotNull("jeton CSRF publié", b.csrf());
        return b;
    }

    private static String first(String regex, String body) {
        Matcher m = Pattern.compile(regex).matcher(body);
        assertTrue("motif " + regex + " absent de : " + body, m.find());
        return m.group(1);
    }

    private static int intOf(String key, String json) {
        return Integer.parseInt(first("\"" + key + "\":(\\d+)", json));
    }

    // ---------------------------------------------------------------- 01
    @Test
    public void app01_welcome() throws Exception {
        Browser b = open("/efi-welcome");
        Response home = b.get("/efi-welcome/");
        assertTrue(home.body.contains("Bienvenue dans la formation WebSphere EFI-Academy"));
        Response t = b.get("/efi-welcome/api/server-time");
        assertEquals(t.toString(), 200, t.status);
        assertTrue(t.body.contains("\"hostname\""));
    }

    // ---------------------------------------------------------------- 02
    @Test
    public void app02_calendrierEtMeteo() throws Exception {
        Browser b = open("/efi-calendar");
        Response w = b.get("/efi-calendar/api/weather");
        if (w.status == 200) {
            assertTrue(w.body, w.body.contains("\"source\":\"server\""));
            System.out.println("[METEO] Open-Meteo joignable : données réelles reçues (" + w.bytes.length + " o)");
        } else {
            assertTrue("dégradation propre attendue : " + w, w.status >= 502 && w.status <= 504 && w.body.contains("\"error\""));
            System.out.println("[METEO] service indisponible, dégradation propre : " + w);
        }
    }

    // ---------------------------------------------------------------- 03
    @Test
    public void app03_chatSessionXssEtAdmin() throws Exception {
        Browser alice = open("/efi-chat");
        assertEquals(403, alice.postNoCsrf("/efi-chat/api/chat/join", Browser.form("nickname", "Alice")).status);
        Browser anonymous = open("/efi-chat");
        assertEquals(401, anonymous.post("/efi-chat/api/chat/send", Browser.form("text", "bonjour")).status);

        Response j = alice.post("/efi-chat/api/chat/join", Browser.form("nickname", "Alice"));
        assertEquals(j.toString(), 200, j.status);
        Response s = alice.post("/efi-chat/api/chat/send", Browser.form("text", "<img src=x onerror=alert(1)> Bonjour é"));
        assertEquals(s.toString(), 200, s.status);
        char[] longText = new char[600];
        Arrays.fill(longText, 'a');
        Response tooLong = alice.post("/efi-chat/api/chat/send", Browser.form("text", new String(longText)));
        assertTrue("message trop long refusé ou tronqué : " + tooLong, tooLong.status == 400 || tooLong.status == 200);

        Response m = alice.get("/efi-chat/api/chat/messages?since=0");
        assertEquals(200, m.status);
        assertTrue("message présent", m.body.contains("Bonjour é"));
        assertFalse("aucun HTML brut renvoyé", m.body.contains("<img"));
        assertTrue("utilisateur actif", m.body.contains("Alice"));

        assertEquals(403, alice.post("/efi-chat/api/chat/clear", null).status);
        assertEquals(403, alice.post("/efi-chat/api/chat/admin", Browser.form("code", "mauvais")).status);
        assertEquals(200, alice.post("/efi-chat/api/chat/admin", Browser.form("code", "code-de-test-integration")).status);
        assertEquals(200, alice.post("/efi-chat/api/chat/clear", null).status);
        assertFalse(alice.get("/efi-chat/api/chat/messages?since=0").body.contains("Bonjour é"));
    }

    // ---------------------------------------------------------------- 04
    @Test
    public void app04_tachesCrud() throws Exception {
        Browser b = open("/efi-tasks");
        Response list = b.get("/efi-tasks/api/tasks");
        assertEquals(200, list.status);
        assertEquals(400, b.post("/efi-tasks/api/tasks", Browser.form("title", "", "priority", "high")).status);

        Response c = b.post("/efi-tasks/api/tasks", Browser.form("title", "Déployer efi-tasks.ear", "description", "<b>TP</b>",
                "priority", "high", "status", "todo", "dueDate", "2026-12-31"));
        assertEquals(c.toString(), 201, c.status);
        int id = intOf("id", c.body);
        assertFalse(c.body.contains("<b>"));

        Response mv = b.put("/efi-tasks/api/tasks/" + id + "/status", Browser.form("status", "done"));
        assertEquals(mv.toString(), 200, mv.status);
        assertTrue(mv.body.contains("\"done\""));
        Response up = b.put("/efi-tasks/api/tasks/" + id, Browser.form("title", "Déployer et vérifier", "description", "",
                "priority", "low", "status", "doing", "dueDate", ""));
        assertEquals(up.toString(), 200, up.status);
        assertTrue(up.body.contains("Déployer et vérifier"));
        assertEquals(400, b.put("/efi-tasks/api/tasks/" + id + "/status", Browser.form("status", "nimporte")).status);
        assertEquals(200, b.delete("/efi-tasks/api/tasks/" + id).status);
        assertEquals(404, b.get("/efi-tasks/api/tasks/" + id).status);
    }

    // ---------------------------------------------------------------- 05
    @Test
    public void app05_quizCompletAvecScore() throws Exception {
        Browser b = open("/efi-quiz");
        Response st = b.post("/efi-quiz/quiz", Browser.form("action", "start", "theme", "all", "count", "10"));
        assertEquals(st.toString(), 302, st.status);
        assertTrue(st.header("Location").endsWith("/efi-quiz/quiz"));
        int answered = 0;
        String location = null;
        for (int i = 0; i < 40 && location == null; i++) {
            Response q = b.page("/efi-quiz/quiz");
            assertEquals(q.toString(), 200, q.status);
            if (q.body.contains("name=\"action\" value=\"answer\"")) {
                Response a = b.post("/efi-quiz/quiz", Browser.form("action", "answer", "choice", "0"));
                assertEquals(302, a.status);
                answered++;
            } else {
                Response n = b.post("/efi-quiz/quiz", Browser.form("action", "next"));
                assertEquals(302, n.status);
                if (n.header("Location").endsWith("/quiz/result")) {
                    location = n.header("Location");
                }
            }
        }
        assertEquals("10 questions répondues", 10, answered);
        assertNotNull("redirection vers le résultat", location);
        Response res = b.get("/efi-quiz/quiz/result");
        assertEquals(200, res.status);
        assertTrue("score sur 10 affiché", res.body.contains("/ 10") || res.body.contains("/10") || res.body.contains("sur 10"));
    }

    // ---------------------------------------------------------------- 06
    @Test
    public void app06_metriquesSansDonneesSensibles() throws Exception {
        Browser b = open("/efi-dashboard");
        Response m1 = b.get("/efi-dashboard/api/metrics");
        assertEquals(200, m1.status);
        assertTrue(m1.body.contains("\"heapUsed\"") && m1.body.contains("\"processors\"") && m1.body.contains("\"uptimeText\""));
        int r1 = intOf("requests", m1.body);
        b.get("/efi-dashboard/api/health");
        int r2 = intOf("requests", b.get("/efi-dashboard/api/metrics").body);
        assertTrue("le compteur de requêtes progresse (" + r1 + " → " + r2 + ")", r2 > r1);
        String low = m1.body.toLowerCase();
        for (String s : new String[] {"password", "passwd", "secret", "token", "java.class.path", "user.home", "\"path\""}) {
            assertFalse("donnée sensible exposée : " + s, low.contains(s));
        }
    }

    // ---------------------------------------------------------------- 07 / 09
    @Test
    public void app07_notesEtApp09_calculatrice() throws Exception {
        Browser n = open("/efi-notes");
        assertTrue(n.get("/efi-notes/").body.contains("notes-core.js"));
        assertEquals(200, n.get("/efi-notes/assets/js/notes-core.js").status);
        Browser c = open("/efi-calculator");
        Response core = c.get("/efi-calculator/assets/js/calc-core.js");
        assertEquals(200, core.status);
        assertFalse("pas d'eval()", Pattern.compile("\\beval\\s*\\(").matcher(core.body).find());
        assertFalse("pas de new Function", core.body.contains("new Function"));
    }

    // ---------------------------------------------------------------- 08
    @Test
    public void app08_fichiersUploadTelechargementSecurite() throws Exception {
        Browser b = open("/efi-files");
        Response l = b.get("/efi-files/api/files");
        assertEquals(200, l.status);
        assertTrue("répertoire inscriptible", l.body.contains("\"writable\":true"));

        byte[] content = "Compte rendu du TP WebSphere — é à ç\n".getBytes(StandardCharsets.UTF_8);
        assertEquals(403, b.upload("/efi-files/api/files", "rapport.txt", content, false).status);
        Response up = b.upload("/efi-files/api/files", "rapport TP.txt", content, true);
        assertEquals(up.toString(), 201, up.status);
        String id = first("\"id\":\"([0-9a-f]{32})\"", up.body);

        Response dl = b.get("/efi-files/api/files/" + id);
        assertEquals(200, dl.status);
        assertTrue(dl.header("Content-Disposition").startsWith("attachment;"));
        assertEquals("application/octet-stream", dl.header("Content-Type"));
        assertArrayEquals(content, dl.bytes);

        Response jsp = b.upload("/efi-files/api/files", "shell.jsp", "<% out.println(1); %>".getBytes(StandardCharsets.UTF_8), true);
        assertTrue("extension .jsp refusée : " + jsp, jsp.status >= 400 && jsp.body.contains("\"errors\""));
        Response trav = b.upload("/efi-files/api/files", "../../evil.txt", "x".getBytes(StandardCharsets.UTF_8), true);
        assertEquals(trav.toString(), 201, trav.status);
        assertFalse("nom assaini", first("\"name\":\"([^\"]*)\"", trav.body).contains("/"));
        assertFalse("aucune écriture hors du répertoire", new File(LabServer.filesDir().getParentFile(), "evil.txt").exists());
        Response big = b.upload("/efi-files/api/files", "gros.txt", new byte[70 * 1024], true);
        assertEquals("limite de taille (64 Ko en test) : " + big, 413, big.status);

        for (String f : LabServer.filesDir().list()) {
            assertFalse("noms générés côté serveur : " + f, f.contains("rapport") || f.contains("evil"));
        }
        assertEquals(200, b.delete("/efi-files/api/files/" + id).status);
        assertEquals(404, b.get("/efi-files/api/files/" + id).status);
    }

    // ---------------------------------------------------------------- 10
    @Test
    public void app10_boutiquePanierCommande() throws Exception {
        Browser b = open("/efi-shop");
        Response cat = b.get("/efi-shop/api/catalog");
        assertEquals(200, cat.status);
        assertTrue("au moins 12 produits", cat.body.split("\"id\":\"p").length - 1 >= 12);

        Response empty = b.get("/efi-shop/checkout");
        assertEquals(302, empty.status);
        assertTrue(empty.header("Location").endsWith("/?panier=vide"));

        assertEquals(403, b.postNoCsrf("/efi-shop/api/cart", Browser.form("productId", "p01", "quantity", "1")).status);
        Response add = b.post("/efi-shop/api/cart", Browser.form("productId", "p01", "quantity", "2"));
        assertEquals(add.toString(), 200, add.status);
        assertEquals(2, intOf("count", add.body));
        b.post("/efi-shop/api/cart", Browser.form("productId", "p02", "quantity", "1"));
        Response put = b.put("/efi-shop/api/cart/p01", Browser.form("quantity", "3"));
        assertEquals(4, intOf("count", put.body));
        Response del = b.delete("/efi-shop/api/cart/p02");
        assertEquals(3, intOf("count", del.body));
        assertEquals(400, b.post("/efi-shop/api/cart", Browser.form("productId", "zz", "quantity", "1")).status);

        Response co = b.page("/efi-shop/checkout");
        assertEquals(200, co.status);
        Response bad = b.post("/efi-shop/checkout", Browser.form("_csrf", b.csrf(), "name", "", "email", "pas-un-mail"));
        assertEquals(422, bad.status);
        assertTrue(bad.body.contains("error-summary"));
        Response ok = b.post("/efi-shop/checkout", Browser.form("_csrf", b.csrf(), "name", "Candidat EFI",
                "email", "candidat@example.org", "address", "1 rue du Labo", "zip", "75001", "city", "Paris",
                "shipping", "express", "terms", "on"));
        assertEquals(ok.toString(), 302, ok.status);
        assertTrue(ok.header("Location").endsWith("/efi-shop/confirmation"));
        Response conf = b.get("/efi-shop/confirmation");
        assertEquals(200, conf.status);
        assertTrue("référence de commande", Pattern.compile("EFI-\\d{8}-[A-Z2-9]{5}").matcher(conf.body).find());
        assertTrue(conf.body.contains("Simulation pédagogique"));
        assertEquals("panier vidé après commande", 0, intOf("count", b.get("/efi-shop/api/cart").body));
    }
}
