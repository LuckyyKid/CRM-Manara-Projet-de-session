package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class userService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        System.out.println("Tentative de connexion pour l'email : " + email);

        User user = userRepo.findByEmail(email.trim()).orElseThrow(() -> {
                    System.out.println("ERREUR : Aucun utilisateur trouvé en DB pour : " + email);
                    return new UsernameNotFoundException("Pas trouvé");
                });

        System.out.println("Utilisateur trouvé ! Son mot de passe hashé est : " + user.getPassword());
        System.out.println("Son rôle est : "+ user.getRole());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();
    }
}
