package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Enfant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="nom" , nullable = false)
    @NotBlank(message = "Champ obligatoire")
    private String nom;

    @Column(name ="prenom" , nullable = false)
    @NotBlank(message = "Champ obligatoire")
    private String prenom;

    @Temporal(TemporalType.DATE)
    @Column(name ="date_de_naissance" , nullable = false)
    @NotBlank(message = "Champ obligatoire")
    private Date date_de_naissance;

    @OneToMany(mappedBy = "enfant",cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Inscription> inscriptions = new ArrayList<>();



    @ManyToOne
    @JoinColumn(name = "Parent_id")
    private Parent parent;

    public Enfant() {

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
    public List<Inscription> getInscriptions() {
        return inscriptions;
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
    public void setDate_de_naissance(Date date_de_naissance) {
        this.date_de_naissance = date_de_naissance;
    }

}
