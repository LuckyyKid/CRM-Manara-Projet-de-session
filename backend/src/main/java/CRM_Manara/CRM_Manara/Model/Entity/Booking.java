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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private AppointmentSlot slot;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "animateur_id", nullable = false)
    private Animateur animateur;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Booking() {
    }

    public Booking(AppointmentSlot slot,
                   Animateur animateur,
                   Parent parent,
                   LocalDate date,
                   LocalDateTime startTime,
                   LocalDateTime endTime,
                   String status) {
        this.slot = slot;
        this.animateur = animateur;
        this.parent = parent;
        this.date = date;
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

    public AppointmentSlot getSlot() {
        return slot;
    }

    public Animateur getAnimateur() {
        return animateur;
    }

    public Parent getParent() {
        return parent;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void cancel() {
        this.status = "CANCELLED";
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = this.cancelledAt;
    }

    public void reschedule() {
        this.status = "RESCHEDULED";
        this.updatedAt = LocalDateTime.now();
    }

    public void detachSlot() {
        this.slot = null;
    }
}
