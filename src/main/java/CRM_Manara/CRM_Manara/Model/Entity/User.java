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

        // ADDED
        @Column(nullable = false)
        private boolean enabled = false;

        @Column(name = "avatar_url", length = 4096)
        private String avatarUrl;

        protected User() {}

        public User(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public User(String email, String password, SecurityRole role) {
            this.email = email;
            this.password = password;
            this.role = role;
        }

        // ADDED
        public User(String email, String password, SecurityRole role, boolean enabled) {
            this.email = email;
            this.password = password;
            this.role = role;
            this.enabled = enabled;
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

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public SecurityRole getRole() {
            return role;
        }
        public void setRole(SecurityRole role) {
            this.role = role;
        }

        // ADDED
        public boolean isEnabled() {
            return enabled;
        }

        // ADDED
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }
    }
