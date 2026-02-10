package CRM_Manara.CRM_Manara.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Enfant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="nom" , nullable = false)
    private String nom;

    @Column(name ="prenom" , nullable = false)
    private String prenom;

    @Temporal(TemporalType.DATE)
    @Column(name ="date_de_naissance" , nullable = false)
    private Date date_de_naissance;


    @OneToMany(mappedBy = "enfant",cascade = CascadeType.ALL)
    private List<Activity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "enfant",cascade = CascadeType.ALL)
    private List<Planing> planings = new ArrayList<>();



    @ManyToOne
    @JoinColumn(name = "Parent_id")
    private Parent parent;

    protected Enfant() {

    }
    public Enfant(String nom, String prenom, Date date_de_naissance) {
        this.nom = nom;
        this.prenom = prenom;
        this.date_de_naissance = date_de_naissance;
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

    public List<Activity> getActivities() {
        return activities;
    }

    public List<Planing> getPlanings() {
        return planings;
    }

    public Parent getParent() {
        return parent;
    }
    public void setParent(Parent parent) {
      this.parent = parent;
    }

    public Date getDate_de_naissance() {
        return date_de_naissance;
    }

}
