package fr.efiacademy.lab.tasks;

import java.io.Serializable;

/** Tâche Kanban. Priorités : basse, moyenne, haute. Statuts : todo, doing, done. */
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String[] PRIORITIES = {"basse", "moyenne", "haute"};
    public static final String[] STATUSES = {"todo", "doing", "done"};

    private long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    /** Date d'échéance au format AAAA-MM-JJ, ou null. */
    private String dueDate;
    private long createdAt;
    private long updatedAt;

    public Task(long id, String title, String description, String priority, String status, String dueDate, long now) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Copie défensive (les objets renvoyés par le magasin ne sont jamais partagés). */
    public Task copy() {
        Task t = new Task(id, title, description, priority, status, dueDate, createdAt);
        t.updatedAt = updatedAt;
        return t;
    }

    public static boolean isValidPriority(String p) {
        return indexOf(PRIORITIES, p) >= 0;
    }

    public static boolean isValidStatus(String s) {
        return indexOf(STATUSES, s) >= 0;
    }

    static int indexOf(String[] values, String v) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(v)) {
                return i;
            }
        }
        return -1;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void touch(long now) {
        this.updatedAt = now;
    }
}
