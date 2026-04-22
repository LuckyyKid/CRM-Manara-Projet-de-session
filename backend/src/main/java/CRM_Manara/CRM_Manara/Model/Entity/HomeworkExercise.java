package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Entity
@Table(name = "HomeworkExercise")
public class HomeworkExercise {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private HomeworkAssignment assignment;

    @Column(name = "AxisTitle", nullable = false)
    private String axisTitle;

    @Column(name = "ExerciseType")
    private String type;

    @Column(name = "Difficulty", nullable = false)
    private String difficulty;

    @Column(name = "QuestionText", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "ExpectedAnswer", nullable = false, columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(name = "TargetMistake", columnDefinition = "TEXT")
    private String targetMistake;

    @Column(name = "OptionsJson", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "Position", nullable = false)
    private int position;

    protected HomeworkExercise() {
    }

    public HomeworkExercise(String axisTitle,
                            String type,
                            String difficulty,
                            String questionText,
                            String expectedAnswer,
                            String targetMistake,
                            int position) {
        this(axisTitle, type, difficulty, questionText, expectedAnswer, targetMistake, position, List.of());
    }

    public HomeworkExercise(String axisTitle,
                            String type,
                            String difficulty,
                            String questionText,
                            String expectedAnswer,
                            String targetMistake,
                            int position,
                            List<String> options) {
        this.axisTitle = axisTitle;
        this.type = type;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.targetMistake = targetMistake;
        this.position = position;
        this.optionsJson = encodeOptions(options);
    }

    public Long getId() {
        return id;
    }

    public HomeworkAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(HomeworkAssignment assignment) {
        this.assignment = assignment;
    }

    public String getAxisTitle() {
        return axisTitle;
    }

    public String getType() {
        return type == null || type.isBlank() ? "OPEN" : type;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public String getTargetMistake() {
        return targetMistake;
    }

    public List<String> getOptions() {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(optionsJson, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de lire les options de l'exercice.", exception);
        }
    }

    public int getPosition() {
        return position;
    }

    private String encodeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(options);
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'enregistrer les options de l'exercice.", exception);
        }
    }
}
