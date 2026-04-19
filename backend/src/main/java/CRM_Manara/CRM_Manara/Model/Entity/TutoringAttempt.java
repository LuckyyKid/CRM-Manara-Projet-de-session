package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// La réponse d'un étudiant à une question de quiz
@Entity
@Table(name = "tutoring_attempt")
public class TutoringAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'enfant qui a répondu
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Enfant student;

    // La question à laquelle il a répondu
    @ManyToOne
    @JoinColumn(name = "question_id")
    private TutoringQuestion question;

    // Ce que l'étudiant a répondu
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    // Vrai si la réponse est correcte
    @Column(name = "correct")
    private boolean correct;

    // Temps de réponse en millisecondes
    @Column(name = "response_time_ms")
    private int responseTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected TutoringAttempt() {}

    public TutoringAttempt(Enfant student, TutoringQuestion question,
                            String answer, boolean correct, int responseTimeMs) {
        this.student = student;
        this.question = question;
        this.answer = answer;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Enfant getStudent() { return student; }
    public TutoringQuestion getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public boolean isCorrect() { return correct; }
    public int getResponseTimeMs() { return responseTimeMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
