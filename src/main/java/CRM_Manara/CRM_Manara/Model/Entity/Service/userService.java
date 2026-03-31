package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.Repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class userService implements UserDetailsService, OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UserRepo userRepo;

    // ADDED
    @Autowired
    private ParentRepo parentRepo;

    // ADDED
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ADDED
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private AvatarService avatarService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        System.out.println("Tentative de connexion pour l'email : " + email);

        User user = userRepo.findByEmail(email.trim()).orElseThrow(() -> {
            System.out.println("ERREUR : Aucun utilisateur trouve en DB pour : " + email);
            return new UsernameNotFoundException("Pas trouve");
        });

        // ADDED
        if (!user.isEnabled() && !verificationTokenRepository.existsByUser(user)) {
            user.setEnabled(true);
            user = userRepo.save(user);
        }

        System.out.println("Utilisateur trouve ! Son mot de passe hashe est : " + user.getPassword());
        System.out.println("Son role est : " + user.getRole());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                // ADDED
                .disabled(!user.isEnabled())
                .authorities(user.getRole().name())
                .build();
    }

    // ADDED
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Email Google introuvable"
            );
        }

        User user = userRepo.findByEmail(email.trim())
                .orElseGet(() -> createGoogleParent(email.trim(), name));

        avatarService.assignOAuthAvatar(
                user,
                name,
                oAuth2User.getAttribute("picture")
        );

        // ADDED
        if (!user.isEnabled()) {
            user.setEnabled(true);
            user = userRepo.save(user);
        }

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                oAuth2User.getAttributes(),
                "email"
        );
    }

    // ADDED
    private User createGoogleParent(String email, String fullName) {
        String[] nameParts = splitName(fullName);

        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), SecurityRole.ROLE_PARENT, true);
        User savedUser = userRepo.save(user);
        avatarService.assignDefaultAvatar(savedUser, fullName);

        Parent parent = new Parent(nameParts[1], nameParts[0], "");
        parent.SetUser(savedUser);
        parentRepo.save(parent);

        return savedUser;
    }

    // ADDED
    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"Google", "Utilisateur"};
        }

        String normalized = fullName.trim().replaceAll("\\s+", " ");
        int separatorIndex = normalized.indexOf(' ');
        if (separatorIndex < 0) {
            return new String[]{normalized, normalized};
        }

        String prenom = normalized.substring(0, separatorIndex);
        String nom = normalized.substring(separatorIndex + 1);
        return new String[]{prenom, nom};
    }
}
