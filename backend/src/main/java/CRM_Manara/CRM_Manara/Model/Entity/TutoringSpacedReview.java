package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

// Révision espacée : rappelle à l'étudiant de revoir un axe maîtrisé
@Entity
@Table(name = "tutoring_spaced_review")
public class TutoringSpacedReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Enfant student;

    @ManyToOne
    @JoinColumn(name = "axis_id")
    private TutoringAxis axis;

    // Date de la prochaine révision
    @Column(name = "next_review_date")
    private LocalDate nextReviewDate;

    // Intervalle en jours : 2, 5, 14, 30
    @Column(name = "interval_days")
    private int intervalDays;

    // Nombre de révisions consécutives réussies
    @Column(name = "consecutive_successes")
    private int consecutiveSuccesses;

    protected TutoringSpacedReview() {}

    public TutoringSpacedReview(Enfant student, TutoringAxis axis, LocalDate nextReviewDate) {
        this.student = student;
        this.axis = axis;
        this.nextReviewDate = nextReviewDate;
        this.intervalDays = 2;
        this.consecutiveSuccesses = 0;
    }

    public Long getId() { return id; }
    public Enfant getStudent() { return student; }
    public TutoringAxis getAxis() { return axis; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public int getIntervalDays() { return intervalDays; }
    public int getConsecutiveSuccesses() { return consecutiveSuccesses; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public void setConsecutiveSuccesses(int consecutiveSuccesses) { this.consecutiveSuccesses = consecutiveSuccesses; }
}
