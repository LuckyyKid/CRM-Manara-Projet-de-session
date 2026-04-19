package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Score d'un étudiant sur un axe pédagogique (mis à jour après chaque quiz)
@Entity
@Table(name = "tutoring_axis_score")
public class TutoringAxisScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Enfant student;

    @ManyToOne
    @JoinColumn(name = "axis_id")
    private TutoringAxis axis;

    // Score entre 0.0 et 1.0
    @Column(name = "score")
    private double score;

    // "weak" (< 0.33), "learning" (< 0.66), "mastered" (>= 0.66)
    @Column(name = "mastery_status")
    private String masteryStatus;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected TutoringAxisScore() {}

    public TutoringAxisScore(Enfant student, TutoringAxis axis, double score, String masteryStatus) {
        this.student = student;
        this.axis = axis;
        this.score = score;
        this.masteryStatus = masteryStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Enfant getStudent() { return student; }
    public TutoringAxis getAxis() { return axis; }
    public double getScore() { return score; }
    public String getMasteryStatus() { return masteryStatus; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setScore(double score) { this.score = score; }
    public void setMasteryStatus(String masteryStatus) { this.masteryStatus = masteryStatus; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
