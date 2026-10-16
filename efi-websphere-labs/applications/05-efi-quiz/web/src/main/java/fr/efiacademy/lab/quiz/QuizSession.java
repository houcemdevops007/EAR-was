package fr.efiacademy.lab.quiz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * État d'un quiz en cours, stocké dans la session HTTP (Serializable pour
 * permettre la persistance / réplication de session WebSphere).
 */
public class QuizSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String theme;
    private final int[] order;
    private final int[] answers;
    private int current;
    private boolean feedback;
    private final long startedAt;
    private long finishedAt;

    /** Crée un quiz de {@code count} questions (0 = toutes) pour un thème ou « all ». */
    public static QuizSession start(String theme, int count, Random random, long now) {
        List<Question> pool = new ArrayList<Question>(QuestionBank.byTheme(theme));
        Collections.shuffle(pool, random);
        int n = count <= 0 || count > pool.size() ? pool.size() : count;
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = pool.get(i).getId();
        }
        return new QuizSession(theme, ids, now);
    }

    QuizSession(String theme, int[] order, long now) {
        this.theme = theme;
        this.order = order.clone();
        this.answers = new int[order.length];
        for (int i = 0; i < answers.length; i++) {
            answers[i] = -1;
        }
        this.startedAt = now;
    }

    public String getTheme() {
        return theme;
    }

    public int getTotal() {
        return order.length;
    }

    public int getCurrentIndex() {
        return current;
    }

    public boolean isFeedback() {
        return feedback;
    }

    public boolean isFinished() {
        return current >= order.length;
    }

    public Question getCurrentQuestion() {
        return isFinished() ? null : QuestionBank.get(order[current]);
    }

    public int getCurrentAnswer() {
        return isFinished() ? -1 : answers[current];
    }

    /**
     * Enregistre la réponse à la question courante et passe en mode correction.
     * @return true si la réponse est correcte
     */
    public boolean answer(int choice) {
        Question q = getCurrentQuestion();
        if (q == null || feedback) {
            throw new IllegalStateException("Aucune question en attente de réponse.");
        }
        if (choice < 0 || choice >= q.getChoiceCount()) {
            throw new IllegalArgumentException("Réponse hors limites.");
        }
        answers[current] = choice;
        feedback = true;
        return choice == q.getAnswer();
    }

    /** Passe à la question suivante ; retourne true si le quiz vient de se terminer. */
    public boolean next(long now) {
        if (!feedback) {
            throw new IllegalStateException("Répondez d'abord à la question.");
        }
        feedback = false;
        current++;
        if (isFinished()) {
            finishedAt = now;
            return true;
        }
        return false;
    }

    public int getScore() {
        int s = 0;
        for (int i = 0; i < order.length; i++) {
            if (answers[i] >= 0 && answers[i] == QuestionBank.get(order[i]).getAnswer()) {
                s++;
            }
        }
        return s;
    }

    public int getPercent() {
        return order.length == 0 ? 0 : Math.round(getScore() * 100f / order.length);
    }

    public long getDurationSeconds() {
        long end = finishedAt > 0 ? finishedAt : System.currentTimeMillis();
        return Math.max(0, (end - startedAt) / 1000);
    }

    public long getStartedAt() {
        return startedAt;
    }

    public Question getQuestionAt(int i) {
        return QuestionBank.get(order[i]);
    }

    public int getAnswerAt(int i) {
        return answers[i];
    }

    /** Score par thème : thème → {bonnes réponses, total}. */
    public Map<String, int[]> getScoreByTheme() {
        Map<String, int[]> m = new LinkedHashMap<String, int[]>();
        for (int i = 0; i < order.length; i++) {
            Question q = QuestionBank.get(order[i]);
            int[] v = m.get(q.getTheme());
            if (v == null) {
                v = new int[2];
                m.put(q.getTheme(), v);
            }
            if (answers[i] == q.getAnswer()) {
                v[0]++;
            }
            v[1]++;
        }
        return m;
    }

    /** Appréciation pédagogique selon le pourcentage. */
    public static String verdict(int percent) {
        if (percent >= 90) {
            return "Excellent ! Vous maîtrisez WebSphere ND.";
        }
        if (percent >= 70) {
            return "Très bien : encore quelques notions à consolider.";
        }
        if (percent >= 50) {
            return "Correct : relisez les explications et recommencez.";
        }
        return "À retravailler : reprenez les TP et le guide formateur.";
    }
}
