package fr.efiacademy.lab.chat;

import java.security.SecureRandom;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Crée le salon au démarrage de l'application.
 * <p>
 * Le code administrateur de démonstration n'est jamais codé en dur :
 * <ol>
 * <li>propriété système JVM {@code -Defi.chat.adminCode=...} si définie ;</li>
 * <li>sinon, code aléatoire généré au démarrage et écrit dans SystemOut.log.</li>
 * </ol>
 */
public class ChatContextListener implements ServletContextListener {

    public static final String ROOM_ATTRIBUTE = "efi.chat.room";
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        String code = null;
        try {
            code = System.getProperty("efi.chat.adminCode");
        } catch (SecurityException ignored) {
            // Java 2 Security active : on génère un code
        }
        boolean generated = false;
        if (code == null || code.trim().length() < 4) {
            code = randomCode(8);
            generated = true;
        }
        ctx.setAttribute(ROOM_ATTRIBUTE, new ChatRoom(code.trim()));
        if (generated) {
            // Message volontairement visible dans SystemOut.log (exercice : consulter les journaux)
            System.out.println("[EFI Chat] Code administrateur de démonstration généré pour cette JVM : " + code);
        } else {
            System.out.println("[EFI Chat] Code administrateur fourni par la propriété JVM efi.chat.adminCode.");
        }
        ctx.log("[EFI Chat] Salon initialisé (stockage mémoire, " + ChatRoom.MAX_HISTORY + " messages max).");
    }

    public void contextDestroyed(ServletContextEvent event) {
        event.getServletContext().removeAttribute(ROOM_ATTRIBUTE);
    }

    static String randomCode(int length) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
