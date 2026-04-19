package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.*;
import CRM_Manara.CRM_Manara.Repository.*;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.AnimateurService;
import CRM_Manara.CRM_Manara.service.TutoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// Contrôleur REST pour le module tutorat adaptatif
@RestController
@RequestMapping("/api/tutoring")
public class ApiTutoringController {

    @Autowired
    private TutoringService tutoringService;

    @Autowired
    private AnimateurService animateurService;

    @Autowired
    private ApiDtoMapper apiDtoMapper;

    @Autowired
    private TutoringSessionRepository sessionRepo;

    @Autowired
    private TutoringAxisRepository axisRepo;

    @Autowired
    private TutoringQuestionRepository questionRepo;

    @Autowired
    private TutoringAxisScoreRepository scoreRepo;

    @Autowired
    private TutoringHomeworkRepository homeworkRepo;

    @Autowired
    private TutoringGroupAlertRepository alertRepo;

    @Autowired
    private TutoringAttemptRepository attemptRepo;

    @Autowired
    private InscriptionRepo inscriptionRepo;

    // --- Liste des animations TUTORAT de l'animateur connecté ---
    @GetMapping("/animations")
    public ResponseEntity<?> getTutoratAnimations(Authentication authentication) {
        try {
            Animateur animateur = animateurService.getAnimateurByEmail(authentication.getName());
            return ResponseEntity.ok(animateurService.getTutoratAnimationsSummary(animateur.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Créer une session (tuteur entre la matière, l'IA génère les axes) ---
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> body) {
        try {
            Long animationId = Long.valueOf(body.get("animationId").toString());
            Long tutorId = Long.valueOf(body.get("tutorId").toString());
            String contentText = (String) body.get("contentText");

            TutoringSession session = tutoringService.createSession(animationId, tutorId, contentText);

            // Retourne la session avec ses axes et questions
            return ResponseEntity.ok(buildSessionResponse(session));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Récupérer une session complète (axes + questions) ---
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable Long sessionId) {
        return sessionRepo.findById(sessionId)
            .map(session -> ResponseEntity.ok(buildSessionResponse(session)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}/sessions/{sessionId}")
    public ResponseEntity<?> getStudentSession(@PathVariable Long studentId, @PathVariable Long sessionId) {
        Optional<TutoringSession> optSession = sessionRepo.findById(sessionId);
        if (optSession.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TutoringSession session = optSession.get();
        boolean enrolled = session.getAnimation() != null
            && inscriptionRepo.findByEnfantIdAndAnimationId(studentId, session.getAnimation().getId()).isPresent();
        if (!enrolled) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Cet élève n'est pas inscrit à l'animation liée à ce quiz."
            ));
        }

        Map<String, Object> response = buildStudentSessionResponse(session);
        int questionCount = ((List<Map<String, Object>>) response.get("axes")).stream()
            .mapToInt(axis -> ((List<Map<String, Object>>) axis.get("questions")).size())
            .sum();
        if (questionCount == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Ce quiz ne contient aucune question. Il doit être regénéré par l'animateur."
            ));
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable Long sessionId, Authentication authentication) {
        try {
            tutoringService.deleteSession(sessionId, authentication.getName());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Soumettre les réponses d'un quiz ---
    @PostMapping("/quiz/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody Map<String, Object> body) {
        try {
            Long studentId = Long.valueOf(body.get("studentId").toString());
            Long sessionId = Long.valueOf(body.get("sessionId").toString());
            List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");

            tutoringService.submitQuizAnswers(studentId, sessionId, answers);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Scores d'un étudiant par axe ---
    @GetMapping("/student/{studentId}/scores")
    public ResponseEntity<?> getStudentScores(@PathVariable Long studentId) {
        List<TutoringAxisScore> scores = scoreRepo.findByStudentId(studentId);
        return ResponseEntity.ok(scores.stream().map(s -> Map.of(
            "axisId", s.getAxis().getId(),
            "axisName", s.getAxis().getName(),
            "score", s.getScore(),
            "masteryStatus", s.getMasteryStatus(),
            "updatedAt", s.getUpdatedAt()
        )).toList());
    }

    // --- Devoirs en attente d'un étudiant ---
    @GetMapping("/student/{studentId}/homework")
    public ResponseEntity<?> getStudentHomework(@PathVariable Long studentId) {
        List<TutoringHomework> homeworks = homeworkRepo.findByStudentIdAndStatus(studentId, "pending");
        return ResponseEntity.ok(homeworks.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("axisId", h.getAxis().getId());
            m.put("axisName", h.getAxis().getName());
            m.put("exercisesJson", h.getExercisesJson());
            m.put("status", h.getStatus());
            m.put("createdAt", h.getCreatedAt());
            return m;
        }).collect(Collectors.toList()));
    }

    // --- Générer un devoir personnalisé ---
    @PostMapping("/homework/{homeworkId}/generate")
    public ResponseEntity<?> generateHomework(
            @PathVariable Long homeworkId,
            @RequestBody Map<String, Object> body) {
        try {
            Long studentId = Long.valueOf(body.get("studentId").toString());
            Long axisId = Long.valueOf(body.get("axisId").toString());

            TutoringHomework homework = tutoringService.generateHomework(studentId, axisId);
            return ResponseEntity.ok(Map.of(
                "id", homework.getId(),
                "exercisesJson", homework.getExercisesJson(),
                "status", homework.getStatus()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Soumettre un devoir (marquer comme terminé) ---
    @PostMapping("/homework/{homeworkId}/submit")
    public ResponseEntity<?> submitHomework(@PathVariable Long homeworkId) {
        return homeworkRepo.findById(homeworkId).map(hw -> {
            hw.setStatus("completed");
            hw.setCompletedAt(LocalDateTime.now());
            homeworkRepo.save(hw);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Questions de révision espacée dues aujourd'hui ---
    @GetMapping("/student/{studentId}/review-questions")
    public ResponseEntity<?> getReviewQuestions(@PathVariable Long studentId) {
        List<TutoringQuestion> questions = tutoringService.getSpacedReviewQuestions(studentId);
        return ResponseEntity.ok(questions.stream().map(this::buildQuestionResponse).toList());
    }

    // --- Tableau de bord tuteur : progression de tous les étudiants ---
    @GetMapping("/tutor/dashboard/{animationId}")
    public ResponseEntity<?> getTutorDashboard(@PathVariable Long animationId) {
        Map<String, Object> progress = tutoringService.getStudentProgress(animationId);
        return ResponseEntity.ok(progress);
    }

    // --- Alertes de groupe pour une animation ---
    @GetMapping("/tutor/alerts/{animationId}")
    public ResponseEntity<?> getAlerts(@PathVariable Long animationId) {
        List<TutoringGroupAlert> alerts = alertRepo.findByAnimationIdOrderByCreatedAtDesc(animationId);
        return ResponseEntity.ok(alerts.stream().map(a -> Map.of(
            "id", a.getId(),
            "axisName", a.getAxis().getName(),
            "failureRate", a.getFailureRate(),
            "affectedCount", a.getAffectedCount(),
            "totalCount", a.getTotalCount(),
            "dominantError", a.getDominantError() != null ? a.getDominantError() : "",
            "createdAt", a.getCreatedAt()
        )).toList());
    }

    // --- Progression pour les parents ---
    @GetMapping("/parent/progress/{enfantId}")
    public ResponseEntity<?> getParentProgress(@PathVariable Long enfantId) {
        List<TutoringAxisScore> scores = scoreRepo.findByStudentId(enfantId);
        return ResponseEntity.ok(scores.stream().map(s -> Map.of(
            "axisId", s.getAxis().getId(),
            "axisName", s.getAxis().getName(),
            "score", s.getScore(),
            "masteryStatus", s.getMasteryStatus(),
            "updatedAt", s.getUpdatedAt()
        )).toList());
    }

    // --- Quiz en attente pour un étudiant (sessions sans aucune tentative) ---
    @GetMapping("/student/{studentId}/pending-quizzes")
    public ResponseEntity<?> getPendingQuizzes(@PathVariable Long studentId) {
        List<TutoringSession> pending = sessionRepo.findPendingByStudentId(studentId);
        return ResponseEntity.ok(pending.stream()
            .filter(s -> !questionRepo.findByAxisSessionId(s.getId()).isEmpty())
            .map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId", s.getId());
            m.put("animationId", s.getAnimation() != null ? s.getAnimation().getId() : null);
            m.put("activityName", s.getAnimation() != null && s.getAnimation().getActivity() != null
                ? s.getAnimation().getActivity().getActivyName()
                : "Tutorat");
            m.put("startTime", s.getAnimation() != null && s.getAnimation().getStartTime() != null ? s.getAnimation().getStartTime().toString() : "");
            m.put("contentText", s.getContentText());
            return m;
        }).collect(Collectors.toList()));
    }

    // --- Historique des 10 derniers événements d'un étudiant ---
    @GetMapping("/student/{studentId}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long studentId) {
        List<Map<String, Object>> events = new ArrayList<>();

        List<TutoringAttempt> attempts = attemptRepo.findByStudentIdOrderByCreatedAtDesc(studentId);
        Set<Long> seenSessions = new LinkedHashSet<>();
        for (TutoringAttempt a : attempts) {
            Long sid = a.getQuestion().getAxis().getSession().getId();
            if (!seenSessions.contains(sid)) {
                seenSessions.add(sid);
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("type", "quiz");
                ev.put("date", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
                ev.put("label", "Quiz : " + a.getQuestion().getAxis().getSession().getAnimation().getActivity().getActivyName());
                events.add(ev);
            }
        }

        List<TutoringHomework> homeworks = homeworkRepo.findByStudentIdOrderByCreatedAtDesc(studentId);
        for (TutoringHomework hw : homeworks) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("type", "homework");
            ev.put("date", hw.getCreatedAt() != null ? hw.getCreatedAt().toString() : "");
            ev.put("label", "Devoir : " + hw.getAxis().getName() + " (" + hw.getStatus() + ")");
            events.add(ev);
        }

        events.sort((a, b) -> {
            String da = (String) a.get("date");
            String db = (String) b.get("date");
            return db.compareTo(da);
        });

        return ResponseEntity.ok(events.stream().limit(10).collect(Collectors.toList()));
    }

    // --- Générer un devoir de groupe pour les étudiants faibles sur un axe ---
    @PostMapping("/homework/generate-group")
    public ResponseEntity<?> generateGroupHomework(@RequestBody Map<String, Object> body) {
        try {
            Long animationId = Long.valueOf(body.get("animationId").toString());
            Long axisId = Long.valueOf(body.get("axisId").toString());
            List<TutoringAxisScore> weakScores = scoreRepo.findByAxisIdAndScoreLessThan(axisId, 0.66);
            int count = 0;
            for (TutoringAxisScore score : weakScores) {
                tutoringService.generateHomework(score.getStudent().getId(), axisId);
                count++;
            }
            return ResponseEntity.ok(Map.of("generated", count, "animationId", animationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Méthode utilitaire : construit la réponse complète d'une session ---
    private Map<String, Object> buildSessionResponse(TutoringSession session) {
        List<Inscription> inscriptions = session.getAnimation() != null
            ? inscriptionRepo.findByAnimationId(session.getAnimation().getId())
            : List.of();
        List<TutoringQuestion> sessionQuestions = questionRepo.findByAxisSessionId(session.getId());
        long completedStudentCount = inscriptions.stream()
            .filter(inscription -> !attemptRepo.findByStudentIdAndQuestionAxisSessionId(
                inscription.getEnfant().getId(),
                session.getId()
            ).isEmpty())
            .count();
        List<Map<String, Object>> axesData = axisRepo.findBySessionId(session.getId())
            .stream().map(axis -> {
                List<Map<String, Object>> questionsData = questionRepo.findByAxisId(axis.getId())
                    .stream().map(this::buildQuestionResponse).toList();
                return Map.of(
                    "id", axis.getId(),
                    "name", axis.getName(),
                    "description", axis.getDescription() != null ? axis.getDescription() : "",
                    "questions", questionsData
                );
            }).toList();

        return Map.of(
            "id", session.getId(),
            "animationId", session.getAnimation().getId(),
            "tutorId", session.getTutor().getId(),
            "contentText", session.getContentText(),
            "createdAt", session.getCreatedAt(),
            "questionCount", sessionQuestions.size(),
            "enrolledStudentCount", inscriptions.size(),
            "completedStudentCount", completedStudentCount,
            "pendingStudentCount", Math.max(0, inscriptions.size() - completedStudentCount),
            "axes", axesData
        );
    }

    private Map<String, Object> buildStudentSessionResponse(TutoringSession session) {
        List<TutoringQuestion> sessionQuestions = questionRepo.findByAxisSessionId(session.getId());
        List<Map<String, Object>> axesData = axisRepo.findBySessionId(session.getId())
            .stream().map(axis -> {
                List<Map<String, Object>> questionsData = questionRepo.findByAxisId(axis.getId())
                    .stream().map(this::buildQuestionResponse).toList();
                return Map.of(
                    "id", axis.getId(),
                    "name", axis.getName(),
                    "description", axis.getDescription() != null ? axis.getDescription() : "",
                    "questions", questionsData
                );
            }).toList();

        return Map.of(
            "id", session.getId(),
            "animationId", session.getAnimation().getId(),
            "contentText", session.getContentText(),
            "createdAt", session.getCreatedAt(),
            "questionCount", sessionQuestions.size(),
            "axes", axesData
        );
    }

    // --- Méthode utilitaire : construit la réponse d'une question ---
    private Map<String, Object> buildQuestionResponse(TutoringQuestion q) {
        return Map.of(
            "id", q.getId(),
            "axisId", q.getAxis().getId(),
            "type", q.getType(),
            "angle", q.getAngle() != null ? q.getAngle() : "",
            "difficulty", q.getDifficulty(),
            "content", q.getContent(),
            "optionsJson", q.getOptionsJson() != null ? q.getOptionsJson() : "[]",
            "correctAnswer", q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "",
            "explanation", q.getExplanation() != null ? q.getExplanation() : ""
        );
    }
}
