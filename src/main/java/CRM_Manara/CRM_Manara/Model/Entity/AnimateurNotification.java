package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "animateur_notifications")
public class AnimateurNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animateur_id", nullable = false)
    private Animateur animateur;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus = false;

    @Column(name = "archived_status", nullable = false)
    private boolean archivedStatus = false;

    protected AnimateurNotification() {
    }

    public AnimateurNotification(Animateur animateur, String category, String title, String message) {
        this.animateur = animateur;
        this.category = category;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Animateur getAnimateur() {
        return animateur;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public void setReadStatus(boolean readStatus) {
        this.readStatus = readStatus;
    }

    public boolean isArchivedStatus() {
        return archivedStatus;
    }

    public void setArchivedStatus(boolean archivedStatus) {
        this.archivedStatus = archivedStatus;
    }
}
