package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AccountStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    UserRepo userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AdminRepo adminRepo;

    @Override
    public void run(String... args) {

        User user = userRepository.findByEmail("admin@manara.com")
                .orElseGet(() -> {

                    User newUser = new User(
                            "admin@manara.com",
                            passwordEncoder.encode("Admin123!"),
                            SecurityRole.ROLE_ADMIN
                    );

                    return userRepository.save(newUser);
                });

        if (adminRepo.findByUser(user).isEmpty()) {

            Administrateurs admin = new Administrateurs("Steven","Chaussé", new Date());

            admin.setUser(user);
            admin.setRole(SecurityRole.ROLE_ADMIN);
            admin.setStatus(AccountStatus.ACTIF);

            adminRepo.save(admin);
        }
    }
}