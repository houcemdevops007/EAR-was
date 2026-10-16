package fr.efiacademy.lab.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/** Tests des utilitaires communs (JSON, HTML, formulaires, CSRF). */
public class CommonUtilsTest {

    @Test
    public void jsonWriterProducesValidStructure() {
        String json = new JsonWriter().beginObject()
                .field("a", "x")
                .field("n", 3)
                .field("b", true)
                .name("list").beginArray().value(1).value("deux").nullValue().endArray()
                .name("o").beginObject().field("k", 1.5).endObject()
                .endObject().toString();
        assertEquals("{\"a\":\"x\",\"n\":3,\"b\":true,\"list\":[1,\"deux\",null],\"o\":{\"k\":1.5}}", json);
    }

    @Test
    public void jsonQuoteEscapesDangerousCharacters() {
        String q = JsonWriter.quote("<script>\"a\\b\"\n'&");
        assertEquals("\"\\u003cscript\\u003e\\\"a\\\\b\\\"\\n\\u0027\\u0026\"", q);
    }

    @Test
    public void jsonNaNBecomesNull() {
        assertEquals("[null]", new JsonWriter().beginArray().value(Double.NaN).endArray().toString());
    }

    @Test
    public void htmlEscapePreventsXss() {
        assertEquals("&lt;img src=x onerror=&quot;alert(1)&quot;&gt;", Html.esc("<img src=x onerror=\"alert(1)\">"));
        assertEquals("", Html.esc(null));
        assertEquals("&#39;&amp;&#47;", Html.esc("'&/"));
    }

    @Test
    public void parseFormDecodesUtf8() {
        Map<String, String> m = new HashMap<String, String>();
        RequestUtil.parseForm("titre=R%C3%A9union+DMGR&prio=haute&vide=&malforme=%E0%A4%A", m);
        assertEquals("Réunion DMGR", m.get("titre"));
        assertEquals("haute", m.get("prio"));
        assertEquals("", m.get("vide"));
        assertFalse(m.containsKey("malforme"));
    }

    @Test
    public void cleanRemovesControlCharsAndTruncates() {
        assertEquals("abc", RequestUtil.clean("  a\u0000b\u0007c  ", 10));
        assertEquals("abcde", RequestUtil.clean("abcdefgh", 5));
        assertEquals("", RequestUtil.clean(null, 5));
    }

    @Test
    public void constantTimeEquals() {
        assertTrue(Csrf.constantTimeEquals("abc123", "abc123"));
        assertFalse(Csrf.constantTimeEquals("abc123", "abc124"));
        assertFalse(Csrf.constantTimeEquals("abc", "abcd"));
    }

    @Test
    public void serverInfoNeverReturnsEmptyHostname() {
        assertTrue(ServerInfo.hostname().length() > 0);
        assertTrue(ServerInfo.badgeLabel().startsWith(ServerInfo.hostname()));
        assertTrue(ServerInfo.isoNow().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }
}
