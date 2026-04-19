package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Alerte automatique quand 60%+ des étudiants échouent sur un axe
@Entity
@Table(name = "tutoring_group_alert")
public class TutoringGroupAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "animation_id")
    private Animation animation;

    @ManyToOne
    @JoinColumn(name = "axis_id")
    private TutoringAxis axis;

    // Taux d'échec (entre 0.0 et 1.0)
    @Column(name = "failure_rate")
    private double failureRate;

    // Nombre d'étudiants en difficulté
    @Column(name = "affected_count")
    private int affectedCount;

    // Nombre total d'étudiants
    @Column(name = "total_count")
    private int totalCount;

    // L'erreur la plus fréquente dans le groupe
    @Column(name = "dominant_error")
    private String dominantError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected TutoringGroupAlert() {}

    public TutoringGroupAlert(Animation animation, TutoringAxis axis,
                               double failureRate, int affectedCount, int totalCount,
                               String dominantError) {
        this.animation = animation;
        this.axis = axis;
        this.failureRate = failureRate;
        this.affectedCount = affectedCount;
        this.totalCount = totalCount;
        this.dominantError = dominantError;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Animation getAnimation() { return animation; }
    public TutoringAxis getAxis() { return axis; }
    public double getFailureRate() { return failureRate; }
    public int getAffectedCount() { return affectedCount; }
    public int getTotalCount() { return totalCount; }
    public String getDominantError() { return dominantError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
