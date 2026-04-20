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
@Table(name = "QuizAnswer")
public class QuizAnswer {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private QuizAttempt attempt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private QuizQuestion question;

    @Column(name = "AnswerText", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    protected QuizAnswer() {
    }

    public QuizAnswer(QuizQuestion question, String answerText) {
        this.question = question;
        this.answerText = answerText;
    }

    public Long getId() {
        return id;
    }

    public QuizAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(QuizAttempt attempt) {
        this.attempt = attempt;
    }

    public QuizQuestion getQuestion() {
        return question;
    }

    public String getAnswerText() {
        return answerText;
    }
}
