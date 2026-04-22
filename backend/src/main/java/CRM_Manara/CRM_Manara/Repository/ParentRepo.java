package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepo extends JpaRepository<Parent, Long> {

    @EntityGraph(attributePaths = {"user", "enfants"})
    Optional<Parent> findByUser(User user);

    @EntityGraph(attributePaths = {"user", "enfants"})
    @Query("SELECT p FROM Parent p WHERE p.user.email = :email")
    Optional<Parent> findByUserEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"user", "enfants"})
    @Query("SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.enfants")
    List<Parent> findAllWithUserAndEnfants();

    long countByUserEnabled(boolean enabled);
}
