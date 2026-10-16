package fr.efiacademy.lab.tasks;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Magasin de tâches en mémoire, partagé par tous les utilisateurs de la JVM
 * (portée application). Thread-safe. Version initiale volontairement simple :
 * données perdues au redémarrage, non partagées entre serveurs.
 */
public class TaskStore {

    public static final int MAX_TASKS = 500;
    public static final int TITLE_MIN = 3;
    public static final int TITLE_MAX = 100;
    public static final int DESCRIPTION_MAX = 1000;

    private final Map<Long, Task> tasks = new LinkedHashMap<Long, Task>();
    private long nextId = 1;

    /** Résultat de validation : message d'erreur par champ. */
    public static final class Validation {
        public final Map<String, String> errors = new LinkedHashMap<String, String>();

        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    /** Valide et normalise les champs saisis. */
    public static Validation validate(String title, String description, String priority, String status, String dueDate) {
        Validation v = new Validation();
        if (title == null || title.trim().length() < TITLE_MIN) {
            v.errors.put("title", "Le titre doit contenir au moins " + TITLE_MIN + " caractères.");
        } else if (title.trim().length() > TITLE_MAX) {
            v.errors.put("title", "Le titre ne doit pas dépasser " + TITLE_MAX + " caractères.");
        }
        if (description != null && description.length() > DESCRIPTION_MAX) {
            v.errors.put("description", "La description ne doit pas dépasser " + DESCRIPTION_MAX + " caractères.");
        }
        if (!Task.isValidPriority(priority)) {
            v.errors.put("priority", "Priorité invalide (basse, moyenne ou haute).");
        }
        if (!Task.isValidStatus(status)) {
            v.errors.put("status", "Statut invalide (todo, doing ou done).");
        }
        if (dueDate != null && dueDate.length() > 0 && parseDate(dueDate) == null) {
            v.errors.put("dueDate", "Date d'échéance invalide (format AAAA-MM-JJ).");
        }
        return v;
    }

    /** Analyse stricte d'une date AAAA-MM-JJ ; null si invalide. */
    public static Date parseDate(String s) {
        if (s == null || !s.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        f.setLenient(false);
        f.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        try {
            return f.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    public synchronized Task create(String title, String description, String priority, String status,
            String dueDate, long now) {
        if (tasks.size() >= MAX_TASKS) {
            throw new IllegalStateException("Nombre maximal de tâches atteint (" + MAX_TASKS + ").");
        }
        Task t = new Task(nextId++, title.trim(), description == null ? "" : description.trim(), priority, status,
                emptyToNull(dueDate), now);
        tasks.put(Long.valueOf(t.getId()), t);
        return t.copy();
    }

    public synchronized Task update(long id, String title, String description, String priority, String status,
            String dueDate, long now) {
        Task t = tasks.get(Long.valueOf(id));
        if (t == null) {
            return null;
        }
        t.setTitle(title.trim());
        t.setDescription(description == null ? "" : description.trim());
        t.setPriority(priority);
        t.setStatus(status);
        t.setDueDate(emptyToNull(dueDate));
        t.touch(now);
        return t.copy();
    }

    public synchronized Task move(long id, String status, long now) {
        Task t = tasks.get(Long.valueOf(id));
        if (t == null) {
            return null;
        }
        t.setStatus(status);
        t.touch(now);
        return t.copy();
    }

    public synchronized boolean delete(long id) {
        return tasks.remove(Long.valueOf(id)) != null;
    }

    public synchronized Task get(long id) {
        Task t = tasks.get(Long.valueOf(id));
        return t == null ? null : t.copy();
    }

    /** Toutes les tâches, triées par priorité décroissante puis échéance. */
    public synchronized List<Task> list() {
        List<Task> out = new ArrayList<Task>(tasks.size());
        for (Task t : tasks.values()) {
            out.add(t.copy());
        }
        Collections.sort(out, new Comparator<Task>() {
            public int compare(Task a, Task b) {
                int pa = Task.indexOf(Task.PRIORITIES, a.getPriority());
                int pb = Task.indexOf(Task.PRIORITIES, b.getPriority());
                if (pa != pb) {
                    return pb - pa;
                }
                String da = a.getDueDate() == null ? "9999-99-99" : a.getDueDate();
                String db = b.getDueDate() == null ? "9999-99-99" : b.getDueDate();
                int c = da.compareTo(db);
                return c != 0 ? c : (a.getId() < b.getId() ? -1 : (a.getId() == b.getId() ? 0 : 1));
            }
        });
        return out;
    }

    /** Statistiques : total, par statut, haute priorité, en retard. */
    public synchronized Map<String, Integer> stats(String todayIso) {
        int todo = 0;
        int doing = 0;
        int done = 0;
        int high = 0;
        int overdue = 0;
        for (Task t : tasks.values()) {
            if ("todo".equals(t.getStatus())) {
                todo++;
            } else if ("doing".equals(t.getStatus())) {
                doing++;
            } else {
                done++;
            }
            if ("haute".equals(t.getPriority()) && !"done".equals(t.getStatus())) {
                high++;
            }
            if (isOverdue(t, todayIso)) {
                overdue++;
            }
        }
        Map<String, Integer> m = new LinkedHashMap<String, Integer>();
        m.put("total", Integer.valueOf(tasks.size()));
        m.put("todo", Integer.valueOf(todo));
        m.put("doing", Integer.valueOf(doing));
        m.put("done", Integer.valueOf(done));
        m.put("high", Integer.valueOf(high));
        m.put("overdue", Integer.valueOf(overdue));
        return m;
    }

    public static boolean isOverdue(Task t, String todayIso) {
        return t.getDueDate() != null && !"done".equals(t.getStatus()) && t.getDueDate().compareTo(todayIso) < 0;
    }

    /** Réinitialise avec des tâches de démonstration liées au parcours de formation. */
    public synchronized void seed(long now) {
        tasks.clear();
        nextId = 1;
        create("Installer le profil Dmgr01", "Créer le profil de gestion avec manageprofiles.sh et démarrer le DMGR.",
                "haute", "done", plusDays(-3), now);
        create("Fédérer nodesa1 dans la cellule", "addNode.sh vm-was-efi 8880 depuis le profil Custom01.",
                "haute", "done", plusDays(-2), now);
        create("Créer le serveur test2 sur nodesa2", "Console : Serveurs > Types de serveurs > Serveurs d'applications WebSphere.",
                "moyenne", "doing", plusDays(1), now);
        create("Déployer efi-tasks.ear", "Installer, mapper sur test, enregistrer, synchroniser puis démarrer.",
                "haute", "doing", plusDays(0), now);
        create("Configurer un cluster test + test2", "Créer le cluster et ajouter les deux membres.",
                "moyenne", "todo", plusDays(5), now);
        create("Lire SystemOut.log après démarrage", "Repérer les messages WSVR0221I et SRVE0250I.",
                "basse", "todo", plusDays(2), now);
        create("Importer le certificat Open-Meteo", "CellDefaultTrustStore > Certificats de signataire > Récupérer du port.",
                "basse", "todo", null, now);
    }

    static String plusDays(int days) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"), Locale.FRANCE);
        c.add(Calendar.DAY_OF_MONTH, days);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
        return f.format(c.getTime());
    }

    public static String today() {
        return plusDays(0);
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().length() == 0 ? null : s.trim();
    }
}
