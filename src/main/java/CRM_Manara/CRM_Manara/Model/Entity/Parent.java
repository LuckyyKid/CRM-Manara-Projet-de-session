package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity

@Table(name = "Parent")

public class Parent  {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;


    @Column(name= "adresse")
    private String adresse;




    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Enfant> enfants = new ArrayList<>();

    protected Parent() {

    }

    public Parent( String nom, String prenom, String adresse) {
        this.nom = nom;
        this.prenom = prenom;
        this.adresse = adresse;


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





    public List<Enfant> getEnfants() {
        return enfants;
    }
    public void AddEnfant(Enfant enfant) {
        enfants.add(enfant);
        enfant.setParent(this);
    }

    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public Long getId() {
        return id;
    }

    public void SetUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }


}
