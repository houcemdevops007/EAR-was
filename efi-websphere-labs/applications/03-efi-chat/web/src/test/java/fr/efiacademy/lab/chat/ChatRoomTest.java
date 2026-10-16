package fr.efiacademy.lab.chat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ChatRoomTest {

    @Test
    public void historyIsCappedTo100Messages() {
        ChatRoom room = new ChatRoom("CODE1234");
        for (int i = 0; i < 150; i++) {
            room.post("alice", "message " + i, 1000L + i);
        }
        assertEquals(ChatRoom.MAX_HISTORY, room.size());
        List<ChatMessage> all = room.since(0);
        assertEquals("message 50", all.get(0).getText());
        assertEquals("message 149", all.get(99).getText());
    }

    @Test
    public void sinceReturnsOnlyNewMessages() {
        ChatRoom room = new ChatRoom("CODE1234");
        ChatMessage a = room.post("alice", "a", 1);
        room.post("bob", "b", 2);
        List<ChatMessage> after = room.since(a.getId());
        assertEquals(1, after.size());
        assertEquals("b", after.get(0).getText());
    }

    @Test
    public void nicknameValidation() {
        assertNull(ChatRoom.validateNickname("Houcem_01"));
        assertNull(ChatRoom.validateNickname("Élodie M."));
        assertNotNull(ChatRoom.validateNickname("a"));
        assertNotNull(ChatRoom.validateNickname("<script>"));
        assertNotNull(ChatRoom.validateNickname("admin"));
        assertNotNull(ChatRoom.validateNickname("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    public void messageValidation() {
        assertNull(ChatRoom.validateMessage("Bonjour"));
        assertNotNull(ChatRoom.validateMessage("   "));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= ChatRoom.MAX_MESSAGE_LENGTH; i++) {
            sb.append('x');
        }
        assertNotNull(ChatRoom.validateMessage(sb.toString()));
    }

    @Test
    public void nicknameCannotBeStolenByAnotherSession() {
        ChatRoom room = new ChatRoom("CODE1234");
        assertNull(room.join("Alice", "S1", 1000));
        assertNotNull(room.join("alice", "S2", 1001));
        assertNull(room.join("Alice", "S1", 1002));
        assertEquals(1, room.activeUsers(1003).size());
    }

    @Test
    public void inactiveUsersArePurged() {
        ChatRoom room = new ChatRoom("CODE1234");
        room.join("Alice", "S1", 0);
        room.join("Bob", "S2", 20000);
        List<String> users = room.activeUsers(ChatRoom.PRESENCE_TIMEOUT_MS + 1000);
        assertEquals(1, users.size());
        assertEquals("Bob", users.get(0));
    }

    @Test
    public void rateLimitBlocksFlood() {
        ChatRoom room = new ChatRoom("CODE1234");
        for (int i = 0; i < ChatRoom.RATE_MAX_MESSAGES; i++) {
            assertTrue(room.allowSend("S1", 1000 + i));
        }
        assertFalse(room.allowSend("S1", 1010));
        assertTrue(room.allowSend("S2", 1010));
        assertTrue(room.allowSend("S1", 1000 + ChatRoom.RATE_WINDOW_MS + 100));
    }

    @Test
    public void adminCodeAndClear() {
        ChatRoom room = new ChatRoom("CODE1234");
        assertTrue(room.checkAdminCode(" CODE1234 "));
        assertFalse(room.checkAdminCode("code1234"));
        assertFalse(room.checkAdminCode(null));
        room.post("alice", "x", 1);
        room.clear("formateur", 2);
        assertEquals(1, room.size());
        assertTrue(room.since(0).get(0).isSystem());
    }

    @Test
    public void generatedCodeHasExpectedFormat() {
        String c = ChatContextListener.randomCode(8);
        assertEquals(8, c.length());
        assertTrue(c.matches("[A-Z2-9]{8}"));
    }
}
