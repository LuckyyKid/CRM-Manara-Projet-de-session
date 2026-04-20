package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.QuizAxis;
import CRM_Manara.CRM_Manara.Model.Entity.QuizQuestion;
import CRM_Manara.CRM_Manara.Repository.QuizRepo;
import CRM_Manara.CRM_Manara.dto.QuizAxisDto;
import CRM_Manara.CRM_Manara.dto.QuizCreateRequestDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.QuizQuestionDto;
import CRM_Manara.CRM_Manara.dto.TutorAxisProgressDto;
import CRM_Manara.CRM_Manara.dto.TutorDashboardDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuizService {

    private static final int MIN_NOTES_LENGTH = 20;
    private static final int MIN_AXES = 3;
    private static final int MAX_AXES = 7;
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}[\\p{L}'-]{2,}");
    private static final List<String> FALLBACK_AXES = List.of(
            "Concepts principaux",
            "Methodes et etapes",
            "Erreurs frequentes"
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
    private final AnimateurService animateurService;

    public QuizService(QuizRepo quizRepo, AnimateurService animateurService) {
        this.quizRepo = quizRepo;
        this.animateurService = animateurService;
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
        }

        String notes = request.sourceNotes().trim();
        String title = cleanTitle(request.title(), animation);
        Quiz quiz = new Quiz(animateur, animation, title, notes);
        List<String> axes = extractAxes(notes, animation);

        for (int i = 0; i < axes.size(); i++) {
            String axisTitle = axes.get(i);
            QuizAxis axis = new QuizAxis(axisTitle, buildSummary(axisTitle, notes), i + 1);
            for (int j = 0; j < ANGLES.size(); j++) {
                axis.addQuestion(buildQuestion(axisTitle, ANGLES.get(j), j + 1));
            }
            quiz.addAxis(axis);
        }

        return toDto(quizRepo.save(quiz));
    }

    @Transactional(readOnly = true)
    public List<QuizDto> listForAnimateur(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        return quizRepo.findByAnimateurIdOrderByCreatedAtDesc(animateur.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizDto getForAnimateur(Long quizId, String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Quiz quiz = quizRepo.findByIdAndAnimateurId(quizId, animateur.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable."));
        return toDto(quiz);
    }

    @Transactional
    public void deleteForAnimateur(Long quizId, String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        Quiz quiz = quizRepo.findByIdAndAnimateurId(quizId, animateur.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz introuvable."));
        quizRepo.delete(quiz);
    }

    @Transactional(readOnly = true)
    public TutorDashboardDto getTutorDashboard(String animateurEmail) {
        Animateur animateur = animateurService.getAnimateurByEmail(animateurEmail);
        List<Quiz> quizzes = quizRepo.findByAnimateurIdOrderByCreatedAtDesc(animateur.getId());
        Map<String, AxisStats> statsByAxis = new LinkedHashMap<>();

        for (Quiz quiz : quizzes) {
            for (QuizAxis axis : quiz.getAxes()) {
                String key = normalizeAxis(axis.getTitle());
                AxisStats stats = statsByAxis.computeIfAbsent(key, ignored -> new AxisStats(axis.getTitle()));
                stats.registerQuiz(quiz, axis);
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
        String suggestion = buildNextSessionSuggestion(quizzes, persistentAxes, axes);

        return new TutorDashboardDto(
                quizzes.size(),
                axes.size(),
                questionCount,
                null,
                null,
                quizzes.isEmpty() ? "Aucun quiz cree" : "En attente de tentatives et de scoring",
                suggestion,
                quizzes.isEmpty() ? null : quizzes.get(0).getCreatedAt(),
                axes,
                persistentAxes
        );
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

    private List<String> extractAxes(String notes, Animation animation) {
        LinkedHashSet<String> axes = new LinkedHashSet<>();
        if (animation != null && animation.getActivity() != null
                && animation.getActivity().getActivyName() != null
                && !animation.getActivity().getActivyName().isBlank()) {
            axes.add(animation.getActivity().getActivyName().trim());
        }

        axes.addAll(extractExplicitAxes(notes));
        axes.addAll(extractKeywordAxes(notes));
        axes.addAll(FALLBACK_AXES);

        return axes.stream()
                .filter(axis -> axis != null && !axis.isBlank())
                .limit(MAX_AXES)
                .toList();
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
                .map(word -> "Axe " + capitalize(word))
                .toList();
    }

    private String buildSummary(String axisTitle, String notes) {
        String sentence = findSentenceForAxis(axisTitle, notes);
        if (sentence.isBlank()) {
            return "Axe extrait des notes de seance pour verifier la comprehension immediate.";
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

    private QuizQuestion buildQuestion(String axisTitle, String angle, int position) {
        return switch (angle) {
            case "reconnaissance" -> new QuizQuestion(
                    "Reconnaissance",
                    "OPEN",
                    "Dans tes mots, identifie l'idee principale reliee a: " + axisTitle + ".",
                    "L'etudiant nomme le concept central et un indice vu pendant la seance.",
                    position
            );
            case "application" -> new QuizQuestion(
                    "Application",
                    "OPEN",
                    "Resous un petit exemple ou explique une procedure qui utilise: " + axisTitle + ".",
                    "La reponse applique correctement la methode ou le concept a une situation simple.",
                    position
            );
            case "piege" -> new QuizQuestion(
                    "Piege",
                    "OPEN",
                    "Quelle erreur frequente pourrait arriver avec " + axisTitle + ", et comment l'eviter?",
                    "La reponse nomme une confusion plausible et donne une verification concrete.",
                    position
            );
            case "transfert" -> new QuizQuestion(
                    "Transfert",
                    "OPEN",
                    "Donne un exemple nouveau, different de celui de la seance, ou " + axisTitle + " serait utile.",
                    "La reponse transfere l'axe dans un contexte nouveau sans changer le principe.",
                    position
            );
            default -> new QuizQuestion(
                    "Justification",
                    "OPEN",
                    "Pourquoi la demarche associee a " + axisTitle + " fonctionne-t-elle?",
                    "La reponse justifie le raisonnement avec une cause, une regle ou un lien logique.",
                    position
            );
        };
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
                                        question.getPosition()
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

    private String buildNextSessionSuggestion(List<Quiz> quizzes,
                                              List<TutorAxisProgressDto> persistentAxes,
                                              List<TutorAxisProgressDto> axes) {
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
        return "Creer un premier quiz a partir des notes de seance pour initialiser le suivi.";
    }

    private static class AxisStats {
        private final String title;
        private int quizCount;
        private int questionCount;
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

        String getTitle() {
            return title;
        }

        int getQuizCount() {
            return quizCount;
        }

        TutorAxisProgressDto toDto() {
            return new TutorAxisProgressDto(
                    title,
                    quizCount,
                    questionCount,
                    null,
                    null,
                    "En attente de scoring",
                    latestQuizTitle,
                    latestQuizCreatedAt
            );
        }
    }
}
