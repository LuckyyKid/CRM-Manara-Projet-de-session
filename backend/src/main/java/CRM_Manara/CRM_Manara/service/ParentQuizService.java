package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
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
import CRM_Manara.CRM_Manara.dto.ParentQuizDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptSubmitDto;
import CRM_Manara.CRM_Manara.dto.QuizAxisDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.QuizQuestionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ParentQuizService {

    private final parentService parentService;
    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final InscriptionRepo inscriptionRepo;

    public ParentQuizService(parentService parentService,
                             QuizRepo quizRepo,
                             QuizAttemptRepo quizAttemptRepo,
                             InscriptionRepo inscriptionRepo) {
        this.parentService = parentService;
        this.quizRepo = quizRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.inscriptionRepo = inscriptionRepo;
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
        Enfant enfant = parentService.getEnfantForParent(request.enfantId(), parentEmail);

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

        return toAttemptDto(quizAttemptRepo.save(attempt));
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptDto> listAttempts(String parentEmail) {
        Parent parent = parentService.getParentByEmail(parentEmail);
        return quizAttemptRepo.findByEnfantParentIdOrderBySubmittedAtDesc(parent.getId()).stream()
                .map(this::toAttemptDto)
                .toList();
    }

    private List<Inscription> eligibleInscriptions(Long parentId) {
        return inscriptionRepo.findByParentId(parentId).stream()
                .filter(inscription -> isVisibleInscriptionStatus(inscription.getStatusInscription()))
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
            result.putIfAbsent(attempt.getQuiz().getId(), attempt);
        }
        return result;
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
                                                question.getPosition()
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
