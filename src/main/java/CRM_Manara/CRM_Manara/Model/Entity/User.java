package CRM_Manara.CRM_Manara.Model.Entity;


import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import jakarta.persistence.*;

import java.util.Date;

@Entity
    @Table(name = "users")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "email" ,unique = true, nullable = false)
        private String email;

        @Column(name = "password",nullable = false)
        private String password;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private SecurityRole role;

        @Temporal(TemporalType.TIMESTAMP)
        private Date dateCreation;

        protected User() {}

        public User(String email, String password, SecurityRole role) {
            this.email = email;
            this.password = password;
            this.role = role;
        }

        @PrePersist
        private void onCreate() {
            this.dateCreation = new Date();
        }



        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public SecurityRole getRole() {
            return role;
        }
    }


