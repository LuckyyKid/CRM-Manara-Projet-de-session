package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // ADDED
    Optional<VerificationToken> findByToken(String token);

    // ADDED
    void deleteByUser(User user);

    // ADDED
    boolean existsByUser(User user);
}
