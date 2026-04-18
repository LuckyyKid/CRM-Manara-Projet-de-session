package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Devoir personnalisé généré par l'IA pour un étudiant sur un axe précis
@Entity
@Table(name = "tutoring_homework")
public class TutoringHomework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Enfant student;

    @ManyToOne
    @JoinColumn(name = "axis_id")
    private TutoringAxis axis;

    // JSON des exercices générés par l'IA
    @Column(name = "exercises_json", columnDefinition = "TEXT")
    private String exercisesJson;

    // "pending" ou "completed"
    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected TutoringHomework() {}

    public TutoringHomework(Enfant student, TutoringAxis axis, String exercisesJson) {
        this.student = student;
        this.axis = axis;
        this.exercisesJson = exercisesJson;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Enfant getStudent() { return student; }
    public TutoringAxis getAxis() { return axis; }
    public String getExercisesJson() { return exercisesJson; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setStatus(String status) { this.status = status; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public void setExercisesJson(String exercisesJson) { this.exercisesJson = exercisesJson; }
}
