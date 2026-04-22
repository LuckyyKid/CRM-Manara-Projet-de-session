package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnfantRepo extends JpaRepository<Enfant, Long> {

    @Override
    @EntityGraph(attributePaths = {"parent", "parent.user"})
    List<Enfant> findAll();

    @EntityGraph(attributePaths = {"parent", "parent.user"})
    @Query("SELECT e FROM Enfant e WHERE e.parent.id = :parentId")
    List<Enfant> findByParentId(@Param("parentId") Long parentId);

    @EntityGraph(attributePaths = {"parent", "parent.user"})
    @Query("SELECT e FROM Enfant e WHERE e.id = :id AND e.parent.id = :parentId")
    Optional<Enfant> findByIdAndParentId(@Param("id") Long id, @Param("parentId") Long parentId);

    long countByActive(boolean active);

    long countByParentId(Long parentId);
}
