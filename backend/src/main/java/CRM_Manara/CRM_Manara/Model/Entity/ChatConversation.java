package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(
        name = "chat_conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participant_one_id", "participant_two_id"})
)
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_one_id", nullable = false)
    private User participantOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_two_id", nullable = false)
    private User participantTwo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "updated_at", nullable = false)
    private Date updatedAt;

    @Column(name = "last_message_preview", length = 280)
    private String lastMessagePreview;

    @Column(name = "last_message_at")
    private Date lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    protected ChatConversation() {
    }

    public ChatConversation(User participantOne, User participantTwo) {
        this.participantOne = participantOne;
        this.participantTwo = participantTwo;
    }

    @PrePersist
    private void onCreate() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void touchWithPreview(String body) {
        Date now = new Date();
        this.updatedAt = now;
        this.lastMessageAt = now;
        if (body == null) {
            this.lastMessagePreview = null;
            return;
        }
        String normalized = body.trim().replaceAll("\\s+", " ");
        this.lastMessagePreview = normalized.length() > 140 ? normalized.substring(0, 137) + "..." : normalized;
    }

    public Long getId() {
        return id;
    }

    public User getParticipantOne() {
        return participantOne;
    }

    public User getParticipantTwo() {
        return participantTwo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public Date getLastMessageAt() {
        return lastMessageAt;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
