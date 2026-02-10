package CRM_Manara.CRM_Manara.Entity;

import CRM_Manara.CRM_Manara.Enum.AccountStatus;
import CRM_Manara.CRM_Manara.Enum.AdminRole;
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

        @Column(unique = true, nullable = false)
        private String email;

        @Column(nullable = false)
        private String password;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AdminRole role;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AccountStatus status;

        @Temporal(TemporalType.TIMESTAMP)
        @Column(nullable = false, updatable = false)
        private Date dateCreation;

        @Temporal(TemporalType.TIMESTAMP)
        private Date lastLogin;

        @PrePersist
        private void onCreate() {
            this.dateCreation = new Date();
            this.status = AccountStatus.ACTIF;
            this.role = AdminRole.ADMIN;
        }

       protected Administrateurs() {

       }

       public Administrateurs(String nom, String prenom, String email, String password) {
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
            this.password = password;
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
       public String getEmail() {
          return email;
       }
       public void setEmail(String email) {
            this.email = email;
       }
       public String getPassword() {
            return password;
       }

       public void setPassword(String password) {
            this.password = password;
       }
       public AdminRole getRole() {
            return role;
       }
       public void setRole(AdminRole role) {
            this.role = role;
       }
       public AccountStatus getStatus() {
            return status;
       }
       public void setStatus(AccountStatus status) {
            this.status = status;
       }
       public Date getDateCreation() {
            return dateCreation;
       }

       public Date getLastLogin() {
            return lastLogin;
       }




}
