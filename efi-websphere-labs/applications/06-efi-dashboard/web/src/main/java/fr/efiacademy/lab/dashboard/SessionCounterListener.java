package fr.efiacademy.lab.dashboard;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Compte les sessions HTTP actives et créées depuis le démarrage de l'application
 * (dans la JVM courante).
 */
public class SessionCounterListener implements HttpSessionListener {

    private static final AtomicInteger ACTIVE = new AtomicInteger();
    private static final AtomicLong CREATED = new AtomicLong();

    public void sessionCreated(HttpSessionEvent se) {
        ACTIVE.incrementAndGet();
        CREATED.incrementAndGet();
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        // ne descend jamais sous zéro (sessions restaurées après redémarrage)
        while (true) {
            int cur = ACTIVE.get();
            if (cur <= 0 || ACTIVE.compareAndSet(cur, cur - 1)) {
                return;
            }
        }
    }

    public static int active() {
        return ACTIVE.get();
    }

    public static long created() {
        return CREATED.get();
    }
}
