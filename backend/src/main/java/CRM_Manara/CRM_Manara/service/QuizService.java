package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAssignment;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAnswer;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAxis;
import CRM_Manara.CRM_Manara.Model.Entity.QuizQuestion;
import CRM_Manara.CRM_Manara.Repository.QuizAttemptRepo;
import CRM_Manara.CRM_Manara.Repository.QuizRepo;
import CRM_Manara.CRM_Manara.Repository.HomeworkAssignmentRepo;
import CRM_Manara.CRM_Manara.service.AnthropicQuizGenerationService.GeneratedAxis;
import CRM_Manara.CRM_Manara.service.AnthropicQuizGenerationService.GeneratedQuestion;
import CRM_Manara.CRM_Manara.service.AnthropicQuizGenerationService.GeneratedQuiz;
import CRM_Manara.CRM_Manara.dto.QuizAxisDto;
import CRM_Manara.CRM_Manara.dto.QuizCreateRequestDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.QuizQuestionDto;
import CRM_Manara.CRM_Manara.dto.TutorAxisProgressDto;
import CRM_Manara.CRM_Manara.dto.TutorDashboardDto;
import CRM_Manara.CRM_Manara.dto.TutorQuizAnswerDto;
import CRM_Manara.CRM_Manara.dto.TutorQuizSubmissionDto;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuizService.class);

    private static final int MIN_NOTES_LENGTH = 20;
    private static final int MIN_AXES = 3;
    private static final int MAX_AXES = 7;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}");
    private static final Pattern FORMULA_COMPLEXITY_PATTERN = Pattern.compile("[=^()+\\-*/]|\\b[fg]\\s*\\(x\\)|\\bx\\b");
    private static final Pattern FORMULA_PATTERN = Pattern.compile("[A-Za-z]\\s*\\([^)]*\\)\\s*=\\s*[^\\r\\n,;.!?]+|[A-Za-z0-9²³]+\\s*=\\s*[^\\r\\n,;.!?]+");
    private static final Pattern DERIVATIVE_POWER_PATTERN = Pattern.compile(
            "(?iu)d[ée]riv[ée]e\\s+de\\s+([+-]?\\d*)\\s*x(?:\\s*\\^\\s*(\\d+)|([²³]))\\s*=\\s*([^\\r\\n,;.!?]+)"
    );
    private static final List<String> ANGLES = List.of(
            "reconnaissance",
            "application",
            "piege",
            "transfert",
            "justification"
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "avec", "dans", "des", "les", "une", "pour", "que", "qui", "sur", "aux", "par", "plus",
            "moins", "comme", "cours", "seance", "exercice", "exercices", "etudiant", "etudiante",
            "nous", "vous", "ils", "elles", "leur", "leurs", "fait", "faire", "etre", "avoir",
            "est", "sont", "ont", "aussi", "apres", "avant", "pendant", "entre", "sans", "tres",
            "bien", "mal", "donc", "mais", "car", "tout", "tous", "toutes", "cette", "cela",
            "celui", "celle", "ces", "ses", "mes", "tes", "nos", "vos"
    );

    private final QuizRepo quizRepo;
    private final QuizAttemptRepo quizAttemptRepo;
    private final HomeworkAssignmentRepo homeworkAssignmentRepo;
    private final AnimateurService animateurService;
    private final AnthropicQuizGenerationService anthropicQuizGenerationService;
    private final HomeworkService homeworkService;

    public QuizService(QuizRepo quizRepo,
                       QuizAttemptRepo quizAttemptRepo,
                       HomeworkAssignmentRepo homeworkAssignmentRepo,
                       AnimateurService animateurService,
                       AnthropicQuizGenerationService anthropicQuizGenerationService,
                       HomeworkService homeworkService) {
        this.quizRepo = quizRepo;
        this.quizAttemptRepo = quizAttemptRepo;
        this.homeworkAssignmentRepo = homeworkAssignmentRepo;
        this.animateurService = animateurService;
        this.anthropicQuizGenerationService = anthropicQuizGenerationService;
        this.homeworkService = homeworkService;
    }

    @Transactional
    public QuizDto createForAnimateur(String animateurEmail, QuizCreateRequestDto request) {
        if (request == null || request.sourceNotes() == null
                || request.sourceNotes().trim().length() < MIN_NOTES_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les notes de seance doivent contenir au moins " + MIN_NOTES_LENGTH + " caracteres.");
        }

        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Animation animation = null;
        if (request.animationId() != null) {
            animation = animateurService.getAnimationForAnimateur(request.animationId(), animateurEmail);
            if (!isTutoringAnimation(animation)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Les quiz sont reserves aux animations de tutorat.");
            }
        }
        if (animation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un quiz doit etre lie a une animation de tutorat.");
        }

        String notes = request.sourceNotes().trim();
        String title = cleanTitle(request.title(), animation);
        Quiz quiz = new Quiz(animateur, animation, title, notes);
        String activityName = animation != null && animation.getActivity() != null
                ? animation.getActivity().getActivyName()
                : null;

        Optional<GeneratedQuiz> generatedQuiz = anthropicQuizGenerationService.generateQuiz(title, notes, activityName);
        if (generatedQuiz.isPresent()) {
            addGeneratedAxes(quiz, generatedQuiz.get());
        } else {
            addLocalAxes(quiz, notes);
        }

        return toDto(quizRepo.save(quiz));
    }

    @Transactional(readOnly = true)
    public List<QuizDto> listForAnimateur(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        return quizRepo.findByAnimateurIdOrderByCreatedAtDesc(animateur.getId()).stream()
                .filter(quiz -> isTutoringAnimation(quiz.getAnimation()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDto getForAnimateur(Long quizId, String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Quiz quiz = quizRepo.findByIdAndAnimateurId(quizId, animateur.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable."));
        if (!isTutoringAnimation(quiz.getAnimation())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable.");
        }
        return toDto(quiz);
    }

    @Transactional(readOnly = true)
    public List<TutorQuizSubmissionDto> listSubmissionsForAnimateur(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        return quizAttemptRepo.findByQuizAnimateurIdOrderBySubmittedAtDesc(animateur.getId()).stream()
                .filter(attempt -> isTutoringAnimation(attempt.getQuiz() == null ? null : attempt.getQuiz().getAnimation()))
                .map(this::toSubmissionDto)
                .toList();
    }

    @Transactional
    public void deleteForAnimateur(Long quizId, String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Quiz quiz = quizRepo.findByIdAndAnimateurId(quizId, animateur.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable."));
        if (!isTutoringAnimation(quiz.getAnimation())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable.");
        }
        List<HomeworkAssignment> linkedAssignments = homeworkAssignmentRepo.findBySourceQuizId(quizId);
        if (!linkedAssignments.isEmpty()) {
            LOGGER.info("Suppression du quiz {}: suppression prealable de {} devoir(s) lies.", quizId, linkedAssignments.size());
            homeworkAssignmentRepo.deleteAll(linkedAssignments);
            homeworkAssignmentRepo.flush();
        }
        LOGGER.info("Suppression du quiz {} par l'animateur {}.", quizId, animateur.getId());
        quizRepo.delete(quiz);
    }

    @Transactional
    public ActionResponseDto backfillMissingHomeworksForAnimateur(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        List<QuizAttempt> attempts = quizAttemptRepo.findByQuizAnimateurIdOrderBySubmittedAtDesc(animateur.getId()).stream()
                .filter(attempt -> isTutoringAnimation(attempt.getQuiz() == null ? null : attempt.getQuiz().getAnimation()))
                .toList();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (QuizAttempt attempt : attempts) {
            try {
                boolean generated = homeworkService.createAutomaticHomeworkFromQuizAttempt(attempt);
                if (generated) {
                    created++;
                } else {
                    skipped++;
                }
            } catch (Exception exception) {
                failed++;
                LOGGER.error("Backfill devoir echoue pour attemptId={} quizId={} enfantId={}.",
                        attempt.getId(),
                        attempt.getQuiz() == null ? null : attempt.getQuiz().getId(),
                        attempt.getEnfant() == null ? null : attempt.getEnfant().getId(),
                        exception);
            }
        }

        String message = "Backfill termine: " + created + " devoir(s) crees, "
                + skipped + " ignore(s), " + failed + " echec(s).";
        LOGGER.info("Backfill des devoirs manquants termine pour animateurId={}. {}", animateur.getId(), message);
        return new ActionResponseDto(failed == 0, message, (long) created);
    }

    @Transactional(readOnly = true)
    public TutorDashboardDto getTutorDashboard(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        List<Quiz> quizzes = quizRepo.findByAnimateurIdOrderByCreatedAtDesc(animateur.getId()).stream()
                .filter(quiz -> isTutoringAnimation(quiz.getAnimation()))
                .toList();
        List<QuizAttempt> attempts = quizAttemptRepo.findByQuizAnimateurIdOrderBySubmittedAtDesc(animateur.getId()).stream()
                .filter(attempt -> isTutoringAnimation(attempt.getQuiz() == null ? null : attempt.getQuiz().getAnimation()))
                .toList();
        List<Inscription> inscriptions = animateurService.getInscriptionsForAnimateur(animateur.getId());
        Map<Long, Enfant> enrolledChildren = uniqueChildren(inscriptions);
        int responderCount = (int) attempts.stream()
                .map(attempt -> attempt.getEnfant().getId())
                .filter(id -> id != null)
                .distinct()
                .count();
        Double globalScore = averageScore(attempts);
        Integer averageResponseTime = averageResponseTime(attempts);
        Double participationPercent = enrolledChildren.isEmpty()
                ? null
                : (responderCount * 100.0) / enrolledChildren.size();
        Double averageStudentAge = averageStudentAge(enrolledChildren.values());
        Map<String, AxisStats> statsByAxis = new LinkedHashMap<>();

        for (Quiz quiz : quizzes) {
            for (QuizAxis axis : quiz.getAxes()) {
                String key = normalizeAxis(axis.getTitle());
                AxisStats stats = statsByAxis.computeIfAbsent(key, ignored -> new AxisStats(axis.getTitle()));
                stats.registerQuiz(quiz, axis);
            }
        }
        for (QuizAttempt attempt : attempts) {
            for (QuizAxis axis : attempt.getQuiz().getAxes()) {
                String key = normalizeAxis(axis.getTitle());
                AxisStats stats = statsByAxis.computeIfAbsent(key, ignored -> new AxisStats(axis.getTitle()));
                stats.registerAttempt(attempt);
            }
        }

        List<TutorAxisProgressDto> axes = statsByAxis.values().stream()
                .sorted(Comparator
                        .comparing(AxisStats::getQuizCount).reversed()
                        .thenComparing(AxisStats::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(AxisStats::toDto)
                .toList();
        List<TutorAxisProgressDto> persistentAxes = axes.stream()
                .filter(axis -> axis.quizCount() >= 2)
                .toList();

        int questionCount = axes.stream().mapToInt(TutorAxisProgressDto::questionCount).sum();
        String suggestion = buildNextSessionSuggestion(quizzes, persistentAxes, axes, attempts, enrolledChildren.size(), responderCount);

        return new TutorDashboardDto(
                enrolledChildren.size(),
                responderCount,
                attempts.size(),
                participationPercent,
                averageStudentAge,
                quizzes.size(),
                axes.size(),
                questionCount,
                globalScore,
                averageResponseTime,
                buildProgressStatus(quizzes, attempts, enrolledChildren.size(), responderCount),
                suggestion,
                quizzes.isEmpty() ? null : quizzes.get(0).getCreatedAt(),
                axes,
                persistentAxes
        );
    }

    private Map<Long, Enfant> uniqueChildren(List<Inscription> inscriptions) {
        Map<Long, Enfant> children = new LinkedHashMap<>();
        for (Inscription inscription : inscriptions) {
            if (inscription.getEnfant() != null && inscription.getEnfant().getId() != null) {
                children.putIfAbsent(inscription.getEnfant().getId(), inscription.getEnfant());
            }
        }
        return children;
    }

    private Double averageScore(List<QuizAttempt> attempts) {
        List<Double> scores = attempts.stream()
                .map(QuizAttempt::getScorePercent)
                .filter(score -> score != null)
                .toList();
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private Integer averageResponseTime(List<QuizAttempt> attempts) {
        List<Integer> times = attempts.stream()
                .map(QuizAttempt::getElapsedSeconds)
                .filter(seconds -> seconds != null && seconds >= 0)
                .toList();
        if (times.isEmpty()) {
            return null;
        }
        return (int) Math.round(times.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private Double averageStudentAge(Iterable<Enfant> children) {
        LocalDate today = LocalDate.now();
        int count = 0;
        int totalAge = 0;
        for (Enfant child : children) {
            LocalDate birthDate = toLocalDate(child.getDate_de_naissance());
            if (birthDate != null) {
                totalAge += java.time.Period.between(birthDate, today).getYears();
                count++;
            }
        }
        return count == 0 ? null : (double) totalAge / count;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String buildProgressStatus(List<Quiz> quizzes, List<QuizAttempt> attempts, int enrolledCount, int responderCount) {
        if (enrolledCount == 0) {
            return "Aucun enfant inscrit";
        }
        if (quizzes.isEmpty()) {
            return "Aucun quiz cree";
        }
        if (attempts.isEmpty()) {
            return "En attente de reponses";
        }
        return responderCount + "/" + enrolledCount + " enfant(s) ont repondu";
    }

    private String cleanTitle(String rawTitle, Animation animation) {
        if (rawTitle != null && !rawTitle.trim().isBlank()) {
            return rawTitle.trim();
        }
        if (animation != null && animation.getActivity() != null) {
            return "Quiz - " + animation.getActivity().getActivyName();
        }
        return "Quiz de micro-diagnostic";
    }

    private void addGeneratedAxes(Quiz quiz, GeneratedQuiz generatedQuiz) {
        for (int i = 0; i < generatedQuiz.axes().size(); i++) {
            GeneratedAxis generatedAxis = generatedQuiz.axes().get(i);
            QuizAxis axis = new QuizAxis(generatedAxis.title(), generatedAxis.summary(), i + 1);
            for (int j = 0; j < generatedAxis.questions().size(); j++) {
                GeneratedQuestion generatedQuestion = generatedAxis.questions().get(j);
                axis.addQuestion(new QuizQuestion(
                        generatedQuestion.angle(),
                        resolveQuestionType(generatedQuestion.expectedAnswer(), generatedQuestion.type(), generatedQuestion.options()),
                        generatedQuestion.questionText(),
                        generatedQuestion.expectedAnswer(),
                        j + 1,
                        resolveQuestionOptions(generatedQuestion.expectedAnswer(), generatedQuestion.options())
                ));
            }
            quiz.addAxis(axis);
        }
    }

    private void addLocalAxes(Quiz quiz, String notes) {
        List<LocalFocus> axes = extractLocalFocuses(notes);

        for (int i = 0; i < axes.size(); i++) {
            LocalFocus focus = axes.get(i);
            QuizAxis axis = new QuizAxis(focus.title(), buildSummary(focus, notes), i + 1);
            for (int j = 0; j < ANGLES.size(); j++) {
                axis.addQuestion(buildQuestion(focus, ANGLES.get(j), j + 1));
            }
            quiz.addAxis(axis);
        }
    }

    private List<LocalFocus> extractLocalFocuses(String notes) {
        LinkedHashMap<String, LocalFocus> focuses = new LinkedHashMap<>();

        findDerivativeExample(notes).ifPresent(example ->
                addFocus(focuses, new LocalFocus(
                        "Derivees de puissances",
                        "Les notes indiquent: derivee de " + example.originalExpression() + " = " + example.derivative() + ".",
                        example.originalExpression() + " = " + example.derivative(),
                        example
                ))
        );

        for (String explicitAxis : extractExplicitAxes(notes)) {
            String evidence = findSentenceForAxis(explicitAxis, notes);
            addFocus(focuses, new LocalFocus(explicitAxis, evidence, extractFormula(evidence), null));
        }

        for (String keywordAxis : extractKeywordAxes(notes)) {
            String evidence = findSentenceForAxis(keywordAxis, notes);
            addFocus(focuses, new LocalFocus(keywordAxis, evidence, extractFormula(evidence), null));
        }

        List<String> sentences = meaningfulSentences(notes);
        for (String sentence : sentences) {
            String title = titleFromSentence(sentence);
            addFocus(focuses, new LocalFocus(title, sentence, extractFormula(sentence), null));
            if (focuses.size() >= MIN_AXES) {
                break;
            }
        }

        if (focuses.isEmpty()) {
            String fallbackTitle = titleFromSentence(notes);
            addFocus(focuses, new LocalFocus(fallbackTitle, notes.trim(), extractFormula(notes), null));
        }

        return focuses.values().stream()
                .limit(MAX_AXES)
                .toList();
    }

    private void addFocus(Map<String, LocalFocus> focuses, LocalFocus focus) {
        if (focus.title() == null || focus.title().isBlank()) {
            return;
        }
        String key = normalizeAxis(focus.title());
        if (!key.isBlank()) {
            focuses.putIfAbsent(key, focus);
        }
    }

    private List<String> extractExplicitAxes(String notes) {
        List<String> axes = new ArrayList<>();
        String[] lines = notes.split("\\R+");
        for (String line : lines) {
            String cleaned = line.replaceFirst("^\\s*[-*\\d.)]+\\s*", "").trim();
            if (cleaned.length() < 4 || cleaned.length() > 80) {
                continue;
            }
            if (cleaned.contains(":")) {
                cleaned = cleaned.substring(0, cleaned.indexOf(':')).trim();
            }
            if (cleaned.split("\\s+").length <= 8) {
                axes.add(capitalize(cleaned));
            }
            if (axes.size() >= MIN_AXES) {
                break;
            }
        }
        return axes;
    }

    private List<String> extractKeywordAxes(String notes) {
        Map<String, Integer> counts = new HashMap<>();
        Matcher matcher = WORD_PATTERN.matcher(notes.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = stripAccents(matcher.group()).replace("'", "").replace("-", "");
            if (word.length() < 4 || STOP_WORDS.contains(word)) {
                continue;
            }
            counts.merge(word, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .filter(new HashSet<>()::add)
                .limit(MAX_AXES)
                .map(word -> "Notion: " + capitalize(word))
                .toList();
    }

    private String buildSummary(LocalFocus focus, String notes) {
        String sentence = focus.evidence();
        if (sentence == null || sentence.isBlank()) {
            sentence = findSentenceForAxis(focus.title(), notes);
        }
        if (sentence.isBlank()) {
            return "Axe extrait des notes de seance: " + focus.title() + ".";
        }
        return sentence;
    }

    private String findSentenceForAxis(String axisTitle, String notes) {
        String normalizedAxis = stripAccents(axisTitle.toLowerCase(Locale.ROOT));
        for (String sentence : notes.split("(?<=[.!?])\\s+|\\R+")) {
            String cleaned = sentence.trim();
            if (cleaned.length() > 12
                    && stripAccents(cleaned.toLowerCase(Locale.ROOT)).contains(normalizedAxis.replace("axe ", ""))) {
                return cleaned.length() > 220 ? cleaned.substring(0, 220).trim() + "..." : cleaned;
            }
        }
        return "";
    }

    private Optional<DerivativeExample> findDerivativeExample(String notes) {
        Matcher matcher = DERIVATIVE_POWER_PATTERN.matcher(notes);
        if (!matcher.find()) {
            return Optional.empty();
        }

        int coefficient = parseCoefficient(matcher.group(1));
        int exponent = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : superscriptToInt(matcher.group(3));
        int similarCoefficient = coefficient == 1 ? 2 : coefficient + 1;
        String similarExpression = formatTerm(similarCoefficient, exponent);
        String similarDerivative = formatTerm(similarCoefficient * exponent, exponent - 1);

        return Optional.of(new DerivativeExample(
                formatTerm(coefficient, exponent),
                matcher.group(4).trim(),
                similarExpression,
                similarDerivative
        ));
    }

    private int parseCoefficient(String rawCoefficient) {
        if (rawCoefficient == null || rawCoefficient.isBlank() || "+".equals(rawCoefficient)) {
            return 1;
        }
        if ("-".equals(rawCoefficient)) {
            return -1;
        }
        return Integer.parseInt(rawCoefficient.trim());
    }

    private int superscriptToInt(String value) {
        if ("²".equals(value)) {
            return 2;
        }
        if ("³".equals(value)) {
            return 3;
        }
        return 1;
    }

    private String formatTerm(int coefficient, int exponent) {
        if (exponent <= 0) {
            return String.valueOf(coefficient);
        }
        String variable = exponent == 1 ? "x" : "x^" + exponent;
        if (coefficient == 1) {
            return variable;
        }
        if (coefficient == -1) {
            return "-" + variable;
        }
        return coefficient + variable;
    }

    private String extractFormula(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = FORMULA_PATTERN.matcher(text);
        return matcher.find() ? matcher.group().trim() : "";
    }

    private List<String> meaningfulSentences(String notes) {
        return List.of(notes.split("(?<=[.!?])\\s+|\\R+")).stream()
                .map(String::trim)
                .filter(sentence -> sentence.length() >= 12)
                .limit(MAX_AXES)
                .toList();
    }

    private String titleFromSentence(String sentence) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(sentence.toLowerCase(Locale.ROOT));
        while (matcher.find() && words.size() < 5) {
            String word = stripAccents(matcher.group()).replace("'", "").replace("-", "");
            if (word.length() >= 4 && !STOP_WORDS.contains(word)) {
                words.add(capitalize(word));
            }
        }
        if (words.isEmpty()) {
            return "Notion tiree des notes";
        }
        return String.join(" ", words);
    }

    private QuizQuestion buildQuestion(LocalFocus focus, String angle, int position) {
        if (focus.derivativeExample() != null) {
            return buildDerivativeQuestion(focus.derivativeExample(), angle, position);
        }

        String axisTitle = focus.title();
        String evidence = focus.evidence() == null || focus.evidence().isBlank()
                ? axisTitle
                : shorten(focus.evidence(), 180);
        String formula = focus.formula() == null || focus.formula().isBlank()
                ? ""
                : " en utilisant la relation vue \"" + focus.formula() + "\"";

        return switch (angle) {
            case "reconnaissance" -> buildQuestionWithOptions(
                    "Reconnaissance",
                    "Dans l'extrait des notes \"" + evidence + "\", quel concept precis faut-il reconnaitre?",
                    "Le concept attendu est " + axisTitle + ", appuye par l'extrait des notes.",
                    position
            );
            case "application" -> buildQuestionWithOptions(
                    "Application",
                    "Resous un exemple similaire a celui des notes sur " + axisTitle + formula + ".",
                    "La reponse doit appliquer " + axisTitle + " au meme type de situation que dans les notes: " + evidence + ".",
                    position
            );
            case "piege" -> buildQuestionWithOptions(
                    "Piege",
                    "Quelle erreur frequente pourrait arriver avec " + axisTitle + " dans ce contexte: \"" + evidence + "\"?",
                    "La reponse doit nommer une confusion liee a cet extrait et donner une verification concrete.",
                    position
            );
            case "transfert" -> buildQuestionWithOptions(
                    "Transfert",
                    "Propose un nouveau cas ou la meme idee que \"" + evidence + "\" s'applique.",
                    "La reponse doit transferer " + axisTitle + " dans un contexte nouveau sans changer le principe.",
                    position
            );
            default -> buildQuestionWithOptions(
                    "Justification",
                    "Pourquoi la demarche utilisee pour " + axisTitle + " fonctionne-t-elle dans les notes?",
                    "La justification doit citer la regle ou le lien logique montre dans les notes: " + evidence + ".",
                    position
            );
        };
    }

    private QuizQuestion buildDerivativeQuestion(DerivativeExample example, String angle, int position) {
        return switch (angle) {
            case "reconnaissance" -> buildQuestionWithOptions(
                    "Reconnaissance",
                    "Dans les notes, quelle derivee est associee a " + example.originalExpression() + "?",
                    "La derivee attendue est " + example.derivative() + ".",
                    position
            );
            case "application" -> buildQuestionWithOptions(
                    "Application",
                    "Calcule la derivee de f(x) = " + example.similarExpression() + ".",
                    "f'(x) = " + example.similarDerivative() + ", avec la regle de puissance.",
                    position
            );
            case "piege" -> buildQuestionWithOptions(
                    "Piege",
                    "Pour f(x) = " + example.similarExpression() + ", quelle erreur d'exposant faut-il eviter en derivant?",
                    "Il ne faut pas garder le meme exposant: on multiplie par l'exposant puis on diminue l'exposant de 1, donc f'(x) = "
                            + example.similarDerivative() + ".",
                    position
            );
            case "transfert" -> buildQuestionWithOptions(
                    "Transfert",
                    "Si g(x) = " + example.similarExpression() + " + 5, calcule g'(x).",
                    "g'(x) = " + example.similarDerivative() + ", car la derivee de la constante 5 est 0.",
                    position
            );
            default -> buildQuestionWithOptions(
                    "Justification",
                    "Pourquoi la derivee de " + example.similarExpression() + " vaut-elle " + example.similarDerivative() + "?",
                    "On applique la regle de puissance: le coefficient est multiplie par l'exposant, puis l'exposant diminue de 1.",
                    position
            );
        };
    }

    private QuizQuestion buildQuestionWithOptions(String angle,
                                                  String questionText,
                                                  String expectedAnswer,
                                                  int position) {
        List<String> options = resolveQuestionOptions(expectedAnswer, List.of());
        return new QuizQuestion(
                angle,
                options.isEmpty() ? "OPEN" : "CHOICE",
                questionText,
                expectedAnswer,
                position,
                options
        );
    }

    private String resolveQuestionType(String expectedAnswer, String generatedType, List<String> generatedOptions) {
        if ("CHOICE".equalsIgnoreCase(generatedType) || (generatedOptions != null && !generatedOptions.isEmpty())) {
            return "CHOICE";
        }
        return resolveQuestionOptions(expectedAnswer, List.of()).isEmpty() ? "OPEN" : "CHOICE";
    }

    private List<String> resolveQuestionOptions(String expectedAnswer, List<String> generatedOptions) {
        List<String> sanitizedGenerated = generatedOptions == null ? List.of() : generatedOptions.stream()
                .map(option -> option == null ? "" : option.trim())
                .filter(option -> !option.isBlank())
                .distinct()
                .toList();
        if (sanitizedGenerated.size() >= 4) {
            return sanitizedGenerated.stream().limit(4).toList();
        }
        if (!shouldUseChoice(expectedAnswer)) {
            return List.of();
        }
        return buildFormulaOptions(expectedAnswer);
    }

    private boolean shouldUseChoice(String expectedAnswer) {
        String formula = extractPrimaryFormula(expectedAnswer);
        return !formula.isBlank() && formula.length() >= 6 && FORMULA_COMPLEXITY_PATTERN.matcher(formula).find();
    }

    private List<String> buildFormulaOptions(String expectedAnswer) {
        String correct = extractPrimaryFormula(expectedAnswer);
        if (correct.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> options = new LinkedHashSet<>();
        options.add(correct);
        options.add(tweakExponent(correct));
        options.add(tweakSign(correct));
        options.add(tweakCoefficient(correct));
        if (options.size() < 4) {
            options.add(correct + " + 1");
        }
        if (options.size() < 4) {
            options.add(correct.replace("=", "= "));
        }

        List<String> shuffled = new ArrayList<>(options.stream()
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .distinct()
                .limit(4)
                .toList());
        Collections.shuffle(shuffled);
        return shuffled.size() >= 4 ? shuffled : List.of();
    }

    private String extractPrimaryFormula(String text) {
        String formula = extractFormula(text);
        if (!formula.isBlank()) {
            return formula;
        }
        String cleaned = text == null ? "" : text.trim();
        return FORMULA_COMPLEXITY_PATTERN.matcher(cleaned).find() ? shorten(cleaned, 80) : "";
    }

    private String tweakExponent(String formula) {
        Matcher matcher = Pattern.compile("\\^(\\d+)").matcher(formula);
        if (matcher.find()) {
            int exponent = Integer.parseInt(matcher.group(1));
            return matcher.replaceFirst("^" + Math.max(1, exponent + 1));
        }
        if (formula.contains("x")) {
            return formula.replaceFirst("x", "x^2");
        }
        return formula + "^2";
    }

    private String tweakSign(String formula) {
        if (formula.contains(" = -")) {
            return formula.replace(" = -", " = ");
        }
        if (formula.contains("=")) {
            return formula.replaceFirst("=\\s*", "= -");
        }
        return "-" + formula;
    }

    private String tweakCoefficient(String formula) {
        Matcher matcher = Pattern.compile("(?<![A-Za-z])(\\d+)").matcher(formula);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            return matcher.replaceFirst(String.valueOf(value + 1));
        }
        if (formula.contains("x")) {
            return formula.replaceFirst("x", "2x");
        }
        return formula + " + 2";
    }

    private String shorten(String value, int maxLength) {
        String cleaned = value.trim();
        return cleaned.length() <= maxLength ? cleaned : cleaned.substring(0, maxLength).trim() + "...";
    }

    private QuizDto toDto(Quiz quiz) {
        Long animationId = quiz.getAnimation() != null ? quiz.getAnimation().getId() : null;
        String activityName = quiz.getAnimation() != null && quiz.getAnimation().getActivity() != null
                ? quiz.getAnimation().getActivity().getActivyName()
                : null;
        List<QuizAxisDto> axes = quiz.getAxes().stream()
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
                                        question.getExpectedAnswer(),
                                        question.getPosition(),
                                        question.getOptions()
                                ))
                                .toList()
                ))
                .toList();
        return new QuizDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSourceNotes(),
                quiz.getCreatedAt(),
                animationId,
                activityName,
                axes
        );
    }

    private TutorQuizSubmissionDto toSubmissionDto(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Animation animation = quiz.getAnimation();
        Long animationId = animation != null ? animation.getId() : null;
        String activityName = animation != null && animation.getActivity() != null
                ? animation.getActivity().getActivyName()
                : null;
        String enfantName = attempt.getEnfant().getPrenom() + " " + attempt.getEnfant().getNom();

        return new TutorQuizSubmissionDto(
                attempt.getId(),
                quiz.getId(),
                quiz.getTitle(),
                animationId,
                activityName,
                attempt.getEnfant().getId(),
                enfantName,
                attempt.getSubmittedAt(),
                attempt.getElapsedSeconds(),
                attempt.getScorePercent(),
                attempt.getStatus(),
                attempt.getAnswers().stream()
                        .sorted(Comparator.comparing(answer -> answer.getQuestion().getPosition()))
                        .map(this::toSubmissionAnswerDto)
                        .toList()
        );
    }

    private TutorQuizAnswerDto toSubmissionAnswerDto(QuizAnswer answer) {
        QuizQuestion question = answer.getQuestion();
        return new TutorQuizAnswerDto(
                question.getId(),
                question.getAxis().getTitle(),
                question.getAngle(),
                question.getQuestionText(),
                question.getExpectedAnswer(),
                answer.getAnswerText(),
                question.getOptions()
        );
    }

    private String capitalize(String value) {
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return cleaned.substring(0, 1).toUpperCase(Locale.ROOT) + cleaned.substring(1);
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

    private boolean isTutoringAnimation(Animation animation) {
        return animation != null
                && animation.getActivity() != null
                && animation.getActivity().getType() == typeActivity.TUTORAT;
    }

    private String buildNextSessionSuggestion(List<Quiz> quizzes,
                                              List<TutorAxisProgressDto> persistentAxes,
                                              List<TutorAxisProgressDto> axes,
                                              List<QuizAttempt> attempts,
                                              int enrolledCount,
                                              int responderCount) {
        if (enrolledCount > 0 && responderCount == 0 && !quizzes.isEmpty()) {
            return "Faire passer le dernier quiz aux " + enrolledCount + " enfant(s) inscrits pour initialiser les scores.";
        }
        if (enrolledCount > 0 && responderCount < enrolledCount) {
            return "Relancer les " + (enrolledCount - responderCount) + " enfant(s) sans reponse avant d'analyser la progression.";
        }
        if (!persistentAxes.isEmpty()) {
            String axis = persistentAxes.get(0).axisTitle();
            return "Revenir sur " + axis + " et demander une justification courte avant de passer aux exercices.";
        }
        if (!axes.isEmpty()) {
            String axis = axes.get(0).axisTitle();
            return "Faire passer le micro-diagnostic sur " + axis + ", puis saisir les resultats pour activer le suivi de progression.";
        }
        if (!quizzes.isEmpty()) {
            return "Faire passer le dernier quiz cree et saisir les resultats avant la prochaine seance.";
        }
        if (attempts.isEmpty() && enrolledCount > 0) {
            return "Creer un quiz pour les enfants inscrits et demarrer le suivi de progression.";
        }
        return "Creer un premier quiz a partir des notes de seance pour initialiser le suivi.";
    }

    private static class AxisStats {
        private final String title;
        private int quizCount;
        private int questionCount;
        private int attemptCount;
        private double scoreTotal;
        private int scoredAttemptCount;
        private int timeTotal;
        private int timedAttemptCount;
        private String latestQuizTitle;
        private java.time.LocalDateTime latestQuizCreatedAt;

        AxisStats(String title) {
            this.title = title;
        }

        void registerQuiz(Quiz quiz, QuizAxis axis) {
            quizCount++;
            questionCount += axis.getQuestions().size();
            if (latestQuizCreatedAt == null || quiz.getCreatedAt().isAfter(latestQuizCreatedAt)) {
                latestQuizTitle = quiz.getTitle();
                latestQuizCreatedAt = quiz.getCreatedAt();
            }
        }

        void registerAttempt(QuizAttempt attempt) {
            attemptCount++;
            if (attempt.getScorePercent() != null) {
                scoreTotal += attempt.getScorePercent();
                scoredAttemptCount++;
            }
            if (attempt.getElapsedSeconds() != null && attempt.getElapsedSeconds() >= 0) {
                timeTotal += attempt.getElapsedSeconds();
                timedAttemptCount++;
            }
        }

        String getTitle() {
            return title;
        }

        int getQuizCount() {
            return quizCount;
        }

        TutorAxisProgressDto toDto() {
            Double scorePercent = scoredAttemptCount == 0 ? null : scoreTotal / scoredAttemptCount;
            Integer averageTime = timedAttemptCount == 0 ? null : (int) Math.round((double) timeTotal / timedAttemptCount);
            return new TutorAxisProgressDto(
                    title,
                    quizCount,
                    questionCount,
                    scorePercent,
                    averageTime,
                    axisStatus(),
                    latestQuizTitle,
                    latestQuizCreatedAt
            );
        }

        private String axisStatus() {
            if (attemptCount == 0) {
                return "En attente de reponses";
            }
            if (scoredAttemptCount == 0) {
                return "En attente de scoring";
            }
            double score = scoreTotal / scoredAttemptCount;
            if (score >= 75) {
                return "Maitrise solide";
            }
            if (score >= 50) {
                return "A consolider";
            }
            return "Prioritaire";
        }
    }

    private record LocalFocus(String title, String evidence, String formula, DerivativeExample derivativeExample) {
    }

    private record DerivativeExample(String originalExpression,
                                     String derivative,
                                     String similarExpression,
                                     String similarDerivative) {
    }
}
