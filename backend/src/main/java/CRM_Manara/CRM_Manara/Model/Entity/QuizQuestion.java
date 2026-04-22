package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Entity
@Table(name = "QuizQuestion")
public class QuizQuestion {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private QuizAxis axis;

    @Column(name = "Angle", nullable = false)
    private String angle;

    @Column(name = "QuestionType", nullable = false)
    private String type;

    @Column(name = "QuestionText", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "ExpectedAnswer", nullable = false, columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(name = "OptionsJson", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "PositionIndex", nullable = false)
    private int position;

    protected QuizQuestion() {
    }

    public QuizQuestion(String angle, String type, String questionText, String expectedAnswer, int position) {
        this(angle, type, questionText, expectedAnswer, intPosition(position), List.of());
    }

    public QuizQuestion(String angle,
                        String type,
                        String questionText,
                        String expectedAnswer,
                        int position,
                        List<String> options) {
        this.angle = angle;
        this.type = type;
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.position = position;
        this.optionsJson = encodeOptions(options);
    }

    public Long getId() {
        return id;
    }

    public QuizAxis getAxis() {
        return axis;
    }

    public void setAxis(QuizAxis axis) {
        this.axis = axis;
    }

    public String getAngle() {
        return angle;
    }

    public String getType() {
        return type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public List<String> getOptions() {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(optionsJson, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de lire les options de la question.", exception);
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
            throw new IllegalStateException("Impossible d'enregistrer les options de la question.", exception);
        }
    }

    private static int intPosition(int position) {
        return position;
    }
}
