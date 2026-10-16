package fr.efiacademy.lab.quiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public class QuizTest {

    @Test
    public void bankHasAtLeast30QuestionsCoveringAllThemes() {
        assertTrue("au moins 30 questions", QuestionBank.all().size() >= 30);
        for (String theme : QuestionBank.themes().keySet()) {
            assertTrue("thème " + theme, QuestionBank.byTheme(theme).size() >= 5);
        }
    }

    @Test
    public void everyQuestionIsWellFormed() {
        Set<String> texts = new HashSet<String>();
        for (Question q : QuestionBank.all()) {
            assertTrue(texts.add(q.getText()));
            assertEquals(4, q.getChoiceCount());
            assertTrue(q.getAnswer() >= 0 && q.getAnswer() < 4);
            assertTrue(q.getExplanation().length() > 20);
            assertNotNull(QuestionBank.get(q.getId()));
        }
    }

    @Test
    public void fullQuizFlowComputesScore() {
        QuizSession s = QuizSession.start(QuestionBank.DMGR, 0, new Random(42), 0);
        assertEquals(QuestionBank.byTheme(QuestionBank.DMGR).size(), s.getTotal());
        int expected = 0;
        for (int i = 0; i < s.getTotal(); i++) {
            Question q = s.getCurrentQuestion();
            boolean answerRight = i % 2 == 0;
            int choice = answerRight ? q.getAnswer() : (q.getAnswer() + 1) % 4;
            assertEquals(answerRight, s.answer(choice));
            if (answerRight) {
                expected++;
            }
            assertTrue(s.isFeedback());
            boolean finished = s.next(1000L * (i + 1));
            assertEquals(i == s.getTotal() - 1, finished);
        }
        assertTrue(s.isFinished());
        assertEquals(expected, s.getScore());
        assertEquals(Math.round(expected * 100f / s.getTotal()), s.getPercent());
        Map<String, int[]> byTheme = s.getScoreByTheme();
        assertEquals(1, byTheme.size());
        assertEquals(expected, byTheme.get(QuestionBank.DMGR)[0]);
    }

    @Test
    public void countLimitsNumberOfQuestionsWithoutDuplicates() {
        QuizSession s = QuizSession.start("all", 10, new Random(1), 0);
        assertEquals(10, s.getTotal());
        Set<Integer> ids = new HashSet<Integer>();
        for (int i = 0; i < 10; i++) {
            assertTrue(ids.add(Integer.valueOf(s.getQuestionAt(i).getId())));
        }
    }

    @Test
    public void cannotAnswerTwiceOrSkip() {
        QuizSession s = QuizSession.start("all", 10, new Random(3), 0);
        try {
            s.next(1);
            fail("next sans réponse");
        } catch (IllegalStateException expected) {
            // attendu
        }
        try {
            s.answer(7);
            fail("réponse hors limites");
        } catch (IllegalArgumentException expected) {
            // attendu
        }
        s.answer(0);
        try {
            s.answer(1);
            fail("double réponse");
        } catch (IllegalStateException expected) {
            // attendu
        }
        assertFalse(s.isFinished());
    }

    @Test
    public void themeValidationAndVerdicts() {
        assertTrue(QuestionBank.isTheme("all"));
        assertTrue(QuestionBank.isTheme(QuestionBank.SSL));
        assertFalse(QuestionBank.isTheme("<script>"));
        assertTrue(QuizSession.verdict(95).startsWith("Excellent"));
        assertTrue(QuizSession.verdict(10).startsWith("À retravailler"));
    }
}
