package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.Repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(userService.class);

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

    @Autowired
    private EmailService emailService;

    @Autowired
    private ParentNotificationService parentNotificationService;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email.trim()).orElseThrow(() -> {
            return new UsernameNotFoundException("Pas trouve");
        });
        logger.debug("Authentification chargée pour {} avec rôle {}", user.getEmail(), user.getRole());

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
    @Transactional(noRollbackFor = OAuth2AuthenticationException.class)
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

        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_pending"),
                    "Compte en attente d'approbation par l'administration."
            );
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

        // enabled = false : le compte est en attente d'approbation par l'admin
        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()), SecurityRole.ROLE_PARENT, false);
        User savedUser = userRepo.save(user);
        avatarService.assignDefaultAvatar(savedUser, fullName);

        Parent parent = new Parent(nameParts[1], nameParts[0], "");
        parent.SetUser(savedUser);
        Parent savedParent = parentRepo.save(parent);

        parentNotificationService.createForParent(
                savedParent,
                "COMPTE",
                "Compte créé",
                "Votre compte parent a été créé avec Google. Il est en attente d'approbation par l'administration."
        );
        emailService.sendEmail(
                savedUser.getEmail(),
                "Compte parent en attente d'approbation - CRM Manara",
                "Bonjour,\n\nVotre compte parent a été créé avec Google.\n"
                        + "Il est maintenant en attente d'approbation par l'administration.\n"
                        + "Tant qu'il n'est pas approuvé, vous ne pourrez pas accéder au portail.\n\n"
                        + "Compte: " + savedUser.getEmail() + "\n\nMerci,\nCRM Manara"
        );
        emailService.notifyAdminsOfParentSignup(fullName, savedUser.getEmail(), "Google");
        adminNotificationService.create(
                "PARENT",
                "COMPTE",
                "Nouveau compte parent Google en attente: " + fullName + " (" + savedUser.getEmail() + ")."
        );

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
