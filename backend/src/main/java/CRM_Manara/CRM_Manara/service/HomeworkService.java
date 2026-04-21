package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HomeworkService {

    private static final double WEAK_AXIS_THRESHOLD = 66.0;
    private static final int AXIS_HISTORY_SIZE = 3;
    private static final int REVIEW_DELAY_DAYS = 10;
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

    public HomeworkService(HomeworkAssignmentRepo homeworkAssignmentRepo,
                           HomeworkAttemptRepo homeworkAttemptRepo,
                           QuizAttemptRepo quizAttemptRepo,
                           AnthropicHomeworkGenerationService anthropicHomeworkGenerationService) {
        this.homeworkAssignmentRepo = homeworkAssignmentRepo;
        this.homeworkAttemptRepo = homeworkAttemptRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.anthropicHomeworkGenerationService = anthropicHomeworkGenerationService;
    }

    @Transactional
    public void createAutomaticHomeworkFromQuizAttempt(QuizAttempt sourceAttempt) {
        if (sourceAttempt == null
                || sourceAttempt.getId() == null
                || sourceAttempt.getEnfant() == null
                || sourceAttempt.getQuiz() == null
                || homeworkAssignmentRepo.existsBySourceAttemptIdAndEnfantId(sourceAttempt.getId(), sourceAttempt.getEnfant().getId())) {
            return;
        }

        List<AxisNeed> weakAxes = determineAxisNeeds(sourceAttempt.getEnfant(), false);
        if (weakAxes.isEmpty()) {
            return;
        }

        HomeworkAssignment assignment = buildAssignment(
                sourceAttempt.getEnfant(),
                sourceAttempt.getQuiz(),
                sourceAttempt,
                weakAxes,
                false
        );
        homeworkAssignmentRepo.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<HomeworkDto> listAssignmentsForParent(String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        return homeworkAssignmentRepo.findByEnfantParentIdOrderByCreatedAtDesc(parentId).stream()
                .map(this::toHomeworkDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeworkDto getAssignmentForParent(Long assignmentId, String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        HomeworkAssignment assignment = homeworkAssignmentRepo.findByIdAndEnfantParentId(assignmentId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devoir introuvable."));
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

        double score = scoreHomeworkAttempt(attempt);
        attempt.markScored(score, "SCORED_LOCAL");
        assignment.addAttempt(attempt);
        assignment.markCompleted();
        homeworkAttemptRepo.save(attempt);

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

        return toAttemptDto(attempt);
    }

    @Transactional(readOnly = true)
    public List<HomeworkAttemptDto> listAttemptsForParent(String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        return homeworkAttemptRepo.findByEnfantParentIdOrderBySubmittedAtDesc(parentId).stream()
                .map(this::toAttemptDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeworkAttemptDto getAttemptDetailForParent(Long attemptId, String parentEmail, parentService parentService) {
        Long parentId = parentService.getParentByEmail(parentEmail).getId();
        HomeworkAttempt attempt = homeworkAttemptRepo.findByIdAndEnfantParentId(attemptId, parentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Soumission du devoir introuvable."));
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
        AnthropicHomeworkGenerationService.GeneratedHomework generatedHomework = anthropicHomeworkGenerationService
                .generate(activityName, childName, axisNeeds, reviewMode)
                .orElseGet(() -> buildLocalHomework(activityName, axisNeeds, reviewMode));

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
                    normalizeDifficulty(exercise.difficulty()),
                    exercise.questionText(),
                    exercise.expectedAnswer(),
                    exercise.targetMistake(),
                    i + 1
            ));
        }
        return assignment;
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
        return result.stream().limit(3).toList();
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

    private double scoreHomeworkAttempt(HomeworkAttempt attempt) {
        if (attempt.getAnswers().isEmpty()) {
            return 0;
        }
        return clampScore(attempt.getAnswers().stream()
                .mapToDouble(this::scoreHomeworkAnswer)
                .average()
                .orElse(0));
    }

    private double scoreHomeworkAnswer(HomeworkAnswer answer) {
        Set<String> expectedTokens = keywords(answer.getExercise().getExpectedAnswer());
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
            needs.add(new AxisNeed(
                    first.getAxisTitle(),
                    score,
                    reviewMode ? "Revision espacee" : "Remediation",
                    reviewMode ? 2 : Math.max(2, exercises.size()),
                    exercises.stream()
                            .map(HomeworkExercise::getTargetMistake)
                            .filter(value -> value != null && !value.isBlank())
                            .distinct()
                            .toList()
            ));
        }
        return needs;
    }

    private AnthropicHomeworkGenerationService.GeneratedHomework buildLocalHomework(String activityName,
                                                                                    List<AxisNeed> axisNeeds,
                                                                                    boolean reviewMode) {
        List<AnthropicHomeworkGenerationService.GeneratedExercise> exercises = new ArrayList<>();
        int position = 1;
        for (AxisNeed axis : axisNeeds) {
            int count = Math.max(2, axis.exerciseCount());
            for (int index = 0; index < count; index++) {
                String difficulty = index == 0 ? "FACILE" : index == count - 1 ? "DIFFICILE" : "MOYEN";
                String mistake = axis.mistakes().isEmpty() ? "" : axis.mistakes().get(Math.min(index, axis.mistakes().size() - 1));
                exercises.add(new AnthropicHomeworkGenerationService.GeneratedExercise(
                        axis.axisTitle(),
                        difficulty,
                        buildLocalQuestion(axis.axisTitle(), difficulty, reviewMode, mistake, position),
                        buildLocalExpectedAnswer(axis.axisTitle(), difficulty, reviewMode),
                        mistake
                ));
                position++;
            }
        }
        return new AnthropicHomeworkGenerationService.GeneratedHomework(
                reviewMode ? "Devoir de revision espacee" : "Devoir personnalise",
                reviewMode
                        ? "Revision de retention sur les axes maitrises pour verifier la memorisation."
                        : "Serie d'exercices cibles sur les axes faibles observes pendant les quiz recents.",
                exercises
        );
    }

    private String buildLocalQuestion(String axisTitle, String difficulty, boolean reviewMode, String mistake, int position) {
        String prefix = reviewMode ? "Revision" : "Entrainement";
        String note = mistake == null || mistake.isBlank() ? "" : " Evite l'erreur suivante: " + mistake + ".";
        return prefix + " " + difficulty.toLowerCase(Locale.ROOT) + " #" + position + " sur \"" + axisTitle
                + "\". Resous un exercice similaire a ceux vus en classe et explique briievement ta demarche." + note;
    }

    private String buildLocalExpectedAnswer(String axisTitle, String difficulty, boolean reviewMode) {
        return "La reponse doit traiter correctement l'axe \"" + axisTitle + "\" avec une justification adaptee au niveau "
                + difficulty.toLowerCase(Locale.ROOT) + (reviewMode ? " dans un contexte de revision." : ".");
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
                                exercise.getDifficulty(),
                                exercise.getQuestionText(),
                                exercise.getExpectedAnswer(),
                                exercise.getTargetMistake(),
                                exercise.getPosition()
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
                                answer.getAnswerText()
                        ))
                        .toList()
        );
    }

    private Integer normalizeElapsedSeconds(Integer elapsedSeconds) {
        return elapsedSeconds == null || elapsedSeconds < 0 ? null : elapsedSeconds;
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
