package fr.efiacademy.lab.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import fr.efiacademy.lab.common.JsonWriter;

public class TaskStoreTest {

    @Test
    public void crudLifecycle() {
        TaskStore s = new TaskStore();
        Task t = s.create("Déployer l'EAR", "via wsadmin", "haute", "todo", "2030-01-15", 1000);
        assertEquals(1, t.getId());
        assertEquals("todo", s.get(1).getStatus());

        Task u = s.update(1, "Déployer l'EAR v2", "", "basse", "doing", "", 2000);
        assertEquals("Déployer l'EAR v2", u.getTitle());
        assertNull(u.getDueDate());
        assertEquals(2000, u.getUpdatedAt());

        assertEquals("done", s.move(1, "done", 3000).getStatus());
        assertTrue(s.delete(1));
        assertFalse(s.delete(1));
        assertNull(s.get(1));
        assertNull(s.update(99, "xxx", "", "basse", "todo", null, 1));
    }

    @Test
    public void returnedTasksAreDefensiveCopies() {
        TaskStore s = new TaskStore();
        s.create("Tâche test", "", "moyenne", "todo", null, 1);
        s.get(1).setTitle("modifié hors magasin");
        assertEquals("Tâche test", s.get(1).getTitle());
    }

    @Test
    public void validationRejectsBadInput() {
        TaskStore.Validation v = TaskStore.validate("ab", null, "urgente", "archive", "2024-02-30");
        assertFalse(v.isValid());
        assertNotNull(v.errors.get("title"));
        assertNotNull(v.errors.get("priority"));
        assertNotNull(v.errors.get("status"));
        assertNotNull(v.errors.get("dueDate"));
        assertTrue(TaskStore.validate("Titre ok", "", "basse", "done", "2024-02-29").isValid());
    }

    @Test
    public void listIsSortedByPriorityThenDueDate() {
        TaskStore s = new TaskStore();
        s.create("basse A", "", "basse", "todo", null, 1);
        s.create("haute tard", "", "haute", "todo", "2030-12-01", 1);
        s.create("haute tôt", "", "haute", "todo", "2030-01-01", 1);
        List<Task> l = s.list();
        assertEquals("haute tôt", l.get(0).getTitle());
        assertEquals("haute tard", l.get(1).getTitle());
        assertEquals("basse A", l.get(2).getTitle());
    }

    @Test
    public void statsCountOverdueAndHighPriority() {
        TaskStore s = new TaskStore();
        s.create("En retard", "", "haute", "todo", "2020-01-01", 1);
        s.create("Terminée en retard", "", "haute", "done", "2020-01-01", 1);
        s.create("Future", "", "basse", "doing", "2099-01-01", 1);
        Map<String, Integer> st = s.stats("2025-06-01");
        assertEquals(3, st.get("total").intValue());
        assertEquals(1, st.get("todo").intValue());
        assertEquals(1, st.get("doing").intValue());
        assertEquals(1, st.get("done").intValue());
        assertEquals(1, st.get("high").intValue());
        assertEquals(1, st.get("overdue").intValue());
    }

    @Test
    public void seedProvidesDemoData() {
        TaskStore s = new TaskStore();
        s.seed(1);
        assertEquals(7, s.list().size());
        assertTrue(TaskStore.today().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void jsonSerializationEscapesUserInput() {
        TaskStore s = new TaskStore();
        Task t = s.create("<img src=x onerror=alert(1)>", "", "basse", "todo", null, 0);
        JsonWriter w = new JsonWriter();
        TaskServlet.write(w, t, "2025-01-01");
        assertFalse(w.toString().contains("<img"));
        assertTrue(w.toString().contains("\\u003cimg"));
    }
}
