package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminNotification() {
    }

    public AdminNotification(String source, String type, String message) {
        this.source = source;
        this.type = type;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
