package fr.efiacademy.lab.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MetricsCollectorTest {

    @Test
    public void formatDurationInFrench() {
        assertEquals("00 min 00 s", MetricsCollector.formatDuration(0));
        assertEquals("01 min 05 s", MetricsCollector.formatDuration(65000));
        assertEquals("02 h 00 min 01 s", MetricsCollector.formatDuration(7201000));
        assertEquals("3 j 00 h 00 min 00 s", MetricsCollector.formatDuration(3L * 86400000));
        assertEquals("00 min 00 s", MetricsCollector.formatDuration(-5));
    }

    @Test
    public void round2HandlesSpecialValues() {
        assertEquals(-1.0, MetricsCollector.round2(Double.NaN), 0.0);
        assertEquals(-1.0, MetricsCollector.round2(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(1.23, MetricsCollector.round2(1.2345), 0.0);
    }

    @Test
    public void jsonContainsExpectedSectionsAndNoSecrets() {
        MetricsCollector.AppCounters app = new MetricsCollector.AppCounters();
        app.requests = 42;
        app.errors = 1;
        app.activeSessions = 3;
        app.createdSessions = 7;
        long now = System.currentTimeMillis();
        app.appStartedAt = now - 61000;
        String json = MetricsCollector.toJson(app, now);

        assertTrue(json.startsWith("{") && json.endsWith("}"));
        for (String key : new String[] {"\"server\"", "\"hostname\"", "\"java\"", "\"version\"", "\"os\"",
                "\"processors\"", "\"memory\"", "\"heapUsed\"", "\"heapMax\"", "\"jvm\"", "\"uptimeMs\"",
                "\"threads\"", "\"gc\"", "\"app\"", "\"requests\":42", "\"activeSessions\":3"}) {
            assertTrue("clé manquante " + key, json.contains(key));
        }
        assertTrue(json.contains("\"uptimeText\":\"01 min 01 s\""));
        // aucune donnée sensible : ni variables d'environnement, ni arguments JVM, ni classpath
        String lower = json.toLowerCase();
        for (String forbidden : new String[] {"password", "passwd", "classpath", "inputarguments", "user.home",
                "java.home", "\"env\"", "token", "secret"}) {
            assertFalse("donnée sensible exposée : " + forbidden, lower.contains(forbidden));
        }
    }
}
