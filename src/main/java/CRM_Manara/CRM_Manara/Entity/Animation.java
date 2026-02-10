package CRM_Manara.CRM_Manara.Entity;

import CRM_Manara.CRM_Manara.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Enum.status;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "Animation")
public class Animation {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Animateur animateur;

    @ManyToOne
    private Activity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role")
    private AnimationRole role;

    @Enumerated(EnumType.STRING)
    @Column(name="Status")
    private animationStatus statusAnimation;

    @Column(name = "Start",nullable = false)
    private LocalDateTime startTime;

    @Column(name = "End",nullable = false)
    private LocalDateTime endTime;

    protected Animation() {

    }

    public Animation(AnimationRole Role, animationStatus Status, LocalDateTime Start, LocalDateTime End) {
        this.role = Role;
        this.statusAnimation = Status;
        this.startTime = Start;
        this.endTime = End;

    }


    public Long getId() {
        return id;
    }

    public Animateur getAnimateur() {
        return animateur;
    }

    public Activity getActivity() {
        return activity;
    }

    public AnimationRole getRole() {
        return role;
    }
    public void setRole(AnimationRole role) {
        this.role = role;
    }
    public animationStatus getStatusAnimation() {
        return statusAnimation;
    }
    public void setStatusAnimation(animationStatus statusAnimation) {
        this.statusAnimation = statusAnimation;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

}
