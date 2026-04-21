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
@Table(name = "HomeworkAnswer")
public class HomeworkAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private HomeworkAttempt attempt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private HomeworkExercise exercise;

    @Column(name = "AnswerText", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    protected HomeworkAnswer() {
    }

    public HomeworkAnswer(HomeworkExercise exercise, String answerText) {
        this.exercise = exercise;
        this.answerText = answerText;
    }

    public Long getId() {
        return id;
    }

    public HomeworkAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(HomeworkAttempt attempt) {
        this.attempt = attempt;
    }

    public HomeworkExercise getExercise() {
        return exercise;
    }

    public String getAnswerText() {
        return answerText;
    }
}
