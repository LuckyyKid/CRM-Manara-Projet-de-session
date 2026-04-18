package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.*;
import CRM_Manara.CRM_Manara.Repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

// Service principal du module tutorat adaptatif
@Service
public class TutoringService {

    @Autowired
    private TutoringSessionRepository sessionRepo;

    @Autowired
    private TutoringAxisRepository axisRepo;

    @Autowired
    private TutoringQuestionRepository questionRepo;

    @Autowired
    private TutoringAttemptRepository attemptRepo;

    @Autowired
    private TutoringAxisScoreRepository scoreRepo;

    @Autowired
    private TutoringHomeworkRepository homeworkRepo;

    @Autowired
    private TutoringSpacedReviewRepository reviewRepo;

    @Autowired
    private TutoringGroupAlertRepository alertRepo;

    @Autowired
    private AnimationRepo animationRepo;

    @Autowired
    private AnimateurRepo animateurRepo;

    @Autowired
    private EnfantRepo enfantRepo;

    @Autowired
    private InscriptionRepo inscriptionRepo;

    @Autowired
    private AnthropicService anthropicService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------
    // 1. Créer une session de tutorat avec génération IA des axes
    // ---------------------------------------------------------------
    @Transactional
    public TutoringSession createSession(Long animationId, Long tutorId, String contentText) throws Exception {
        Animation animation = animationRepo.findById(animationId)
            .orElseThrow(() -> new RuntimeException("Animation introuvable"));
        Animateur tutor = animateurRepo.findById(tutorId)
            .orElseThrow(() -> new RuntimeException("Animateur introuvable"));

        // Sauvegarde la session
        TutoringSession session = new TutoringSession(animation, tutor, contentText);
        session = sessionRepo.save(session);

        // Prompt système pour Claude
        String systemPrompt = "Tu es un expert pedagogique. Analyse le contenu et retourne UNIQUEMENT un objet JSON valide, sans markdown. " +
            "Genere 3 a 5 axes pedagogiques. Pour chaque axe, genere exactement 5 questions, une par angle : reconnaissance, application, piege, transfert, justification. " +
            "Garde les textes courts pour eviter un JSON trop long. Les types autorises sont mcq et dev. " +
            "Pour mcq, mets options comme tableau JSON de 4 choix. Pour dev, mets options comme tableau vide. " +
            "Format exact : { \"axes\": [{ \"name\": \"\", \"description\": \"\", \"questions\": [{ \"type\": \"mcq|dev\", \"angle\": \"reconnaissance|application|piege|transfert|justification\", \"difficulty\": 1, \"content\": \"\", \"options\": [], \"correctAnswer\": \"\", \"explanation\": \"\", \"targetedError\": \"\" }] }] }";

        String jsonResponse = stripMarkdownJson(anthropicService.callClaude(systemPrompt, contentText));

        int generatedQuestionCount = 0;

        // Parse le JSON retourné par l'IA
        try {
            Map<String, Object> result = objectMapper.readValue(jsonResponse, Map.class);
            List<Map<String, Object>> axes = (List<Map<String, Object>>) result.get("axes");

            if (axes == null || axes.isEmpty()) {
                throw new IllegalStateException("L'IA n'a retourné aucun axe pédagogique.");
            }

            for (Map<String, Object> axisData : axes) {
                TutoringAxis axis = new TutoringAxis(
                    session,
                    (String) axisData.get("name"),
                    (String) axisData.get("description")
                );
                axis = axisRepo.save(axis);

                List<Map<String, Object>> questions = (List<Map<String, Object>>) axisData.get("questions");
                if (questions != null) {
                    for (Map<String, Object> qData : questions) {
                        TutoringQuestion question = new TutoringQuestion(
                            axis,
                            (String) qData.getOrDefault("type", "mcq"),
                            (String) qData.getOrDefault("angle", ""),
                            readDifficulty(qData.get("difficulty")),
                            (String) qData.getOrDefault("content", ""),
                            readOptionsJson(qData.containsKey("optionsJson") ? qData.get("optionsJson") : qData.get("options")),
                            (String) qData.getOrDefault("correctAnswer", ""),
                            (String) qData.getOrDefault("explanation", ""),
                            (String) qData.getOrDefault("targetedError", "")
                        );
                        questionRepo.save(question);
                        generatedQuestionCount++;
                    }
                }
            }

            if (generatedQuestionCount == 0) {
                throw new IllegalStateException("L'IA n'a retourné aucune question pour cette séance.");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                "Impossible de générer un quiz valide. La réponse IA était invalide ou tronquée. Détail : " + e.getMessage(),
                e
            );
        }

        return session;
    }

    @Transactional
    public void deleteSession(Long sessionId, String animateurEmail) {
        Animateur animateur = animateurRepo.findByUserEmail(animateurEmail)
            .orElseThrow(() -> new RuntimeException("Animateur introuvable"));
        TutoringSession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Quiz introuvable"));

        if (session.getAnimation() == null
            || session.getAnimation().getAnimateur() == null
            || !animateur.getId().equals(session.getAnimation().getAnimateur().getId())) {
            throw new RuntimeException("Vous ne pouvez supprimer que les quiz de vos animations.");
        }

        attemptRepo.deleteByQuestionAxisSessionId(sessionId);
        scoreRepo.deleteByAxisSessionId(sessionId);
        reviewRepo.deleteByAxisSessionId(sessionId);
        homeworkRepo.deleteByAxisSessionId(sessionId);
        alertRepo.deleteByAxisSessionId(sessionId);
        questionRepo.deleteByAxisSessionId(sessionId);
        axisRepo.deleteBySessionId(sessionId);
        sessionRepo.delete(session);
    }

    private int readDifficulty(Object rawDifficulty) {
        if (rawDifficulty instanceof Number number) {
            return number.intValue();
        }
        if (rawDifficulty != null) {
            try {
                return Integer.parseInt(rawDifficulty.toString());
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private String readOptionsJson(Object rawOptions) throws Exception {
        if (rawOptions == null) {
            return "[]";
        }
        if (rawOptions instanceof String options) {
            return options.isBlank() ? "[]" : options;
        }
        return objectMapper.writeValueAsString(rawOptions);
    }

    // ---------------------------------------------------------------
    // 2. Soumettre les réponses d'un étudiant au quiz
    // ---------------------------------------------------------------
    public void submitQuizAnswers(Long studentId, Long sessionId, List<Map<String, Object>> answers) throws Exception {
        Enfant student = enfantRepo.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Étudiant introuvable"));

        for (Map<String, Object> answerData : answers) {
            Long questionId = Long.valueOf(answerData.get("questionId").toString());
            TutoringQuestion question = questionRepo.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));

            String answer = (String) answerData.getOrDefault("answer", "");
            boolean correct = (boolean) answerData.getOrDefault("correct", false);
            int responseTimeMs = (int) answerData.getOrDefault("responseTimeMs", 0);

            // Pour les QCM, on compare automatiquement
            if ("mcq".equals(question.getType())) {
                correct = answer.equals(question.getCorrectAnswer());
            }

            TutoringAttempt attempt = new TutoringAttempt(student, question, answer, correct, responseTimeMs);
            attemptRepo.save(attempt);
        }

        // Mise à jour des scores par axe
        updateAxisScores(studentId, sessionId);

        // Vérification des alertes de groupe
        TutoringSession session = sessionRepo.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session introuvable"));
        List<TutoringAxis> axes = axisRepo.findBySessionId(sessionId);
        for (TutoringAxis axis : axes) {
            checkGroupAlerts(session.getAnimation().getId(), axis.getId());
        }
    }

    // ---------------------------------------------------------------
    // 3. Mettre à jour les scores par axe pour un étudiant
    // ---------------------------------------------------------------
    public void updateAxisScores(Long studentId, Long sessionId) {
        List<TutoringAxis> axes = axisRepo.findBySessionId(sessionId);

        for (TutoringAxis axis : axes) {
            List<TutoringAttempt> attempts = attemptRepo.findByStudentIdAndQuestionAxisId(studentId, axis.getId());

            if (attempts.isEmpty()) continue;

            // On prend les 3 dernières tentatives
            List<TutoringAttempt> recent = attempts.subList(Math.max(0, attempts.size() - 3), attempts.size());
            long correct = recent.stream().filter(TutoringAttempt::isCorrect).count();
            double score = (double) correct / recent.size();

            // Détermine le statut de maîtrise
            String mastery;
            if (score >= 0.66) {
                mastery = "mastered";
            } else if (score >= 0.33) {
                mastery = "learning";
            } else {
                mastery = "weak";
            }

            // Sauvegarde ou met à jour le score
            Optional<TutoringAxisScore> existingScore = scoreRepo.findByStudentIdAndAxisId(studentId, axis.getId());
            TutoringAxisScore axisScore;
            if (existingScore.isPresent()) {
                axisScore = existingScore.get();
                axisScore.setScore(score);
                axisScore.setMasteryStatus(mastery);
                axisScore.setUpdatedAt(LocalDateTime.now());
            } else {
                axisScore = new TutoringAxisScore(
                    enfantRepo.findById(studentId).orElseThrow(),
                    axis, score, mastery
                );
            }
            scoreRepo.save(axisScore);

            // Si l'axe est maîtrisé, créer une révision espacée
            if ("mastered".equals(mastery)) {
                Optional<TutoringSpacedReview> existingReview = reviewRepo.findByStudentIdAndAxisId(studentId, axis.getId());
                if (existingReview.isEmpty()) {
                    TutoringSpacedReview review = new TutoringSpacedReview(
                        enfantRepo.findById(studentId).orElseThrow(),
                        axis,
                        LocalDate.now().plusDays(2)
                    );
                    reviewRepo.save(review);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // 4. Générer un devoir personnalisé pour un étudiant sur un axe
    // ---------------------------------------------------------------
    public TutoringHomework generateHomework(Long studentId, Long axisId) throws Exception {
        Enfant student = enfantRepo.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Étudiant introuvable"));
        TutoringAxis axis = axisRepo.findById(axisId)
            .orElseThrow(() -> new RuntimeException("Axe introuvable"));

        // Cherche l'erreur dominante de l'étudiant sur cet axe
        List<TutoringAttempt> wrongAttempts = attemptRepo.findByStudentIdAndQuestionAxisId(studentId, axisId)
            .stream().filter(a -> !a.isCorrect()).toList();

        String dominantError = wrongAttempts.isEmpty() ? "erreurs générales"
            : wrongAttempts.get(wrongAttempts.size() - 1).getQuestion().getTargetedError();

        String prompt = String.format(
            "Génère 5 exercices progressifs (facile → difficile) pour l'axe '%s'. " +
            "L'étudiant fait cette erreur : '%s'. Chaque exercice cible cette erreur. " +
            "Retourne UNIQUEMENT du JSON : { \"exercises\": [{ \"difficulty\": 1, \"content\": \"\", \"solution\": \"\", \"explanation\": \"\" }] }",
            axis.getName(), dominantError
        );

        String jsonResponse = stripMarkdownJson(anthropicService.callClaude("Tu es un tuteur pédagogique expert.", prompt));

        TutoringHomework homework = new TutoringHomework(student, axis, jsonResponse);
        return homeworkRepo.save(homework);
    }

    // ---------------------------------------------------------------
    // 5. Récupérer les questions de révision espacée dues aujourd'hui
    // ---------------------------------------------------------------
    public List<TutoringQuestion> getSpacedReviewQuestions(Long studentId) {
        List<TutoringSpacedReview> dueReviews = reviewRepo.findByStudentIdAndNextReviewDateLessThanEqual(
            studentId, LocalDate.now()
        );

        List<TutoringQuestion> reviewQuestions = new ArrayList<>();
        for (TutoringSpacedReview review : dueReviews) {
            List<TutoringQuestion> axisQuestions = questionRepo.findByAxisId(review.getAxis().getId());
            if (!axisQuestions.isEmpty()) {
                // Prend une question aléatoire de l'axe
                int randomIndex = (int) (Math.random() * axisQuestions.size());
                reviewQuestions.add(axisQuestions.get(randomIndex));
            }
        }
        return reviewQuestions;
    }

    // ---------------------------------------------------------------
    // 6. Mettre à jour une révision espacée (succès ou échec)
    // ---------------------------------------------------------------
    public void updateSpacedReview(Long studentId, Long axisId, boolean success) {
        Optional<TutoringSpacedReview> optReview = reviewRepo.findByStudentIdAndAxisId(studentId, axisId);
        if (optReview.isEmpty()) return;

        TutoringSpacedReview review = optReview.get();

        if (success) {
            review.setConsecutiveSuccesses(review.getConsecutiveSuccesses() + 1);
            // Progression : 2 → 5 → 14 → 30 jours
            int nextInterval = nextInterval(review.getIntervalDays());
            review.setIntervalDays(nextInterval);
        } else {
            review.setConsecutiveSuccesses(0);
            review.setIntervalDays(2);
        }

        review.setNextReviewDate(LocalDate.now().plusDays(review.getIntervalDays()));
        reviewRepo.save(review);
    }

    // Calcule le prochain intervalle de révision
    private int nextInterval(int current) {
        if (current < 5) return 5;
        if (current < 14) return 14;
        return 30;
    }

    // ---------------------------------------------------------------
    // 7. Vérifier les alertes de groupe sur un axe
    // ---------------------------------------------------------------
    public void checkGroupAlerts(Long animationId, Long axisId) {
        List<TutoringAxisScore> scores = scoreRepo.findByAxisId(axisId);
        if (scores.isEmpty()) return;

        long failCount = scores.stream()
            .filter(s -> s.getScore() < 0.66)
            .count();

        double failureRate = (double) failCount / scores.size();

        // Si 60%+ des étudiants échouent, créer une alerte
        if (failureRate >= 0.60) {
            Animation animation = animationRepo.findById(animationId).orElse(null);
            TutoringAxis axis = axisRepo.findById(axisId).orElse(null);
            if (animation == null || axis == null) return;

            // Cherche l'erreur dominante dans le groupe
            String dominantError = scores.stream()
                .filter(s -> s.getScore() < 0.66)
                .findFirst()
                .map(s -> {
                    List<TutoringAttempt> attempts = attemptRepo.findByStudentIdAndQuestionAxisId(
                        s.getStudent().getId(), axisId
                    );
                    return attempts.stream()
                        .filter(a -> !a.isCorrect())
                        .map(a -> a.getQuestion().getTargetedError())
                        .filter(e -> e != null && !e.isEmpty())
                        .findFirst()
                        .orElse("erreur non identifiée");
                })
                .orElse("erreur non identifiée");

            TutoringGroupAlert alert = new TutoringGroupAlert(
                animation, axis, failureRate, (int) failCount, scores.size(), dominantError
            );
            alertRepo.save(alert);
        }
    }

    // ---------------------------------------------------------------
    // 8. Récupérer la progression de tous les étudiants d'une animation
    // ---------------------------------------------------------------
    public Map<String, Object> getStudentProgress(Long animationId) {
        List<TutoringSession> sessions = sessionRepo.findByAnimationId(animationId);
        List<Map<String, Object>> studentsProgress = new ArrayList<>();
        List<Map<String, Object>> sessionsProgress = new ArrayList<>();
        List<Map<String, Object>> homeworkProgress = new ArrayList<>();

        // Récupère toutes les inscriptions de cette animation
        Animation animation = animationRepo.findById(animationId).orElse(null);
        if (animation == null) return Map.of("students", studentsProgress);

        List<Inscription> inscriptions = inscriptionRepo.findByAnimationId(animationId);
        Set<Long> sessionIds = sessions.stream().map(TutoringSession::getId).collect(java.util.stream.Collectors.toSet());
        int totalQuestions = sessions.stream()
            .mapToInt(session -> questionRepo.findByAxisSessionId(session.getId()).size())
            .sum();
        int totalQuizCompletions = 0;
        int totalHomeworkAssigned = 0;
        int totalHomeworkCompleted = 0;
        double totalQuizScore = 0;
        int scoredQuizCount = 0;
        long totalResponseTimeMs = 0;
        int timedAttemptCount = 0;

        for (Inscription inscription : inscriptions) {
            Enfant student = inscription.getEnfant();
            List<TutoringAxisScore> scores = scoreRepo.findByStudentId(student.getId()).stream()
                .filter(score -> score.getAxis() != null
                    && score.getAxis().getSession() != null
                    && score.getAxis().getSession().getAnimation() != null
                    && animationId.equals(score.getAxis().getSession().getAnimation().getId()))
                .toList();
            List<TutoringAttempt> studentAttempts = attemptRepo.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(attempt -> attempt.getQuestion() != null
                    && attempt.getQuestion().getAxis() != null
                    && attempt.getQuestion().getAxis().getSession() != null
                    && sessionIds.contains(attempt.getQuestion().getAxis().getSession().getId()))
                .toList();
            List<TutoringHomework> studentHomeworks = homeworkRepo.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(homework -> homework.getAxis() != null
                    && homework.getAxis().getSession() != null
                    && homework.getAxis().getSession().getAnimation() != null
                    && animationId.equals(homework.getAxis().getSession().getAnimation().getId()))
                .toList();

            int quizCompletedCount = 0;
            double studentQuizScoreSum = 0;
            int studentScoredQuizCount = 0;
            long studentResponseTimeSum = 0;
            int studentTimedAttemptCount = 0;

            for (TutoringSession session : sessions) {
                List<TutoringAttempt> attemptsForSession = studentAttempts.stream()
                    .filter(attempt -> session.getId().equals(attempt.getQuestion().getAxis().getSession().getId()))
                    .toList();
                if (attemptsForSession.isEmpty()) {
                    continue;
                }
                quizCompletedCount++;
                long correct = attemptsForSession.stream().filter(TutoringAttempt::isCorrect).count();
                double score = (double) correct / attemptsForSession.size();
                studentQuizScoreSum += score;
                studentScoredQuizCount++;
            }

            for (TutoringAttempt attempt : studentAttempts) {
                if (attempt.getResponseTimeMs() > 0) {
                    studentResponseTimeSum += attempt.getResponseTimeMs();
                    studentTimedAttemptCount++;
                }
            }

            int homeworkAssignedCount = studentHomeworks.size();
            int homeworkCompletedCount = (int) studentHomeworks.stream()
                .filter(homework -> "completed".equalsIgnoreCase(homework.getStatus()))
                .count();

            Map<String, Object> studentData = new HashMap<>();
            studentData.put("studentId", student.getId());
            studentData.put("nom", student.getNom());
            studentData.put("prenom", student.getPrenom());
            studentData.put("quizCompletedCount", quizCompletedCount);
            studentData.put("quizAssignedCount", sessions.size());
            studentData.put("homeworkAssignedCount", homeworkAssignedCount);
            studentData.put("homeworkCompletedCount", homeworkCompletedCount);
            studentData.put("averageQuizScore", studentScoredQuizCount == 0 ? null : studentQuizScoreSum / studentScoredQuizCount);
            studentData.put("averageResponseTimeMs", studentTimedAttemptCount == 0 ? null : studentResponseTimeSum / studentTimedAttemptCount);
            studentData.put("scores", scores.stream().map(s -> {
                Map<String, Object> scoreMap = new HashMap<>();
                scoreMap.put("axisId", s.getAxis().getId());
                scoreMap.put("axisName", s.getAxis().getName());
                scoreMap.put("score", s.getScore());
                scoreMap.put("masteryStatus", s.getMasteryStatus());
                return scoreMap;
            }).toList());

            studentsProgress.add(studentData);

            for (TutoringHomework homework : studentHomeworks) {
                Map<String, Object> homeworkData = new LinkedHashMap<>();
                homeworkData.put("id", homework.getId());
                homeworkData.put("studentId", student.getId());
                homeworkData.put("studentName", student.getPrenom() + " " + student.getNom());
                homeworkData.put("sessionId", homework.getAxis().getSession().getId());
                homeworkData.put("axisId", homework.getAxis().getId());
                homeworkData.put("axisName", homework.getAxis().getName());
                homeworkData.put("status", homework.getStatus());
                homeworkData.put("createdAt", homework.getCreatedAt());
                homeworkData.put("completedAt", homework.getCompletedAt());
                homeworkProgress.add(homeworkData);
            }

            totalQuizCompletions += quizCompletedCount;
            totalHomeworkAssigned += homeworkAssignedCount;
            totalHomeworkCompleted += homeworkCompletedCount;
            totalQuizScore += studentQuizScoreSum;
            scoredQuizCount += studentScoredQuizCount;
            totalResponseTimeMs += studentResponseTimeSum;
            timedAttemptCount += studentTimedAttemptCount;
        }

        for (TutoringSession session : sessions) {
            List<TutoringQuestion> questions = questionRepo.findByAxisSessionId(session.getId());
            List<Map<String, Object>> completions = new ArrayList<>();
            double scoreSum = 0;
            int completedCount = 0;

            for (Inscription inscription : inscriptions) {
                Enfant student = inscription.getEnfant();
                List<TutoringAttempt> attempts = attemptRepo.findByStudentIdAndQuestionAxisSessionId(
                    student.getId(), session.getId()
                );
                if (attempts.isEmpty()) {
                    continue;
                }

                long correct = attempts.stream().filter(TutoringAttempt::isCorrect).count();
                double score = (double) correct / attempts.size();
                int averageResponseTimeMs = (int) attempts.stream()
                    .filter(attempt -> attempt.getResponseTimeMs() > 0)
                    .mapToInt(TutoringAttempt::getResponseTimeMs)
                    .average()
                    .orElse(0);

                Map<String, Object> completion = new LinkedHashMap<>();
                completion.put("studentId", student.getId());
                completion.put("nom", student.getNom());
                completion.put("prenom", student.getPrenom());
                completion.put("answeredCount", attempts.size());
                completion.put("questionCount", questions.size());
                completion.put("score", score);
                completion.put("averageResponseTimeMs", averageResponseTimeMs);
                completions.add(completion);

                scoreSum += score;
                completedCount++;
            }

            Map<String, Object> sessionData = new LinkedHashMap<>();
            sessionData.put("sessionId", session.getId());
            sessionData.put("contentText", session.getContentText());
            sessionData.put("createdAt", session.getCreatedAt());
            sessionData.put("axisCount", axisRepo.findBySessionId(session.getId()).size());
            sessionData.put("questionCount", questions.size());
            sessionData.put("studentCount", inscriptions.size());
            sessionData.put("completedCount", completedCount);
            sessionData.put("averageScore", completedCount == 0 ? null : scoreSum / completedCount);
            sessionData.put("completions", completions);
            sessionsProgress.add(sessionData);
        }

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("studentCount", inscriptions.size());
        kpis.put("sessionCount", sessions.size());
        kpis.put("questionCount", totalQuestions);
        kpis.put("quizCompletionCount", totalQuizCompletions);
        kpis.put("quizCompletionRate", inscriptions.isEmpty() || sessions.isEmpty() ? 0 : (double) totalQuizCompletions / (inscriptions.size() * sessions.size()));
        kpis.put("homeworkAssignedCount", totalHomeworkAssigned);
        kpis.put("homeworkCompletedCount", totalHomeworkCompleted);
        kpis.put("homeworkCompletionRate", totalHomeworkAssigned == 0 ? 0 : (double) totalHomeworkCompleted / totalHomeworkAssigned);
        kpis.put("averageQuizScore", scoredQuizCount == 0 ? null : totalQuizScore / scoredQuizCount);
        kpis.put("averageResponseTimeMs", timedAttemptCount == 0 ? null : totalResponseTimeMs / timedAttemptCount);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kpis", kpis);
        response.put("students", studentsProgress);
        response.put("sessionCount", sessions.size());
        response.put("sessions", sessionsProgress);
        response.put("homeworks", homeworkProgress);
        return response;
    }

    // Supprime les balises markdown ```json ... ``` que les LLMs ajoutent parfois
    private String stripMarkdownJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```(?:json)?\\s*", "");
            int end = trimmed.lastIndexOf("```");
            if (end >= 0) trimmed = trimmed.substring(0, end);
        }
        return trimmed.trim();
    }
}
