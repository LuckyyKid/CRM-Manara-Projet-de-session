package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnimateurRepo extends JpaRepository<Animateur, Long> {
    Optional<Animateur> findByUserEmail(String email);
}
