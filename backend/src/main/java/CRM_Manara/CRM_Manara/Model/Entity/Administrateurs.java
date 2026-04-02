package CRM_Manara.CRM_Manara.Model.Entity;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.AccountStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "Administrateurs")

public class Administrateurs {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String nom;

        @Column(nullable = false)
        private String prenom;

        @OneToOne
        @JoinColumn(name = "user_id")
        private User user;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private SecurityRole role;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AccountStatus status;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name = "dateCreation", nullable = false, updatable = false)
        private Date dateCreation;

        @Temporal(TemporalType.TIMESTAMP)
        private Date lastLogin;

        @PrePersist
        private void onCreate() {
            this.dateCreation = new Date();
            this.status = AccountStatus.ACTIF;
        }

       protected Administrateurs() {

       }

       public Administrateurs(String nom, String prenom) {
            this.nom = nom;
            this.prenom = prenom;
       }
    public Administrateurs(String nom, String prenom,Date dateCreation) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateCreation = dateCreation;
        this.status = AccountStatus.ACTIF;
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

       public SecurityRole getRole() {
            return role;
       }
       public void setRole(SecurityRole role) {
            this.role = role;
       }
       public AccountStatus getStatus() {
            return status;
       }
       public void setStatus(AccountStatus status) {
            this.status = status;
       }
       public void setDateCreation(Date dateCreation) {
            this.dateCreation = dateCreation;
       }
       public Date getDateCreation() {
            return dateCreation;
       }

       public Date getLastLogin() {
            return lastLogin;
       }
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }




}
