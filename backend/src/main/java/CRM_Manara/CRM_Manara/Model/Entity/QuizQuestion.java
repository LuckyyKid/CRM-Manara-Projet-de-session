package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "QuizQuestion")
public class QuizQuestion {

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

    @Column(name = "PositionIndex", nullable = false)
    private int position;

    protected QuizQuestion() {
    }

    public QuizQuestion(String angle, String type, String questionText, String expectedAnswer, int position) {
        this.angle = angle;
        this.type = type;
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.position = position;
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

    public int getPosition() {
        return position;
    }
}
