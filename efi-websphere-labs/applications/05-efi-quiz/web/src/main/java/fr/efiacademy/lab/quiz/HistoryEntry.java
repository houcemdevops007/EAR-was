package fr.efiacademy.lab.quiz;

import java.io.Serializable;

/** Entrée de l'historique de session (une tentative terminée). */
public final class HistoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long date;
    private final String theme;
    private final int score;
    private final int total;
    private final long durationSeconds;

    public HistoryEntry(long date, String theme, int score, int total, long durationSeconds) {
        this.date = date;
        this.theme = theme;
        this.score = score;
        this.total = total;
        this.durationSeconds = durationSeconds;
    }

    public long getDate() {
        return date;
    }

    public String getTheme() {
        return theme;
    }

    public int getScore() {
        return score;
    }

    public int getTotal() {
        return total;
    }

    public int getPercent() {
        return total == 0 ? 0 : Math.round(score * 100f / total);
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }
}
