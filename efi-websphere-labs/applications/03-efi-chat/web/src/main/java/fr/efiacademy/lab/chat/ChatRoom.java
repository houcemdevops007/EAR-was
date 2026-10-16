package fr.efiacademy.lab.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Salon de discussion en mémoire (portée application, thread-safe).
 * <p>
 * LIMITE PÉDAGOGIQUE VOLONTAIRE : les messages sont stockés dans la JVM du
 * serveur d'applications. Ils sont perdus au redémarrage et ne sont pas
 * partagés entre les serveurs « test » et « test2 ». Évolution prévue en TP
 * avancé : persistance JDBC via une source de données WebSphere.
 */
public class ChatRoom {

    public static final int MAX_HISTORY = 100;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public static final int NICK_MIN = 2;
    public static final int NICK_MAX = 20;
    public static final long PRESENCE_TIMEOUT_MS = 30000L;
    public static final int RATE_MAX_MESSAGES = 5;
    public static final long RATE_WINDOW_MS = 10000L;

    private static final Pattern NICK_PATTERN = Pattern.compile("[\\p{L}\\p{N}_.\\- ]+");
    private static final String[] RESERVED = {"admin", "administrateur", "système", "systeme", "system", "efi"};

    private final LinkedList<ChatMessage> messages = new LinkedList<ChatMessage>();
    /** pseudo (minuscules) → présence */
    private final Map<String, Presence> users = new HashMap<String, Presence>();
    /** identifiant de session → horodatages des derniers envois */
    private final Map<String, LinkedList<Long>> rates = new HashMap<String, LinkedList<Long>>();
    private final String adminCode;
    private long nextId = 1;
    /** Change à chaque vidage et à chaque démarrage : permet aux clients de se resynchroniser. */
    private long generation = System.currentTimeMillis();

    private static final class Presence {
        final String nickname;
        final String sessionId;
        long lastSeen;

        Presence(String nickname, String sessionId, long lastSeen) {
            this.nickname = nickname;
            this.sessionId = sessionId;
            this.lastSeen = lastSeen;
        }
    }

    public ChatRoom(String adminCode) {
        this.adminCode = adminCode;
    }

    /** Valide un pseudonyme ; retourne un message d'erreur ou null si valide. */
    public static String validateNickname(String nick) {
        if (nick == null || nick.trim().length() < NICK_MIN) {
            return "Le pseudonyme doit contenir au moins " + NICK_MIN + " caractères.";
        }
        String n = nick.trim();
        if (n.length() > NICK_MAX) {
            return "Le pseudonyme ne doit pas dépasser " + NICK_MAX + " caractères.";
        }
        if (!NICK_PATTERN.matcher(n).matches()) {
            return "Caractères autorisés : lettres, chiffres, espace, point, tiret et souligné.";
        }
        String lower = n.toLowerCase(Locale.FRENCH);
        for (int i = 0; i < RESERVED.length; i++) {
            if (lower.equals(RESERVED[i])) {
                return "Ce pseudonyme est réservé.";
            }
        }
        return null;
    }

    /** Valide un message ; retourne un message d'erreur ou null si valide. */
    public static String validateMessage(String text) {
        if (text == null || text.trim().length() == 0) {
            return "Le message est vide.";
        }
        if (text.length() > MAX_MESSAGE_LENGTH) {
            return "Le message ne doit pas dépasser " + MAX_MESSAGE_LENGTH + " caractères.";
        }
        return null;
    }

    /**
     * Inscrit un pseudo pour une session. Retourne null en cas de succès ou un
     * message d'erreur si le pseudo est déjà utilisé par une autre session active.
     */
    public synchronized String join(String nickname, String sessionId, long now) {
        purge(now);
        String key = nickname.toLowerCase(Locale.FRENCH);
        Presence p = users.get(key);
        if (p != null && !p.sessionId.equals(sessionId)) {
            return "Ce pseudonyme est déjà utilisé dans le salon.";
        }
        boolean isNew = p == null;
        users.put(key, new Presence(nickname, sessionId, now));
        if (isNew) {
            addSystem(nickname + " a rejoint le salon", now);
        }
        return null;
    }

    public synchronized void leave(String nickname, long now) {
        if (nickname != null && users.remove(nickname.toLowerCase(Locale.FRENCH)) != null) {
            addSystem(nickname + " a quitté le salon", now);
        }
    }

    /** Met à jour la présence (appelé à chaque polling). */
    public synchronized void touch(String nickname, String sessionId, long now) {
        if (nickname == null) {
            return;
        }
        String key = nickname.toLowerCase(Locale.FRENCH);
        Presence p = users.get(key);
        if (p == null) {
            users.put(key, new Presence(nickname, sessionId, now));
        } else if (p.sessionId.equals(sessionId)) {
            p.lastSeen = now;
        }
    }

    /** Vérifie la limite de débit : RATE_MAX_MESSAGES par RATE_WINDOW_MS et par session. */
    public synchronized boolean allowSend(String sessionId, long now) {
        LinkedList<Long> times = rates.get(sessionId);
        if (times == null) {
            times = new LinkedList<Long>();
            rates.put(sessionId, times);
        }
        while (!times.isEmpty() && now - times.getFirst().longValue() > RATE_WINDOW_MS) {
            times.removeFirst();
        }
        if (times.size() >= RATE_MAX_MESSAGES) {
            return false;
        }
        times.addLast(Long.valueOf(now));
        return true;
    }

    public synchronized ChatMessage post(String nickname, String text, long now) {
        ChatMessage m = new ChatMessage(nextId++, nickname, text, now, "user");
        append(m);
        return m;
    }

    private void addSystem(String text, long now) {
        append(new ChatMessage(nextId++, "Système", text, now, "system"));
    }

    private void append(ChatMessage m) {
        messages.addLast(m);
        while (messages.size() > MAX_HISTORY) {
            messages.removeFirst();
        }
    }

    /** Messages dont l'identifiant est strictement supérieur à sinceId. */
    public synchronized List<ChatMessage> since(long sinceId) {
        List<ChatMessage> out = new ArrayList<ChatMessage>();
        for (ChatMessage m : messages) {
            if (m.getId() > sinceId) {
                out.add(m);
            }
        }
        return out;
    }

    /** Pseudos actifs (vus depuis moins de PRESENCE_TIMEOUT_MS), triés. */
    public synchronized List<String> activeUsers(long now) {
        purge(now);
        List<String> out = new ArrayList<String>();
        for (Presence p : users.values()) {
            out.add(p.nickname);
        }
        Collections.sort(out, String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    private void purge(long now) {
        Iterator<Map.Entry<String, Presence>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            Presence p = it.next().getValue();
            if (now - p.lastSeen > PRESENCE_TIMEOUT_MS) {
                it.remove();
                addSystem(p.nickname + " est inactif et a quitté le salon", now);
            }
        }
        if (rates.size() > 1000) {
            rates.clear();
        }
    }

    /** Vérifie le code administrateur de démonstration. */
    public boolean checkAdminCode(String code) {
        if (code == null || adminCode == null) {
            return false;
        }
        String c = code.trim();
        if (c.length() != adminCode.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < c.length(); i++) {
            diff |= c.charAt(i) ^ adminCode.charAt(i);
        }
        return diff == 0;
    }

    public synchronized void clear(String by, long now) {
        messages.clear();
        generation++;
        addSystem("Le salon a été vidé par l'administrateur (" + by + ")", now);
    }

    public synchronized long lastId() {
        return messages.isEmpty() ? nextId - 1 : messages.getLast().getId();
    }

    public synchronized long generation() {
        return generation;
    }

    public synchronized int size() {
        return messages.size();
    }
}
