package CRM_Manara.CRM_Manara.Model.Entity;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
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




}
