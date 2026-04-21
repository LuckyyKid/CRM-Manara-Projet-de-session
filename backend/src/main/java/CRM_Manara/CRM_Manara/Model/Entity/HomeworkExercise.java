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
@Table(name = "HomeworkExercise")
public class HomeworkExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private HomeworkAssignment assignment;

    @Column(name = "AxisTitle", nullable = false)
    private String axisTitle;

    @Column(name = "Difficulty", nullable = false)
    private String difficulty;

    @Column(name = "QuestionText", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "ExpectedAnswer", nullable = false, columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(name = "TargetMistake", columnDefinition = "TEXT")
    private String targetMistake;

    @Column(name = "Position", nullable = false)
    private int position;

    protected HomeworkExercise() {
    }

    public HomeworkExercise(String axisTitle,
                            String difficulty,
                            String questionText,
                            String expectedAnswer,
                            String targetMistake,
                            int position) {
        this.axisTitle = axisTitle;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.expectedAnswer = expectedAnswer;
        this.targetMistake = targetMistake;
        this.position = position;
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

    public int getPosition() {
        return position;
    }
}
