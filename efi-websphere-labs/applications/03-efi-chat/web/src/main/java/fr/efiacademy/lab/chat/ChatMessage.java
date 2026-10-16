package fr.efiacademy.lab.chat;

import java.io.Serializable;

/** Message du salon (immuable). type : "user" ou "system". */
public final class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String nickname;
    private final String text;
    private final long timestamp;
    private final String type;

    public ChatMessage(long id, String nickname, String text, long timestamp, String type) {
        this.id = id;
        this.nickname = nickname;
        this.text = text;
        this.timestamp = timestamp;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public boolean isSystem() {
        return "system".equals(type);
    }
}
