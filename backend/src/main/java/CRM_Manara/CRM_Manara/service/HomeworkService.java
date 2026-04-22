package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAssignment;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAttempt;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkExercise;
import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
import CRM_Manara.CRM_Manara.Repository.HomeworkAssignmentRepo;
import CRM_Manara.CRM_Manara.Repository.HomeworkAttemptRepo;
import CRM_Manara.CRM_Manara.Repository.QuizAttemptRepo;
import CRM_Manara.CRM_Manara.dto.AnimateurHomeworkOverviewDto;
import CRM_Manara.CRM_Manara.dto.AnimateurHomeworkStudentDetailDto;
import CRM_Manara.CRM_Manara.dto.AnimateurHomeworkStudentRowDto;
import CRM_Manara.CRM_Manara.dto.HomeworkAttemptDto;
import CRM_Manara.CRM_Manara.dto.HomeworkAttemptSubmitDto;
import CRM_Manara.CRM_Manara.dto.HomeworkDto;
import CRM_Manara.CRM_Manara.dto.HomeworkExerciseDto;
import CRM_Manara.CRM_Manara.dto.TutorQuizAnswerDto;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HomeworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeworkService.class);

    private static final double WEAK_AXIS_THRESHOLD = 66.0;
    private static final double REVIEW_AXIS_THRESHOLD = 85.0;
    private static final int AXIS_HISTORY_SIZE = 3;
    private static final int REVIEW_DELAY_DAYS = 10;
    private static final int IMMEDIATE_AXIS_LIMIT = 3;
    private static final int MAX_EXERCISE_TEXT_LENGTH = 500;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}|\\d+(?:[.,]\\d+)?");
    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "des", "les", "une", "pour", "que", "qui", "sur", "aux", "par",
            "plus", "moins", "comme", "cette", "cela", "donc", "mais", "car", "est", "sont",
            "reponse", "etudiant", "etudiante", "doit", "question", "axe", "notes"
    );

    private final HomeworkAssignmentRepo homeworkAssignmentRepo;
    private final HomeworkAttemptRepo homeworkAttemptRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final AnthropicHomeworkGenerationService anthropicHomeworkGenerationService;
    private final AnthropicHomeworkScoringService anthropicHomeworkScoringService;
    private final HomeworkTemplateService homeworkTemplateService;
    private final ParentNotificationService parentNotificationService;
    private final EmailService emailService;
    private final AnimateurNotificationService animateurNotificationService;

    public HomeworkService(HomeworkAssignmentRepo homeworkAssignmentRepo,
                           HomeworkAttemptRepo homeworkAttemptRepo,
                           QuizAttemptRepo quizAttemptRepo,
                           AnthropicHomeworkGenerationService anthropicHomeworkGenerationService,
                           AnthropicHomeworkScoringService anthropicHomeworkScoringService,
                           HomeworkTemplateService homeworkTemplateService,
                           ParentNotificationService parentNotificationService,
                           EmailService emailService,
                           AnimateurNotificationService animateurNotificationService) {
        this.homeworkAssignmentRepo = homeworkAssignmentRepo;
        this.homeworkAttemptRepo = homeworkAttemptRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.anthropicHomeworkGenerationService = anthropicHomeworkGenerationService;
        this.anthropicHomeworkScoringService = anthropicHomeworkScoringService;
        this.homeworkTemplateService = homeworkTemplateService;
        this.parentNotificationService = parentNotificationService;
        this.emailService = emailService;
        this.animateurNotificationService = animateurNotificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createAutomaticHomeworkFromQuizAttempt(QuizAttempt sourceAttempt) {
        if (sourceAttempt == null
                || sourceAttempt.getId() == null
                || sourceAttempt.getEnfant() == null
                || sourceAttempt.getQuiz() == null
                || homeworkAssignmentRepo.existsBySourceAttemptIdAndEnfantId(sourceAttempt.getId(), sourceAttempt.getEnfant().getId())) {
            LOGGER.info("Generation automatique ignoree. attemptId={}, enfantId={}, quizId={}, reason={}",
                    sourceAttempt == null ? null : sourceAttempt.getId(),
                    sourceAttempt == null || sourceAttempt.getEnfant() == null ? null : sourceAttempt.getEnfant().getId(),
                    sourceAttempt == null || sourceAttempt.getQuiz() == null ? null : sourceAttempt.getQuiz().getId(),
                    sourceAttempt == null ? "attempt_null"
                            : sourceAttempt.getId() == null ? "attempt_id_null"
                            : sourceAttempt.getEnfant() == null ? "enfant_null"
                            : sourceAttempt.getQuiz() == null ? "quiz_null"
                            : "homework_already_exists");
            return false;
        }

        LOGGER.info("Generation automatique du devoir lancee. attemptId={}, enfantId={}, quizId={}",
                sourceAttempt.getId(), sourceAttempt.getEnfant().getId(), sourceAttempt.getQuiz().getId());
        List<AxisNeed> weakAxes = determineImmediateAxisNeeds(sourceAttempt);
        if (weakAxes.isEmpty()) {
            LOGGER.warn("Aucun axe faible detecte pour la generation automatique. attemptId={}, enfantId={}",
                    sourceAttempt.getId(), sourceAttempt.getEnfant().getId());
            return false;
        }

        HomeworkAssignment assignment = buildAssignment(
                sourceAttempt.getEnfant(),
                sourceAttempt.getQuiz(),
                sourceAttempt,
                weakAxes,
                false
        );
        HomeworkAssignment savedAssignment = homeworkAssignmentRepo.save(assignment);
        notifyParentOfHomework(savedAssignment);
        LOGGER.info("Devoir automatique sauvegarde. assignmentId={}, attemptId={}, exerciseCount={}",
                savedAssignment.getId(), sourceAttempt.getId(), savedAssignment.getExercises().size());
        return true;
    }

    @Transactional
    public HomeworkDto generateHomeworkForExistingQuizAttempt(QuizAttempt sourceAttempt) {
        if (sourceAttempt == null || sourceAttempt.getId() == null || sourceAttempt.getEnfant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Soumission de quiz invalide.");
        }

        HomeworkAssignment existingAssignment = homeworkAssignmentRepo
                .findBySourceAttemptIdAndEnfantParentId(sourceAttempt.getId(), sourceAttempt.getEnfant().getParent().getId())
                .orElse(null);
        if (existingAssignment != null) {
            LOGGER.info("Generation manuelle reutilise un devoir existant. attemptId={}, assignmentId={}",
                    sourceAttempt.getId(), existingAssignment.getId());
            return toHomeworkDto(existingAssignment);
        }

        LOGGER.info("Generation manuelle du devoir lancee. attemptId={}, enfantId={}, quizId={}",
                sourceAttempt.getId(), sourceAttempt.getEnfant().getId(),
                sourceAttempt.getQuiz() == null ? null : sourceAttempt.getQuiz().getId());
        List<AxisNeed> weakAxes = determineImmediateAxisNeeds(sourceAttempt);
        if (weakAxes.isEmpty()) {
            LOGGER.warn("Generation manuelle impossible: aucun axe faible detecte. attemptId={}, enfantId={}",
                    sourceAttempt.getId(), sourceAttempt.getEnfant().getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun axe faible n'a ete detecte pour generer un devoir.");
        }

        HomeworkAssignment assignment = buildAssignment(
                sourceAttempt.getEnfant(),
                sourceAttempt.getQuiz(),
                sourceAttempt,
                weakAxes,
                false
        );
        HomeworkAssignment savedAssignment = homeworkAssignmentRepo.save(assignment);
        notifyParentOfHomework(savedAssignment);
        LOGGER.info("Devoir manuel sauvegarde. assignmentId={}, attemptId={}, exerciseCount={}",
                savedAssignment.getId(), sourceAttempt.getId(), savedAssignment.getExercises().size());
        return toHomeworkDto(savedAssignment);
    }

    @Transactional(readOnly = true)
    public List<HomeworkDto> listAssignmentsForParent(String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        return homeworkAssignmentRepo.findByEnfantParentIdOrderByCreatedAtDesc(parentId).stream()
                .filter(this::isTutoringAssignment)
                .map(this::toHomeworkDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeworkDto getAssignmentForParent(Long assignmentId, String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        HomeworkAssignment assignment = homeworkAssignmentRepo.findByIdAndEnfantParentId(assignmentId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable."));
        if (!isTutoringAssignment(assignment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable.");
        }
        return toHomeworkDto(assignment);
    }

    @Transactional
    public HomeworkAttemptDto submitAssignment(Long assignmentId,
                                               HomeworkAttemptSubmitDto request,
                                               String parentEmail,
                                               parentService parentService) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les reponses du devoir sont requises.");
        }
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        HomeworkAssignment assignment = homeworkAssignmentRepo.findByIdAndEnfantParentId(assignmentId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable."));
        if (!isTutoringAssignment(assignment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable.");
        }
        if (!assignment.getAttempts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce devoir a deja ete soumis.");
        }

        Map<Long, HomeworkExercise> exercisesById = assignment.getExercises().stream()
                .collect(Collectors.toMap(HomeworkExercise::getId, exercise -> exercise));
        HomeworkAttempt attempt = new HomeworkAttempt(assignment, assignment.getEnfant(), normalizeElapsedSeconds(request.elapsedSeconds()));

        for (var submitted : request.answers()) {
            if (submitted.exerciseId() == null || submitted.answerText() == null || submitted.answerText().trim().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Toutes les questions du devoir doivent avoir une reponse.");
            }
            HomeworkExercise exercise = exercisesById.get(submitted.exerciseId());
            if (exercise == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercice invalide pour ce devoir.");
            }
            attempt.addAnswer(new HomeworkAnswer(exercise, submitted.answerText().trim()));
        }
        if (attempt.getAnswers().size() != assignment.getExercises().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les exercices du devoir doivent etre repondus.");
        }

        assignment.addAttempt(attempt);
        homeworkAttemptRepo.save(attempt);

        assignment.markCompleted();
        attempt.markStatus("PENDING_AI");

        var scoringResult = anthropicHomeworkScoringService.scoreAttempt(attempt);
        if (scoringResult.isPresent()) {
            double score = scoringResult.get().scorePercent();
            attempt.markScored(score, scoringResult.get().status());

            if (score < WEAK_AXIS_THRESHOLD) {
                HomeworkAssignment followUp = buildAssignment(
                        assignment.getEnfant(),
                        assignment.getSourceQuiz(),
                        assignment.getSourceAttempt(),
                        deriveNeedsFromHomework(assignment, score, false),
                        false
                );
                homeworkAssignmentRepo.save(followUp);
            } else {
                HomeworkAssignment review = buildAssignment(
                        assignment.getEnfant(),
                        assignment.getSourceQuiz(),
                        assignment.getSourceAttempt(),
                        deriveNeedsFromHomework(assignment, score, true),
                        true
                );
                homeworkAssignmentRepo.save(review);
            }
        } else {
            LOGGER.warn("Soumission de devoir en attente de correction IA. assignmentId={}, attemptId={}",
                    assignment.getId(), attempt.getId());
        }

        notifyAnimateurOfHomeworkSubmission(attempt);

        return toAttemptDto(attempt);
    }

    @Transactional(readOnly = true)
    public List<HomeworkAttemptDto> listAttemptsForParent(String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        return homeworkAttemptRepo.findByEnfantParentIdOrderBySubmittedAtDesc(parentId).stream()
                .filter(attempt -> isTutoringAssignment(attempt.getAssignment()))
                .map(this::toAttemptDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeworkAttemptDto getAttemptDetailForParent(Long attemptId, String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        HomeworkAttempt attempt = homeworkAttemptRepo.findByIdAndEnfantParentId(attemptId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission du devoir introuvable."));
        if (!isTutoringAssignment(attempt.getAssignment())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission du devoir introuvable.");
        }
        return toAttemptDto(attempt);
    }

    @Transactional(readOnly = true)
    public AnimateurHomeworkOverviewDto getOverviewForAnimateur(String animateurEmail, AnimateurService animateurService) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        List<HomeworkAssignment> assignments = homeworkAssignmentRepo.findByAnimateurIdOrderByCreatedAtDesc(animateurId).stream()
                .filter(this::isTutoringAssignment)
                .toList();
        List<HomeworkAttempt> attempts = homeworkAttemptRepo.findByAssignmentAnimateurIdOrderBySubmittedAtDesc(animateurId).stream()
                .filter(attempt -> isTutoringAssignment(attempt.getAssignment()))
                .toList();

        List<AnimateurHomeworkStudentRowDto> students = assignments.stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getEnfant().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(studentAssignments -> toAnimateurStudentRow(studentAssignments, attempts))
                .sorted(Comparator.comparing(AnimateurHomeworkStudentRowDto::enfantName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        long assignedCount = assignments.size();
        long submittedCount = assignments.stream().filter(assignment -> !assignment.getAttempts().isEmpty()).count();
        return new AnimateurHomeworkOverviewDto(
                assignedCount,
                submittedCount,
                Math.max(0, assignedCount - submittedCount),
                students.size(),
                students
        );
    }

    @Transactional(readOnly = true)
    public AnimateurHomeworkStudentDetailDto getStudentDetailForAnimateur(Long enfantId,
                                                                          String animateurEmail,
                                                                          AnimateurService animateurService) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        List<HomeworkAssignment> assignments = homeworkAssignmentRepo.findByAnimateurIdAndEnfantIdOrderByCreatedAtDesc(animateurId, enfantId).stream()
                .filter(this::isTutoringAssignment)
                .toList();
        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun devoir trouve pour cet etudiant.");
        }
        List<HomeworkAttempt> attempts = homeworkAttemptRepo.findByAssignmentAnimateurIdAndEnfantIdOrderBySubmittedAtDesc(animateurId, enfantId).stream()
                .filter(attempt -> isTutoringAssignment(attempt.getAssignment()))
                .toList();

        AnimateurHomeworkStudentRowDto row = toAnimateurStudentRow(assignments, attempts);
        return new AnimateurHomeworkStudentDetailDto(
                row.enfantId(),
                row.enfantName(),
                row.assignedCount(),
                row.submittedCount(),
                row.remainingCount(),
                row.averageScorePercent(),
                assignments.stream().map(this::toHomeworkDto).toList(),
                attempts.stream().map(this::toAttemptDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public HomeworkDto getAssignmentForAnimateur(Long assignmentId, String animateurEmail, AnimateurService animateurService) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        HomeworkAssignment assignment = homeworkAssignmentRepo.findByIdAndAnimateurId(assignmentId, animateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable."));
        if (!isTutoringAssignment(assignment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable.");
        }
        return toHomeworkDto(assignment);
    }

    @Transactional(readOnly = true)
    public HomeworkAttemptDto getLatestAttemptForAnimateurAssignment(Long assignmentId,
                                                                     String animateurEmail,
                                                                     AnimateurService animateurService) {
        Long animateurId = animateurService.getAnimateurByEmail(animateurEmail).getId();
        HomeworkAttempt attempt = homeworkAttemptRepo.findTopByAssignmentIdAndAssignmentAnimateurIdOrderBySubmittedAtDesc(assignmentId, animateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune soumission trouvee pour ce devoir."));
        if (!isTutoringAssignment(attempt.getAssignment())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune soumission trouvee pour ce devoir.");
        }
        return toAttemptDto(attempt);
    }

    private HomeworkAssignment buildAssignment(Enfant enfant,
                                               Quiz sourceQuiz,
                                               QuizAttempt sourceAttempt,
                                               List<AxisNeed> axisNeeds,
                                               boolean reviewMode) {
        String activityName = sourceQuiz != null && sourceQuiz.getAnimation() != null && sourceQuiz.getAnimation().getActivity() != null
                ? sourceQuiz.getAnimation().getActivity().getActivyName()
                : null;
        String childName = enfant.getPrenom() + " " + enfant.getNom();
        LOGGER.info("Construction du devoir. enfantId={}, quizId={}, attemptId={}, reviewMode={}, activityName={}, axisNeeds={}",
                enfant.getId(),
                sourceQuiz == null ? null : sourceQuiz.getId(),
                sourceAttempt == null ? null : sourceAttempt.getId(),
                reviewMode,
                activityName,
                summarizeAxisNeeds(axisNeeds));
        AnthropicHomeworkGenerationService.GeneratedHomework draftHomework = homeworkTemplateService
                .buildDraft(activityName, axisNeeds, reviewMode);
        LOGGER.info("Brouillon local genere. attemptId={}, title={}, exerciseCount={}",
                sourceAttempt == null ? null : sourceAttempt.getId(),
                draftHomework.title(),
                draftHomework.exercises().size());
        AnthropicHomeworkGenerationService.GeneratedHomework generatedHomework = anthropicHomeworkGenerationService
                .generate(activityName, childName, axisNeeds, reviewMode, draftHomework)
                .map(homework -> validateAndMergeHomework(homework, draftHomework))
                .orElseGet(() -> {
                    LOGGER.warn("Fallback local conserve pour le devoir. attemptId={}, draftExerciseCount={}",
                            sourceAttempt == null ? null : sourceAttempt.getId(),
                            draftHomework.exercises().size());
                    return draftHomework;
                });

        Animation animation = sourceQuiz != null ? sourceQuiz.getAnimation() : null;
        HomeworkAssignment assignment = new HomeworkAssignment(
                sourceQuiz != null ? sourceQuiz.getAnimateur() : sourceAttempt.getQuiz().getAnimateur(),
                enfant,
                animation,
                sourceQuiz,
                sourceAttempt,
                generatedHomework.title(),
                generatedHomework.summary(),
                LocalDate.now().plusDays(reviewMode ? REVIEW_DELAY_DAYS : 4)
        );

        for (int i = 0; i < generatedHomework.exercises().size(); i++) {
            AnthropicHomeworkGenerationService.GeneratedExercise exercise = generatedHomework.exercises().get(i);
            assignment.addExercise(new HomeworkExercise(
                    exercise.axisTitle(),
                    normalizeExerciseType(exercise.type(), exercise.options()),
                    normalizeDifficulty(exercise.difficulty()),
                    exercise.questionText(),
                    exercise.expectedAnswer(),
                    exercise.targetMistake(),
                    i + 1,
                    sanitizeOptions(exercise.options())
            ));
        }
        return assignment;
    }

    private AnthropicHomeworkGenerationService.GeneratedHomework validateAndMergeHomework(
            AnthropicHomeworkGenerationService.GeneratedHomework generated,
            AnthropicHomeworkGenerationService.GeneratedHomework draft
    ) {
        if (generated == null || generated.exercises().isEmpty()) {
            LOGGER.warn("Validation du devoir IA impossible: contenu vide. Fallback complet sur le brouillon.");
            return draft;
        }

        String title = sanitizeHomeworkText(generated.title(), 120);
        String summary = sanitizeHomeworkText(generated.summary(), 220);
        List<AnthropicHomeworkGenerationService.GeneratedExercise> mergedExercises = new ArrayList<>();
        List<AnthropicHomeworkGenerationService.GeneratedExercise> draftExercises = draft.exercises();
        for (int i = 0; i < draftExercises.size(); i++) {
            AnthropicHomeworkGenerationService.GeneratedExercise draftExercise = draftExercises.get(i);
            AnthropicHomeworkGenerationService.GeneratedExercise generatedExercise = i < generated.exercises().size()
                    ? generated.exercises().get(i)
                    : null;
            if (generatedExercise == null) {
                LOGGER.warn("Exercice IA manquant a la position {}. Reutilisation du brouillon.", i + 1);
                mergedExercises.add(draftExercise);
                continue;
            }
            mergedExercises.add(validateExercise(generatedExercise, draftExercise));
        }

        if (generated.exercises().size() != draftExercises.size()) {
            LOGGER.warn("Le nombre d'exercices IA differe du brouillon. generated={}, draft={}.", generated.exercises().size(), draftExercises.size());
        }

        return new AnthropicHomeworkGenerationService.GeneratedHomework(
                fallbackText(title, draft.title()),
                fallbackText(summary, draft.summary()),
                mergedExercises
        );
    }

    private AnthropicHomeworkGenerationService.GeneratedExercise validateExercise(
            AnthropicHomeworkGenerationService.GeneratedExercise generatedExercise,
            AnthropicHomeworkGenerationService.GeneratedExercise draftExercise
    ) {
        String axisTitle = matchesExpectedAxis(generatedExercise.axisTitle(), draftExercise.axisTitle())
                ? sanitizeHomeworkText(generatedExercise.axisTitle(), 120)
                : draftExercise.axisTitle();
        if (!axisTitle.equals(draftExercise.axisTitle())
                && !normalizeAxis(axisTitle).equals(normalizeAxis(draftExercise.axisTitle()))) {
            LOGGER.warn("Axe IA remplace par le brouillon. generatedAxis='{}', expectedAxis='{}'",
                    generatedExercise.axisTitle(), draftExercise.axisTitle());
        }
        String difficulty = normalizeDifficulty(fallbackText(generatedExercise.difficulty(), draftExercise.difficulty()));
        String type = normalizeExerciseType(fallbackText(generatedExercise.type(), draftExercise.type()),
                generatedExercise.options().isEmpty() ? draftExercise.options() : generatedExercise.options());
        String questionText = sanitizeHomeworkText(generatedExercise.questionText(), MAX_EXERCISE_TEXT_LENGTH);
        String expectedAnswer = sanitizeHomeworkText(generatedExercise.expectedAnswer(), MAX_EXERCISE_TEXT_LENGTH);
        String targetMistake = sanitizeHomeworkText(generatedExercise.targetMistake(), 240);
        List<String> options = type.equals("CHOICE")
                ? ensureExpectedOption(
                        sanitizeOptions(generatedExercise.options().isEmpty() ? draftExercise.options() : generatedExercise.options()),
                        fallbackText(expectedAnswer, draftExercise.expectedAnswer())
                )
                : List.of();

        return new AnthropicHomeworkGenerationService.GeneratedExercise(
                axisTitle,
                type,
                difficulty,
                fallbackText(questionText, draftExercise.questionText()),
                fallbackText(expectedAnswer, draftExercise.expectedAnswer()),
                fallbackText(targetMistake, draftExercise.targetMistake()),
                options
        );
    }

    private boolean matchesExpectedAxis(String candidateAxis, String expectedAxis) {
        String normalizedCandidate = normalizeAxis(candidateAxis);
        String normalizedExpected = normalizeAxis(expectedAxis);
        if (normalizedCandidate.isBlank()) {
            return false;
        }
        return normalizedCandidate.equals(normalizedExpected)
                || normalizedCandidate.contains(normalizedExpected)
                || normalizedExpected.contains(normalizedCandidate);
    }

    private String sanitizeHomeworkText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String sanitized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength).trim();
        }
        return sanitized;
    }

    private String fallbackText(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
    }

    private List<AxisNeed> determineAxisNeeds(Enfant enfant, boolean reviewMode) {
        Map<String, List<AxisAttemptStat>> historyByAxis = new LinkedHashMap<>();
        for (QuizAttempt attempt : quizAttemptRepo.findByEnfantIdOrderBySubmittedAtDesc(enfant.getId())) {
            Map<String, AxisAttemptStat> perAttempt = axisStatsForAttempt(attempt);
            for (AxisAttemptStat stat : perAttempt.values()) {
                historyByAxis.computeIfAbsent(normalizeAxis(stat.axisTitle()), ignored -> new ArrayList<>());
                List<AxisAttemptStat> history = historyByAxis.get(normalizeAxis(stat.axisTitle()));
                if (history.size() < AXIS_HISTORY_SIZE) {
                    history.add(stat);
                }
            }
        }

        List<AxisNeed> result = new ArrayList<>();
        for (List<AxisAttemptStat> history : historyByAxis.values()) {
            if (history.isEmpty()) {
                continue;
            }
            double average = history.stream().mapToDouble(AxisAttemptStat::scorePercent).average().orElse(0);
            AxisAttemptStat latest = history.get(0);
            boolean weak = average < WEAK_AXIS_THRESHOLD;
            boolean dueForReview = !weak && latest.submittedAt().toLocalDate().plusDays(REVIEW_DELAY_DAYS).isBefore(LocalDate.now());
            if (weak || (reviewMode && dueForReview)) {
                result.add(new AxisNeed(
                        latest.axisTitle(),
                        average,
                        weak ? "A consolider" : "Revision",
                        weak ? 3 : 2,
                        history.stream().flatMap(stat -> stat.mistakes().stream()).distinct().limit(3).toList()
                ));
            }
        }
        List<AxisNeed> limited = result.stream().limit(3).toList();
        LOGGER.info("Axes detectes pour l'enfant {}. reviewMode={}, axisNeeds={}",
                enfant.getId(), reviewMode, summarizeAxisNeeds(limited));
        return limited;
    }

    private List<AxisNeed> determineImmediateAxisNeeds(QuizAttempt sourceAttempt) {
        if (sourceAttempt == null) {
            return List.of();
        }

        List<AxisNeed> result = axisStatsForAttempt(sourceAttempt).values().stream()
                .sorted(Comparator.comparingDouble(AxisAttemptStat::scorePercent)
                        .thenComparing(AxisAttemptStat::axisTitle, String.CASE_INSENSITIVE_ORDER))
                .limit(IMMEDIATE_AXIS_LIMIT)
                .map(stat -> new AxisNeed(
                        stat.axisTitle(),
                        stat.scorePercent(),
                        stat.scorePercent() < WEAK_AXIS_THRESHOLD ? "A consolider" : "Revision",
                        stat.scorePercent() < 40 ? 3 : 2,
                        stat.mistakes()
                ))
                .toList();

        LOGGER.info("Axes immediats detectes pour attemptId={}. axisNeeds={}",
                sourceAttempt.getId(), summarizeAxisNeeds(result));
        return result;
    }

    private Map<String, AxisAttemptStat> axisStatsForAttempt(QuizAttempt attempt) {
        Map<String, List<QuizAnswer>> answersByAxis = attempt.getAnswers().stream()
                .collect(Collectors.groupingBy(answer -> normalizeAxis(answer.getQuestion().getAxis().getTitle()), LinkedHashMap::new, Collectors.toList()));

        Map<String, AxisAttemptStat> stats = new LinkedHashMap<>();
        for (List<QuizAnswer> axisAnswers : answersByAxis.values()) {
            if (axisAnswers.isEmpty()) {
                continue;
            }
            String axisTitle = axisAnswers.get(0).getQuestion().getAxis().getTitle();
            double average = axisAnswers.stream().mapToDouble(this::scoreQuizAnswer).average().orElse(0);
            List<String> mistakes = axisAnswers.stream()
                    .filter(answer -> scoreQuizAnswer(answer) < WEAK_AXIS_THRESHOLD)
                    .map(answer -> "Question: " + answer.getQuestion().getQuestionText() + " | Reponse eleve: " + answer.getAnswerText())
                    .limit(3)
                    .toList();
            stats.put(normalizeAxis(axisTitle), new AxisAttemptStat(axisTitle, average, attempt.getSubmittedAt(), mistakes));
        }
        return stats;
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
        return clampScore(20 + (overlap * 80));
    }

    private List<AxisNeed> deriveNeedsFromHomework(HomeworkAssignment assignment, double score, boolean reviewMode) {
        Map<String, List<HomeworkExercise>> byAxis = assignment.getExercises().stream()
                .collect(Collectors.groupingBy(exercise -> normalizeAxis(exercise.getAxisTitle()), LinkedHashMap::new, Collectors.toList()));
        List<AxisNeed> needs = new ArrayList<>();
        for (List<HomeworkExercise> exercises : byAxis.values()) {
            HomeworkExercise first = exercises.get(0);
            boolean stillWeak = score < REVIEW_AXIS_THRESHOLD;
            needs.add(new AxisNeed(
                    first.getAxisTitle(),
                    score,
                    reviewMode ? "Revision espacee" : stillWeak ? "Remediation" : "Revision",
                    reviewMode ? 2 : stillWeak ? Math.max(2, exercises.size()) : 2,
                    exercises.stream()
                            .map(HomeworkExercise::getTargetMistake)
                            .filter(value -> value != null && !value.isBlank())
                            .distinct()
                            .toList()
            ));
        }
        return needs;
    }

    private HomeworkDto toHomeworkDto(HomeworkAssignment assignment) {
        HomeworkAttempt latestAttempt = assignment.getAttempts().stream()
                .max(Comparator.comparing(HomeworkAttempt::getSubmittedAt))
                .orElse(null);
        String enfantName = assignment.getEnfant().getPrenom() + " " + assignment.getEnfant().getNom();
        return new HomeworkDto(
                assignment.getId(),
                assignment.getEnfant().getId(),
                enfantName,
                assignment.getAnimation() == null ? null : assignment.getAnimation().getId(),
                assignment.getAnimation() != null && assignment.getAnimation().getActivity() != null
                        ? assignment.getAnimation().getActivity().getActivyName()
                        : null,
                assignment.getTitle(),
                assignment.getSummary(),
                assignment.getStatus(),
                assignment.getCreatedAt(),
                assignment.getDueDate(),
                assignment.getExercises().stream()
                        .sorted(Comparator.comparingInt(HomeworkExercise::getPosition))
                        .map(exercise -> new HomeworkExerciseDto(
                                exercise.getId(),
                                exercise.getAxisTitle(),
                                exercise.getType(),
                                exercise.getDifficulty(),
                                exercise.getQuestionText(),
                                exercise.getExpectedAnswer(),
                                exercise.getTargetMistake(),
                                exercise.getPosition(),
                                exercise.getOptions()
                        ))
                        .toList(),
                latestAttempt == null ? null : latestAttempt.getScorePercent(),
                latestAttempt == null ? null : latestAttempt.getSubmittedAt()
        );
    }

    private HomeworkAttemptDto toAttemptDto(HomeworkAttempt attempt) {
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();
        return new HomeworkAttemptDto(
                attempt.getId(),
                attempt.getAssignment().getId(),
                attempt.getAssignment().getTitle(),
                attempt.getEnfant().getId(),
                enfantName,
                attempt.getSubmittedAt(),
                attempt.getElapsedSeconds(),
                attempt.getScorePercent(),
                attempt.getStatus(),
                attempt.getAnswers().stream()
                        .map(answer -> new TutorQuizAnswerDto(
                                answer.getExercise().getId(),
                                answer.getExercise().getAxisTitle(),
                                answer.getExercise().getDifficulty(),
                                answer.getExercise().getQuestionText(),
                                answer.getExercise().getExpectedAnswer(),
                                answer.getAnswerText(),
                                answer.getExercise().getOptions()
                        ))
                        .toList()
        );
    }

    private Integer normalizeElapsedSeconds(Integer elapsedSeconds) {
        return elapsedSeconds == null || elapsedSeconds < 0 ? null : elapsedSeconds;
    }

    private void notifyParentOfHomework(HomeworkAssignment assignment) {
        if (assignment == null || assignment.getEnfant() == null || assignment.getEnfant().getParent() == null) {
            return;
        }

        String enfantName = assignment.getEnfant().getPrenom() + " " + assignment.getEnfant().getNom();
        String activityName = assignment.getAnimation() != null && assignment.getAnimation().getActivity() != null
                ? assignment.getAnimation().getActivity().getActivyName()
                : null;

        parentNotificationService.createForParent(
                assignment.getEnfant().getParent(),
                "HOMEWORK",
                "Nouveau devoir disponible",
                "Un nouveau devoir \"" + assignment.getTitle() + "\" a ete genere pour " + enfantName + "."
        );

        if (assignment.getEnfant().getParent().getUser() != null
                && assignment.getEnfant().getParent().getUser().getEmail() != null
                && !assignment.getEnfant().getParent().getUser().getEmail().isBlank()) {
            emailService.sendHomeworkAvailableEmail(
                    assignment.getEnfant().getParent().getUser().getEmail(),
                    enfantName,
                    assignment.getTitle(),
                    activityName
            );
        }
    }

    private void notifyAnimateurOfHomeworkSubmission(HomeworkAttempt attempt) {
        if (attempt == null
                || attempt.getAssignment() == null
                || attempt.getAssignment().getAnimateur() == null
                || attempt.getAssignment().getAnimateur().getUser() == null) {
            return;
        }

        String animateurEmail = attempt.getAssignment().getAnimateur().getUser().getEmail();
        if (animateurEmail == null || animateurEmail.isBlank()) {
            return;
        }

        String animateurName = attempt.getAssignment().getAnimateur().getPrenom()
                + " "
                + attempt.getAssignment().getAnimateur().getNom();
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();
        String activityName = attempt.getAssignment().getAnimation() != null
                && attempt.getAssignment().getAnimation().getActivity() != null
                ? attempt.getAssignment().getAnimation().getActivity().getActivyName()
                : null;

        emailService.sendHomeworkSubmissionEmail(
                animateurEmail,
                animateurName.trim(),
                enfantName,
                attempt.getAssignment().getTitle(),
                activityName,
                attempt.getScorePercent()
        );
        animateurNotificationService.createForAnimateur(
                attempt.getAssignment().getAnimateur(),
                "HOMEWORK_SUBMISSION",
                "Nouvelle soumission de devoir",
                enfantName + " a soumis le devoir \"" + attempt.getAssignment().getTitle() + "\"."
        );
    }

    private AnimateurHomeworkStudentRowDto toAnimateurStudentRow(List<HomeworkAssignment> assignments, List<HomeworkAttempt> attempts) {
        HomeworkAssignment first = assignments.get(0);
        Long enfantId = first.getEnfant().getId();
        String enfantName = first.getEnfant().getPrenom() + " " + first.getEnfant().getNom();
        long submittedCount = assignments.stream().filter(assignment -> !assignment.getAttempts().isEmpty()).count();
        Double averageScore = attempts.stream()
                .filter(attempt -> attempt.getEnfant().getId().equals(enfantId))
                .map(HomeworkAttempt::getScorePercent)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
        LocalDateTime latestSubmittedAt = attempts.stream()
                .filter(attempt -> attempt.getEnfant().getId().equals(enfantId))
                .map(HomeworkAttempt::getSubmittedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new AnimateurHomeworkStudentRowDto(
                enfantId,
                enfantName,
                assignments.size(),
                submittedCount,
                Math.max(0, assignments.size() - submittedCount),
                Double.isNaN(averageScore) ? null : averageScore,
                latestSubmittedAt
        );
    }

    private Set<String> keywords(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(value == null ? "" : value.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = stripAccents(matcher.group()).replace("'", "").replace("-", "");
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private boolean isTutoringAssignment(HomeworkAssignment assignment) {
        return assignment != null
                && assignment.getAnimation() != null
                && assignment.getAnimation().getActivity() != null
                && assignment.getAnimation().getActivity().getType() == typeActivity.TUTORAT;
    }

    private String normalizeAxis(String title) {
        return stripAccents(title == null ? "" : title)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private double clampScore(double score) {
        return Math.max(0, Math.min(100, score));
    }

    private String normalizeDifficulty(String difficulty) {
        String normalized = difficulty == null ? "" : stripAccents(difficulty).toUpperCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "FACILE", "MOYEN", "DIFFICILE" -> normalized;
            default -> "MOYEN";
        };
    }

    private String normalizeExerciseType(String type, List<String> options) {
        String normalized = type == null ? "" : stripAccents(type).toUpperCase(Locale.ROOT).trim();
        if ("CHOICE".equals(normalized) && options != null && options.size() >= 2) {
            return "CHOICE";
        }
        return "OPEN";
    }

    private List<String> sanitizeOptions(List<String> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> sanitizeHomeworkText(option, 240))
                .filter(option -> !option.isBlank())
                .distinct()
                .limit(6)
                .toList();
    }

    private List<String> ensureExpectedOption(List<String> options, String expectedAnswer) {
        if (expectedAnswer == null || expectedAnswer.isBlank()) {
            return options;
        }
        List<String> normalized = new ArrayList<>(options);
        boolean containsExpected = normalized.stream()
                .anyMatch(option -> normalizeComparableText(option).equals(normalizeComparableText(expectedAnswer)));
        if (!containsExpected) {
            normalized.add(0, sanitizeHomeworkText(expectedAnswer, 240));
        }
        return normalized.stream().distinct().limit(6).toList();
    }

    private String normalizeComparableText(String value) {
        return stripAccents(value == null ? "" : value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }

    private String summarizeAxisNeeds(List<AxisNeed> axisNeeds) {
        return axisNeeds.stream()
                .map(axis -> axis.axisTitle() + "(score=" + Math.round(axis.averageScore())
                        + ",difficulty=" + axis.targetDifficulty()
                        + ",count=" + axis.exerciseCount() + ")")
                .collect(Collectors.joining(", "));
    }

    public record AxisNeed(String axisTitle,
                           double averageScore,
                           String targetDifficulty,
                           int exerciseCount,
                           List<String> mistakes) {
    }

    private record AxisAttemptStat(String axisTitle,
                                   double scorePercent,
                                   LocalDateTime submittedAt,
                                   List<String> mistakes) {
    }
}
