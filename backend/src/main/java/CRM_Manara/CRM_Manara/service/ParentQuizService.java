package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAxis;
import CRM_Manara.CRM_Manara.Model.Entity.QuizQuestion;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.QuizAttemptRepo;
import CRM_Manara.CRM_Manara.Repository.QuizRepo;
import CRM_Manara.CRM_Manara.dto.EnfantSummaryDto;
import CRM_Manara.CRM_Manara.dto.HomeworkDto;
import CRM_Manara.CRM_Manara.dto.ParentQuizAttemptDetailDto;
import CRM_Manara.CRM_Manara.dto.ParentQuizDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptSubmitDto;
import CRM_Manara.CRM_Manara.dto.QuizAxisDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.QuizQuestionDto;
import CRM_Manara.CRM_Manara.dto.TutorQuizAnswerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ParentQuizService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParentQuizService.class);
    private static final double ANSWER_SUCCESS_THRESHOLD = 66.0;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}|\\d+(?:[.,]\\d+)?");
    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "des", "les", "une", "pour", "que", "qui", "sur", "aux", "par",
            "plus", "moins", "comme", "cette", "cela", "donc", "mais", "car", "est", "sont",
            "reponse", "etudiant", "etudiante", "doit", "question", "axe", "notes"
    );

    private final parentService parentService;
    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final InscriptionRepo inscriptionRepo;
    private final AnthropicQuizScoringService anthropicQuizScoringService;
    private final HomeworkService homeworkService;
    private final EmailService emailService;
    private final AnimateurNotificationService animateurNotificationService;

    public ParentQuizService(parentService parentService,
                             QuizRepo quizRepo,
                             QuizAttemptRepo quizAttemptRepo,
                             InscriptionRepo inscriptionRepo,
                             AnthropicQuizScoringService anthropicQuizScoringService,
                             HomeworkService homeworkService,
                             EmailService emailService,
                             AnimateurNotificationService animateurNotificationService) {
        this.parentService = parentService;
        this.quizRepo = quizRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.inscriptionRepo = inscriptionRepo;
        this.anthropicQuizScoringService = anthropicQuizScoringService;
        this.homeworkService = homeworkService;
        this.emailService = emailService;
        this.animateurNotificationService = animateurNotificationService;
    }

    @Transactional(readOnly = true)
    public List<ParentQuizDto> listAvailableQuizzes(String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        List<Inscription> inscriptions = eligibleInscriptions(parent.getId());
        Map<Long, List<Enfant>> childrenByAnimation = childrenByAnimation(inscriptions);
        Map<Long, QuizAttempt> latestAttemptByQuiz = latestAttemptByQuiz(parent.getId());

        return quizRepo.findVisibleForParent(parent.getId(), visibleStatuses()).stream()
                .map(quiz -> toParentQuizDto(quiz, childrenByAnimation, latestAttemptByQuiz.get(quiz.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ParentQuizDto getAvailableQuiz(Long quizId, String parentEmail) {
        return listAvailableQuizzes(parentEmail).stream()
                .filter(parentQuiz -> parentQuiz.quiz().id().equals(quizId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable pour ce parent."));
    }

    @Transactional
    public QuizAttemptDto submitAttempt(Long quizId, QuizAttemptSubmitDto request, String parentEmail) {
        if (request == null || request.enfantId() == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un enfant et des reponses sont requis.");
        }

        Parent parent = parentService.getParentByEmail(parentEmail);
        Quiz quiz = quizRepo.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable."));
        if (!isTutoringQuiz(quiz)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable.");
        }
        Enfant enfant = parentService.getEnfantForParent(request.enfantId(), parentEmail);
        LOGGER.info("Soumission de quiz recue. quizId={}, enfantId={}, parentId={}, answerCount={}",
                quizId, enfant.getId(), parent.getId(), request.answers().size());

        if (!isEligibleForQuiz(parent.getId(), enfant, quiz)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet enfant ne peut pas faire ce quiz.");
        }
        if (quizAttemptRepo.existsByQuizIdAndEnfantId(quiz.getId(), enfant.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce quiz a deja ete soumis pour cet enfant.");
        }

        Map<Long, QuizQuestion> questionsById = quiz.getAxes().stream()
                .flatMap(axis -> axis.getQuestions().stream())
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        QuizAttempt attempt = new QuizAttempt(quiz, enfant, normalizeElapsedSeconds(request.elapsedSeconds()));

        for (var submittedAnswer : request.answers()) {
            if (submittedAnswer.questionId() == null || submittedAnswer.answerText() == null
                    || submittedAnswer.answerText().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toutes les questions doivent avoir une reponse.");
            }
            QuizQuestion question = questionsById.get(submittedAnswer.questionId());
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question invalide pour ce quiz.");
            }
            attempt.addAnswer(new QuizAnswer(question, submittedAnswer.answerText().trim()));
        }
        if (attempt.getAnswers().size() != questionsById.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toutes les questions du quiz doivent etre repondues.");
        }

        AnthropicQuizScoringService.ScoringResult scoringResult = anthropicQuizScoringService.scoreAttempt(attempt);
        attempt.markScored(scoringResult.scorePercent(), scoringResult.status());
        QuizAttempt savedAttempt = quizAttemptRepo.save(attempt);
        LOGGER.info("QuizAttempt sauvegarde. attemptId={}, quizId={}, enfantId={}, score={}, status={}",
                savedAttempt.getId(), quizId, enfant.getId(), savedAttempt.getScorePercent(), savedAttempt.getStatus());
        notifyAnimateurOfQuizSubmission(savedAttempt);
        try {
            homeworkService.createAutomaticHomeworkFromQuizAttempt(savedAttempt);
        } catch (Exception exception) {
            LOGGER.error("Creation automatique du devoir echouee apres la soumission du quiz {} et de l'attempt {}.",
                    quizId, savedAttempt.getId(), exception);
        }
        return toAttemptDto(savedAttempt);
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptDto> listAttempts(String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        return quizAttemptRepo.findByEnfantParentIdOrderBySubmittedAtDesc(parent.getId()).stream()
                .map(this::toAttemptDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParentQuizAttemptDetailDto getAttemptDetail(Long attemptId, String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        QuizAttempt attempt = quizAttemptRepo.findByIdAndEnfantParentId(attemptId, parent.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission introuvable."));
        if (!isTutoringQuiz(attempt.getQuiz())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission introuvable.");
        }
        return toAttemptDetailDto(attempt);
    }

    @Transactional
    public HomeworkDto generateHomeworkFromAttempt(Long attemptId, String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        QuizAttempt attempt = quizAttemptRepo.findByIdAndEnfantParentId(attemptId, parent.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission introuvable."));
        if (!isTutoringQuiz(attempt.getQuiz())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission introuvable.");
        }
        LOGGER.info("Generation manuelle demandee depuis une soumission existante. attemptId={}, parentId={}, enfantId={}",
                attemptId, parent.getId(), attempt.getEnfant().getId());
        try {
            return homeworkService.generateHomeworkForExistingQuizAttempt(attempt);
        } catch (Exception exception) {
            LOGGER.error("Generation manuelle du devoir echouee pour la soumission {}.", attemptId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le devoir n'a pas pu etre genere pour cette soumission.");
        }
    }

    private List<Inscription> eligibleInscriptions(Long parentId) {
        return inscriptionRepo.findByParentId(parentId).stream()
                .filter(inscription -> isVisibleInscriptionStatus(inscription.getStatusInscription()))
                .filter(inscription -> inscription.getAnimation() != null
                        && inscription.getAnimation().getActivity() != null
                        && inscription.getAnimation().getActivity().getType() == typeActivity.TUTORAT)
                .toList();
    }

    private List<statusInscription> visibleStatuses() {
        return List.of(statusInscription.EN_ATTENTE, statusInscription.APPROUVEE, statusInscription.ACTIF);
    }

    private boolean isVisibleInscriptionStatus(statusInscription status) {
        return status == statusInscription.EN_ATTENTE
                || status == statusInscription.APPROUVEE
                || status == statusInscription.ACTIF;
    }

    private boolean isEligibleForQuiz(Long parentId, Enfant enfant, Quiz quiz) {
        if (quiz.getAnimation() == null) {
            return false;
        }
        return eligibleInscriptions(parentId).stream()
                .anyMatch(inscription -> inscription.getEnfant() != null
                        && inscription.getEnfant().getId().equals(enfant.getId())
                        && inscription.getAnimation() != null
                        && inscription.getAnimation().getId().equals(quiz.getAnimation().getId()));
    }

    private Map<Long, List<Enfant>> childrenByAnimation(List<Inscription> inscriptions) {
        return inscriptions.stream()
                .filter(inscription -> inscription.getAnimation() != null && inscription.getEnfant() != null)
                .collect(Collectors.groupingBy(
                        inscription -> inscription.getAnimation().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(Inscription::getEnfant, Collectors.toList())
                ));
    }

    private Map<Long, QuizAttempt> latestAttemptByQuiz(Long parentId) {
        Map<Long, QuizAttempt> result = new LinkedHashMap<>();
        for (QuizAttempt attempt : quizAttemptRepo.findByEnfantParentIdOrderBySubmittedAtDesc(parentId)) {
            if (isTutoringQuiz(attempt.getQuiz())) {
                result.putIfAbsent(attempt.getQuiz().getId(), attempt);
            }
        }
        return result;
    }

    private boolean isTutoringQuiz(Quiz quiz) {
        return quiz != null
                && quiz.getAnimation() != null
                && quiz.getAnimation().getActivity() != null
                && quiz.getAnimation().getActivity().getType() == typeActivity.TUTORAT;
    }

    private ParentQuizDto toParentQuizDto(Quiz quiz,
                                          Map<Long, List<Enfant>> childrenByAnimation,
                                          QuizAttempt latestAttempt) {
        List<Enfant> eligibleSource = quiz.getAnimation() == null
                ? List.of()
                : childrenByAnimation.getOrDefault(quiz.getAnimation().getId(), List.of());
        List<EnfantSummaryDto> eligibleChildren = eligibleSource
                .stream()
                .map(enfant -> new EnfantSummaryDto(
                        enfant.getId(),
                        enfant.getNom(),
                        enfant.getPrenom(),
                        toLocalDate(enfant.getDate_de_naissance()),
                        enfant.isActive()
                ))
                .toList();
        LocalDateTime latestSubmittedAt = latestAttempt != null ? latestAttempt.getSubmittedAt() : null;
        return new ParentQuizDto(toQuizDto(quiz), eligibleChildren, latestAttempt != null, latestSubmittedAt);
    }

    private QuizAttemptDto toAttemptDto(QuizAttempt attempt) {
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();
        return new QuizAttemptDto(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getEnfant().getId(),
                enfantName,
                attempt.getSubmittedAt(),
                attempt.getElapsedSeconds(),
                attempt.getScorePercent(),
                attempt.getStatus()
        );
    }

    private ParentQuizAttemptDetailDto toAttemptDetailDto(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();
        return new ParentQuizAttemptDetailDto(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getAnimation() == null ? null : quiz.getAnimation().getId(),
                quiz.getAnimation() == null || quiz.getAnimation().getActivity() == null
                        ? null
                        : quiz.getAnimation().getActivity().getActivyName(),
                attempt.getEnfant().getId(),
                enfantName,
                attempt.getSubmittedAt(),
                attempt.getElapsedSeconds(),
                attempt.getScorePercent(),
                attempt.getStatus(),
                attempt.getAnswers().stream()
                        .map(answer -> new TutorQuizAnswerDto(
                                answer.getQuestion().getId(),
                                answer.getQuestion().getAxis().getTitle(),
                                answer.getQuestion().getAngle(),
                                answer.getQuestion().getQuestionText(),
                                answer.getQuestion().getExpectedAnswer(),
                                answer.getAnswerText(),
                                answer.getQuestion().getOptions(),
                                scoreQuizAnswer(answer),
                                scoreQuizAnswer(answer) >= ANSWER_SUCCESS_THRESHOLD,
                                quizFeedback(scoreQuizAnswer(answer))
                        ))
                        .toList()
        );
    }

    private double scoreQuizAnswer(QuizAnswer answer) {
        Set<String> expectedTokens = keywords(answer.getQuestion().getExpectedAnswer());
        Set<String> answerTokens = keywords(answer.getAnswerText());
        if (answerTokens.isEmpty()) {
            return 0;
        }
        if (expectedTokens.isEmpty()) {
            return answer.getAnswerText().trim().length() >= 20 ? 60 : 35;
        }
        long matches = expectedTokens.stream().filter(answerTokens::contains).count();
        double overlap = (double) matches / expectedTokens.size();
        return Math.max(0, Math.min(100, 20 + (overlap * 80)));
    }

    private Set<String> keywords(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(value == null ? "" : value.toLowerCase());
        while (matcher.find()) {
            String token = Normalizer.normalize(matcher.group(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replace("'", "")
                    .replace("-", "");
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String quizFeedback(double score) {
        if (score >= ANSWER_SUCCESS_THRESHOLD) {
            return "Reponse suffisante: l'idee principale est presente.";
        }
        if (score < 35) {
            return "La reponse ne reprend pas les notions attendues. Revoir la definition ou la methode avant de refaire l'exercice.";
        }
        return "La piste est partielle. Il fallait expliciter les mots cles de la reponse attendue et justifier davantage.";
    }

    private QuizDto toQuizDto(Quiz quiz) {
        Long animationId = quiz.getAnimation() != null ? quiz.getAnimation().getId() : null;
        String activityName = quiz.getAnimation() != null && quiz.getAnimation().getActivity() != null
                ? quiz.getAnimation().getActivity().getActivyName()
                : null;
        return new QuizDto(
                quiz.getId(),
                quiz.getTitle(),
                "",
                quiz.getCreatedAt(),
                animationId,
                activityName,
                quiz.getAxes().stream()
                        .sorted(Comparator.comparingInt(QuizAxis::getPosition))
                        .map(axis -> new QuizAxisDto(
                                axis.getId(),
                                axis.getTitle(),
                                axis.getSummary(),
                                axis.getPosition(),
                                axis.getQuestions().stream()
                                        .sorted(Comparator.comparingInt(QuizQuestion::getPosition))
                                        .map(question -> new QuizQuestionDto(
                                                question.getId(),
                                                question.getAngle(),
                                                question.getType(),
                                                question.getQuestionText(),
                                                "",
                                                question.getPosition(),
                                                question.getOptions()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private Integer normalizeElapsedSeconds(Integer elapsedSeconds) {
        if (elapsedSeconds == null || elapsedSeconds < 0) {
            return null;
        }
        return elapsedSeconds;
    }

    private void notifyAnimateurOfQuizSubmission(QuizAttempt attempt) {
        if (attempt == null
                || attempt.getQuiz() == null
                || attempt.getQuiz().getAnimateur() == null
                || attempt.getQuiz().getAnimateur().getUser() == null) {
            return;
        }

        String animateurEmail = attempt.getQuiz().getAnimateur().getUser().getEmail();
        if (animateurEmail == null || animateurEmail.isBlank()) {
            return;
        }

        String animateurName = attempt.getQuiz().getAnimateur().getPrenom() + " " + attempt.getQuiz().getAnimateur().getNom();
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();
        String activityName = attempt.getQuiz().getAnimation() != null && attempt.getQuiz().getAnimation().getActivity() != null
                ? attempt.getQuiz().getAnimation().getActivity().getActivyName()
                : null;

        emailService.sendQuizSubmissionEmail(
                animateurEmail,
                animateurName.trim(),
                enfantName,
                attempt.getQuiz().getTitle(),
                activityName,
                attempt.getScorePercent()
        );
        animateurNotificationService.createForAnimateur(
                attempt.getQuiz().getAnimateur(),
                "QUIZ_SUBMISSION",
                "Nouvelle soumission de quiz",
                enfantName + " a soumis le quiz \"" + attempt.getQuiz().getTitle() + "\"."
        );
    }

    private java.time.LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
