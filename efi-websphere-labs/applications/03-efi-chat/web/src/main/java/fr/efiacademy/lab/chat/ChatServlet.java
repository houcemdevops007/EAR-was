package fr.efiacademy.lab.chat;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fr.efiacademy.lab.common.JsonWriter;
import fr.efiacademy.lab.common.RequestUtil;
import fr.efiacademy.lab.common.ServerInfo;

/**
 * API REST du salon (mappée sur /api/chat/*) :
 * <pre>
 * GET  /api/chat/messages?since=N  messages récents + utilisateurs actifs
 * POST /api/chat/join              nickname
 * POST /api/chat/send              text
 * POST /api/chat/leave
 * POST /api/chat/admin             code   (active le mode administrateur de démonstration)
 * POST /api/chat/clear                    (mode administrateur requis)
 * </pre>
 * Le pseudo est conservé dans la session HTTP (attribut « chat.nick »).
 * Les textes sont renvoyés en JSON échappé et affichés via textContent côté
 * navigateur : aucune injection HTML possible.
 */
public class ChatServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    static final String NICK = "chat.nick";
    static final String ADMIN = "chat.admin";

    private ChatRoom room() {
        return (ChatRoom) getServletContext().getAttribute(ChatContextListener.ROOM_ATTRIBUTE);
    }

    private static String action(HttpServletRequest req) {
        String p = req.getPathInfo();
        return p == null ? "" : p;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!"/messages".equals(action(req))) {
            RequestUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Ressource inconnue.");
            return;
        }
        HttpSession session = req.getSession(false);
        String nick = session == null ? null : (String) session.getAttribute(NICK);
        long now = System.currentTimeMillis();
        ChatRoom room = room();
        if (nick != null) {
            room.touch(nick, session.getId(), now);
        }
        long since = RequestUtil.parseLong(req.getParameter("since"), 0);
        List<ChatMessage> list = room.since(since);
        JsonWriter w = new JsonWriter().beginObject()
                .field("me", nick)
                .field("admin", session != null && Boolean.TRUE.equals(session.getAttribute(ADMIN)))
                .field("lastId", room.lastId())
                .field("generation", String.valueOf(room.generation()))
                .field("hostname", ServerInfo.hostname())
                .field("serverTime", ServerInfo.isoNow());
        w.name("messages").beginArray();
        for (ChatMessage m : list) {
            w.beginObject()
                    .field("id", m.getId())
                    .field("nick", m.getNickname())
                    .field("text", m.getText())
                    .field("time", ServerInfo.iso(new java.util.Date(m.getTimestamp())))
                    .field("type", m.getType())
                    .field("mine", nick != null && !m.isSystem() && nick.equalsIgnoreCase(m.getNickname()))
                    .endObject();
        }
        w.endArray();
        w.name("users").beginArray();
        for (String u : room.activeUsers(now)) {
            w.value(u);
        }
        w.endArray().endObject();
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK, w.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!RequestUtil.checkCsrf(req, resp)) {
            return;
        }
        String action = action(req);
        HttpSession session = req.getSession(true);
        long now = System.currentTimeMillis();
        ChatRoom room = room();
        String nick = (String) session.getAttribute(NICK);

        if ("/join".equals(action)) {
            String wanted = RequestUtil.clean(req.getParameter("nickname"), 40);
            String err = ChatRoom.validateNickname(wanted);
            if (err == null) {
                if (nick != null && !nick.equalsIgnoreCase(wanted)) {
                    room.leave(nick, now);
                }
                err = room.join(wanted, session.getId(), now);
            }
            if (err != null) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, err);
                return;
            }
            session.setAttribute(NICK, wanted);
            ok(resp, "nickname", wanted);
        } else if ("/send".equals(action)) {
            if (nick == null) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Choisissez d'abord un pseudonyme.");
                return;
            }
            String raw = req.getParameter("text");
            String err = ChatRoom.validateMessage(raw);
            if (err != null) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, err);
                return;
            }
            if (!room.allowSend(session.getId(), now)) {
                RequestUtil.sendError(resp, 429, "Trop de messages : maximum " + ChatRoom.RATE_MAX_MESSAGES
                        + " messages toutes les " + ChatRoom.RATE_WINDOW_MS / 1000 + " secondes.");
                return;
            }
            room.touch(nick, session.getId(), now);
            ChatMessage m = room.post(nick, RequestUtil.clean(raw, ChatRoom.MAX_MESSAGE_LENGTH), now);
            ok(resp, "id", String.valueOf(m.getId()));
        } else if ("/leave".equals(action)) {
            room.leave(nick, now);
            session.removeAttribute(NICK);
            session.removeAttribute(ADMIN);
            ok(resp, "status", "left");
        } else if ("/admin".equals(action)) {
            if (!room.checkAdminCode(req.getParameter("code"))) {
                getServletContext().log("[EFI Chat] Code administrateur invalide depuis " + req.getRemoteAddr());
                RequestUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Code administrateur incorrect (consultez SystemOut.log du serveur).");
                return;
            }
            session.setAttribute(ADMIN, Boolean.TRUE);
            ok(resp, "admin", "true");
        } else if ("/clear".equals(action)) {
            if (!Boolean.TRUE.equals(session.getAttribute(ADMIN))) {
                RequestUtil.sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Mode administrateur requis.");
                return;
            }
            room.clear(nick == null ? "anonyme" : nick, now);
            getServletContext().log("[EFI Chat] Salon vidé par " + nick);
            ok(resp, "status", "cleared");
        } else {
            RequestUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Action inconnue.");
        }
    }

    private static void ok(HttpServletResponse resp, String key, String value) throws IOException {
        RequestUtil.sendJson(resp, HttpServletResponse.SC_OK,
                new JsonWriter().beginObject().field("ok", true).field(key, value).endObject().toString());
    }
}
