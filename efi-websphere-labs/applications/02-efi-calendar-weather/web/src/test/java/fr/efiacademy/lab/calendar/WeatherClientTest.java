package fr.efiacademy.lab.calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Tests réels du client HTTP contre un serveur local (aucun accès Internet
 * requis) : succès, cache, HTTP 500, délai dépassé, réponse non JSON.
 */
public class WeatherClientTest {

    private HttpServer server;
    private int port;
    private volatile int calls;

    @Before
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", handler(200, "{\"current\":{\"temperature_2m\":18.5}}", 0));
        server.createContext("/err", handler(500, "{}", 0));
        server.createContext("/slow", handler(200, "{}", 1500));
        server.createContext("/html", handler(200, "<html>proxy</html>", 0));
        server.start();
        port = server.getAddress().getPort();
    }

    @After
    public void stop() {
        server.stop(0);
    }

    private HttpHandler handler(final int status, final String body, final long delay) {
        return new HttpHandler() {
            public void handle(HttpExchange ex) throws IOException {
                calls++;
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
                byte[] b = body.getBytes("UTF-8");
                ex.sendResponseHeaders(status, b.length);
                OutputStream os = ex.getResponseBody();
                os.write(b);
                os.close();
            }
        };
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    @Test
    public void fetchesAndCaches() throws Exception {
        WeatherClient c = new WeatherClient(url("/ok"), 2000, 2000, 60000);
        WeatherClient.Result r1 = c.fetch();
        assertTrue(r1.body.contains("18.5"));
        assertFalse(r1.cached);
        WeatherClient.Result r2 = c.fetch();
        assertTrue(r2.cached);
        assertEquals(1, calls);
    }

    @Test
    public void httpErrorIsReported() {
        try {
            new WeatherClient(url("/err"), 2000, 2000, 0).fetch();
            fail("exception attendue");
        } catch (WeatherException e) {
            assertEquals("http", e.getCategory());
            assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    public void readTimeoutIsReported() {
        try {
            new WeatherClient(url("/slow"), 2000, 300, 0).fetch();
            fail("exception attendue");
        } catch (WeatherException e) {
            assertEquals("delai", e.getCategory());
        }
    }

    @Test
    public void nonJsonIsRejected() {
        try {
            new WeatherClient(url("/html"), 2000, 2000, 0).fetch();
            fail("exception attendue");
        } catch (WeatherException e) {
            assertEquals("reponse", e.getCategory());
        }
    }

    @Test
    public void classifiesNetworkErrors() {
        assertEquals("dns", WeatherClient.classify(new UnknownHostException("x"), "h").getCategory());
        assertEquals("delai", WeatherClient.classify(new SocketTimeoutException(), "h").getCategory());
        assertEquals("tls", WeatherClient.classify(new SSLHandshakeException("x"), "h").getCategory());
        assertEquals("connexion", WeatherClient.classify(new ConnectException(), "h").getCategory());
    }

    @Test
    public void defaultUrlTargetsParis() {
        assertTrue(WeatherClient.DEFAULT_URL.contains("latitude=48.8566"));
        assertTrue(WeatherClient.DEFAULT_URL.contains("longitude=2.3522"));
        assertTrue(WeatherClient.DEFAULT_URL.contains("Europe%2FParis"));
        assertEquals("api.open-meteo.com", new WeatherClient(WeatherClient.DEFAULT_URL, 1, 1, 1).getHost());
    }
}
