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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HomeworkAssignment")
public class HomeworkAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Animateur animateur;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Enfant enfant;

    @ManyToOne(fetch = FetchType.LAZY)
    private Animation animation;

    @ManyToOne(fetch = FetchType.LAZY)
    private Quiz sourceQuiz;

    @ManyToOne(fetch = FetchType.LAZY)
    private QuizAttempt sourceAttempt;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "Summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "DueDate")
    private LocalDate dueDate;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<HomeworkExercise> exercises = new ArrayList<>();

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("submittedAt DESC")
    private List<HomeworkAttempt> attempts = new ArrayList<>();

    protected HomeworkAssignment() {
    }

    public HomeworkAssignment(Animateur animateur,
                              Enfant enfant,
                              Animation animation,
                              Quiz sourceQuiz,
                              QuizAttempt sourceAttempt,
                              String title,
                              String summary,
                              LocalDate dueDate) {
        this.animateur = animateur;
        this.enfant = enfant;
        this.animation = animation;
        this.sourceQuiz = sourceQuiz;
        this.sourceAttempt = sourceAttempt;
        this.title = title;
        this.summary = summary;
        this.dueDate = dueDate;
        this.status = "ASSIGNED";
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Animateur getAnimateur() {
        return animateur;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public Animation getAnimation() {
        return animation;
    }

    public Quiz getSourceQuiz() {
        return sourceQuiz;
    }

    public QuizAttempt getSourceAttempt() {
        return sourceAttempt;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public List<HomeworkExercise> getExercises() {
        return exercises;
    }

    public List<HomeworkAttempt> getAttempts() {
        return attempts;
    }

    public void addExercise(HomeworkExercise exercise) {
        exercise.setAssignment(this);
        exercises.add(exercise);
    }

    public void addAttempt(HomeworkAttempt attempt) {
        attempt.setAssignment(this);
        attempts.add(attempt);
    }

    public void markCompleted() {
        this.status = "COMPLETED";
    }

    public void markAssigned() {
        this.status = "ASSIGNED";
    }
}
