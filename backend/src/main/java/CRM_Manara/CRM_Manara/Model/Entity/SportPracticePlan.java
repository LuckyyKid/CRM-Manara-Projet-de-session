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
@Table(name = "SportPracticePlan")
public class SportPracticePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Animateur animateur;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Animation animation;

    @Column(name = "Title", nullable = false)
    private String title;

    @Column(name = "Summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "SourceNotes", nullable = false, columnDefinition = "TEXT")
    private String sourceNotes;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<SportPracticePlanItem> items = new ArrayList<>();

    protected SportPracticePlan() {
    }

    public SportPracticePlan(Animateur animateur,
                             Animation animation,
                             String title,
                             String summary,
                             String sourceNotes) {
        this.animateur = animateur;
        this.animation = animation;
        this.title = title;
        this.summary = summary;
        this.sourceNotes = sourceNotes;
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

    public Animation getAnimation() {
        return animation;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceNotes() {
        return sourceNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<SportPracticePlanItem> getItems() {
        return items;
    }

    public void addItem(SportPracticePlanItem item) {
        item.setPlan(this);
        items.add(item);
    }
}
