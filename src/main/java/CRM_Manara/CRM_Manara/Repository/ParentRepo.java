package CRM_Manara.CRM_Manara.Repository;


import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentRepo extends JpaRepository<Parent, Long> {

    @Query("SELECT p FROM Parent p WHERE p.user.email = :email")
    Optional<Parent> findByUserEmail(@Param("email") String email);
}
