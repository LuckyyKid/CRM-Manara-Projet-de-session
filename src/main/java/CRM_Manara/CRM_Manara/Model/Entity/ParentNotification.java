package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "parent_notifications")
public class ParentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "category", nullable = false, length = 80)
    private String category;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", nullable = false, length = 1200)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_status", nullable = false)
    private boolean readStatus;

    protected ParentNotification() {
    }

    public ParentNotification(Parent parent, String category, String title, String message) {
        this.parent = parent;
        this.category = category;
        this.title = title;
        this.message = message;
        this.readStatus = false;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Parent getParent() {
        return parent;
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
}
