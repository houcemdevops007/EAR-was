package fr.efiacademy.lab.welcome;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServerTimeServletTest {

    @Test
    public void jsonContainsEpochAndTimezone() {
        String json = ServerTimeServlet.buildJson(1234567890L);
        assertTrue(json.startsWith("{\"epochMillis\":1234567890,"));
        assertTrue(json.contains("\"timezone\":\"Europe/Paris\""));
        assertTrue(json.contains("\"hostname\":\""));
    }
}
