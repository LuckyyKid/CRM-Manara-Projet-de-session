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
@Table(name = "SportPracticePlanItem")
public class SportPracticePlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SportPracticePlan plan;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "Instructions", nullable = false, columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "Purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "DurationLabel")
    private String durationLabel;

    @Column(name = "SafetyTip", columnDefinition = "TEXT")
    private String safetyTip;

    @Column(name = "Position", nullable = false)
    private int position;

    protected SportPracticePlanItem() {
    }

    public SportPracticePlanItem(String title,
                                 String instructions,
                                 String purpose,
                                 String durationLabel,
                                 String safetyTip,
                                 int position) {
        this.title = title;
        this.instructions = instructions;
        this.purpose = purpose;
        this.durationLabel = durationLabel;
        this.safetyTip = safetyTip;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public SportPracticePlan getPlan() {
        return plan;
    }

    public void setPlan(SportPracticePlan plan) {
        this.plan = plan;
    }

    public String getTitle() {
        return title;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getDurationLabel() {
        return durationLabel;
    }

    public String getSafetyTip() {
        return safetyTip;
    }

    public int getPosition() {
        return position;
    }
}
