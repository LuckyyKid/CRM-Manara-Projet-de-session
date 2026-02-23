package CRM_Manara.CRM_Manara.Model.Entity;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "Inscription",uniqueConstraints = {
@UniqueConstraint(columnNames = {"enfant_id", "animation_id"})
    })

public class Inscription {
    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name= "StatusInscription")
    private statusInscription statusInscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "PresenceStatus", nullable = false)
    private PresenceStatus presenceStatus;

    @Column(name = "IncidentNote", length = 1000)
    private String incidentNote;

    @ManyToOne(optional = false)
    private Enfant enfant;

    @ManyToOne(optional = false)
   private Animation animation;



    protected Inscription() {

    }

    public Inscription(Enfant enfant, Animation animation) {
        this.enfant = enfant;
        this.animation = animation;
        this.statusInscription = statusInscription.ACTIF;
        this.presenceStatus = PresenceStatus.NON_SIGNEE;
    }


    public Long getId() {
        return id;
    }
    public Animation getAnimation() {
        return animation;
    }
    public Enfant getEnfant() {
        return enfant;
    }
    public statusInscription getStatusInscription() {
        return statusInscription;
    }
    public PresenceStatus getPresenceStatus() {
        return presenceStatus;
    }
    public void setPresenceStatus(PresenceStatus presenceStatus) {
        this.presenceStatus = presenceStatus;
    }
    public String getIncidentNote() {
        return incidentNote;
    }
    public void setIncidentNote(String incidentNote) {
        this.incidentNote = incidentNote;
    }




}
