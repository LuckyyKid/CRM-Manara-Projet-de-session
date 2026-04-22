package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_slots")
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "animateur_id", nullable = false)
    private Animateur animateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    protected AppointmentSlot() {
    }

    public AppointmentSlot(Animateur animateur, LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.animateur = animateur;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Animateur getAnimateur() {
        return animateur;
    }

    public Parent getParent() {
        return parent;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public void setBookedAt(LocalDateTime bookedAt) {
        this.bookedAt = bookedAt;
    }

    public void reserve(Parent parent) {
        this.parent = parent;
        this.status = "BOOKED";
        this.bookedAt = LocalDateTime.now();
    }

    public void clearReservation() {
        this.parent = null;
        this.status = "AVAILABLE";
        this.bookedAt = null;
    }
}
