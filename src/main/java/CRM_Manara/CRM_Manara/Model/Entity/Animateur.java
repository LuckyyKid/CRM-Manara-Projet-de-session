package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Animateurs")

public class Animateur {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "animateur", fetch = FetchType.LAZY)
    private List<Animation> animations = new ArrayList<>();

    protected Animateur() {

    }

    public Animateur(String nom, String prenom) {
        this.nom = nom;
        this.prenom = prenom;

    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
