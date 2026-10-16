package fr.efiacademy.lab.quiz;

import java.io.Serializable;

/** Question à choix multiples (une seule bonne réponse). */
public final class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String theme;
    private final String text;
    private final String[] choices;
    private final int answer;
    private final String explanation;

    public Question(int id, String theme, String text, String[] choices, int answer, String explanation) {
        if (choices == null || choices.length < 2 || answer < 0 || answer >= choices.length) {
            throw new IllegalArgumentException("Question " + id + " invalide");
        }
        this.id = id;
        this.theme = theme;
        this.text = text;
        this.choices = choices.clone();
        this.answer = answer;
        this.explanation = explanation;
    }

    public int getId() {
        return id;
    }

    public String getTheme() {
        return theme;
    }

    public String getText() {
        return text;
    }

    public String[] getChoices() {
        return choices.clone();
    }

    public int getChoiceCount() {
        return choices.length;
    }

    public String getChoice(int i) {
        return choices[i];
    }

    public int getAnswer() {
        return answer;
    }

    public String getExplanation() {
        return explanation;
    }
}
