package fr.efiacademy.lab.quiz;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fr.efiacademy.lab.common.Csrf;
import fr.efiacademy.lab.common.RequestUtil;

/**
 * Contrôleur du quiz (modèle MVC + Post/Redirect/Get).
 * <pre>
 * GET  /quiz          question courante ou correction (JSP /WEB-INF/jsp/question.jsp)
 * GET  /quiz/result   score final et corrections   (JSP /WEB-INF/jsp/result.jsp)
 * POST /quiz          action = start | answer | next | restart
 * </pre>
 * L'état est conservé dans la session HTTP : attributs « quiz.current »,
 * « quiz.history » (liste des tentatives) et « quiz.flash » (message ponctuel).
 */
public class QuizServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String CURRENT = "quiz.current";
    public static final String HISTORY = "quiz.history";
    public static final String FLASH = "quiz.flash";
    static final int MAX_HISTORY = 20;

    private final transient Random random = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        QuizSession quiz = (QuizSession) session.getAttribute(CURRENT);
        String ctx = req.getContextPath();
        boolean resultPage = "/result".equals(req.getPathInfo());
        if (quiz == null) {
            resp.sendRedirect(ctx + "/");
            return;
        }
        if (resultPage) {
            if (!quiz.isFinished()) {
                resp.sendRedirect(ctx + "/quiz");
                return;
            }
            req.setAttribute("quiz", quiz);
            req.getRequestDispatcher("/WEB-INF/jsp/result.jsp").forward(req, resp);
            return;
        }
        if (quiz.isFinished()) {
            resp.sendRedirect(ctx + "/quiz/result");
            return;
        }
        req.setAttribute("quiz", quiz);
        req.setAttribute("flash", consumeFlash(session));
        req.getRequestDispatcher("/WEB-INF/jsp/question.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        String ctx = req.getContextPath();
        if (!Csrf.isValid(req)) {
            session.setAttribute(FLASH, "Votre session a expiré ou le formulaire est invalide. Veuillez recommencer.");
            resp.sendRedirect(ctx + "/");
            return;
        }
        String action = req.getParameter("action");
        QuizSession quiz = (QuizSession) session.getAttribute(CURRENT);
        long now = System.currentTimeMillis();

        if ("start".equals(action)) {
            String theme = req.getParameter("theme");
            int count = RequestUtil.parseInt(req.getParameter("count"), -1);
            if (!QuestionBank.isTheme(theme)) {
                session.setAttribute(FLASH, "Veuillez choisir un thème valide.");
                resp.sendRedirect(ctx + "/");
                return;
            }
            if (count != 0 && count != 10 && count != 20) {
                session.setAttribute(FLASH, "Nombre de questions invalide.");
                resp.sendRedirect(ctx + "/");
                return;
            }
            session.setAttribute(CURRENT, QuizSession.start(theme, count, random, now));
            resp.sendRedirect(ctx + "/quiz");
        } else if ("answer".equals(action)) {
            if (quiz == null || quiz.isFinished()) {
                resp.sendRedirect(ctx + "/");
                return;
            }
            if (quiz.isFeedback()) {
                resp.sendRedirect(ctx + "/quiz");
                return;
            }
            String raw = req.getParameter("choice");
            int choice = RequestUtil.parseInt(raw, -1);
            Question q = quiz.getCurrentQuestion();
            if (raw == null || choice < 0 || choice >= q.getChoiceCount()) {
                session.setAttribute(FLASH, "Veuillez sélectionner une réponse avant de valider.");
            } else {
                quiz.answer(choice);
                session.setAttribute(CURRENT, quiz); // signale la modification (réplication de session)
            }
            resp.sendRedirect(ctx + "/quiz");
        } else if ("next".equals(action)) {
            if (quiz == null) {
                resp.sendRedirect(ctx + "/");
                return;
            }
            if (quiz.isFeedback() && quiz.next(now)) {
                addHistory(session, new HistoryEntry(now, quiz.getTheme(), quiz.getScore(), quiz.getTotal(),
                        quiz.getDurationSeconds()));
                session.setAttribute(CURRENT, quiz);
                resp.sendRedirect(ctx + "/quiz/result");
                return;
            }
            session.setAttribute(CURRENT, quiz);
            resp.sendRedirect(ctx + "/quiz");
        } else if ("restart".equals(action)) {
            session.removeAttribute(CURRENT);
            resp.sendRedirect(ctx + "/");
        } else if ("clear-history".equals(action)) {
            session.removeAttribute(HISTORY);
            resp.sendRedirect(ctx + "/");
        } else {
            session.setAttribute(FLASH, "Action inconnue.");
            resp.sendRedirect(ctx + "/");
        }
    }

    @SuppressWarnings("unchecked")
    static void addHistory(HttpSession session, HistoryEntry e) {
        List<HistoryEntry> h = (List<HistoryEntry>) session.getAttribute(HISTORY);
        if (h == null) {
            h = new ArrayList<HistoryEntry>();
        }
        h.add(0, e);
        while (h.size() > MAX_HISTORY) {
            h.remove(h.size() - 1);
        }
        session.setAttribute(HISTORY, h);
    }

    /** Variante publique utilisée par les JSP (index.jsp). */
    public static String consumeFlashPublic(HttpSession session) {
        return session == null ? null : consumeFlash(session);
    }

    static String consumeFlash(HttpSession session) {
        String f = (String) session.getAttribute(FLASH);
        if (f != null) {
            session.removeAttribute(FLASH);
        }
        return f;
    }
}
