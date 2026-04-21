package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "QuizAttempt")
public class QuizAttempt {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Quiz quiz;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Enfant enfant;

    @Column(name = "SubmittedAt", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "ElapsedSeconds")
    private Integer elapsedSeconds;

    @Column(name = "ScorePercent")
    private Double scorePercent;

    @Column(name = "Status", nullable = false)
    private String status;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("question.id ASC")
    private List<QuizAnswer> answers = new ArrayList<>();

    protected QuizAttempt() {
    }

    public QuizAttempt(Quiz quiz, Enfant enfant, Integer elapsedSeconds) {
        this.quiz = quiz;
        this.enfant = enfant;
        this.elapsedSeconds = elapsedSeconds;
        this.status = "SUBMITTED";
    }

    @PrePersist
    private void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public Double getScorePercent() {
        return scorePercent;
    }

    public String getStatus() {
        return status;
    }

    public void markScored(Double scorePercent, String status) {
        this.scorePercent = scorePercent;
        this.status = status;
    }

    public List<QuizAnswer> getAnswers() {
        return answers;
    }

    public void addAnswer(QuizAnswer answer) {
        answer.setAttempt(this);
        answers.add(answer);
    }

}
