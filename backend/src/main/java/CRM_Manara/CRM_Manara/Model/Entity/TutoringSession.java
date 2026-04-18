package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Représente une séance de tutorat liée à une animation
@Entity
@Table(name = "tutoring_session")
public class TutoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'animation (cours) à laquelle cette séance est rattachée
    @ManyToOne
    @JoinColumn(name = "animation_id")
    private Animation animation;

    // L'animateur qui a créé la séance
    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Animateur tutor;

    // Le texte de la matière vue (entré par le tuteur)
    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected TutoringSession() {}

    public TutoringSession(Animation animation, Animateur tutor, String contentText) {
        this.animation = animation;
        this.tutor = tutor;
        this.contentText = contentText;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Animation getAnimation() { return animation; }
    public Animateur getTutor() { return tutor; }
    public String getContentText() { return contentText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
