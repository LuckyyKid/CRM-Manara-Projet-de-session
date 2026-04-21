package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkAssignmentRepo extends JpaRepository<HomeworkAssignment, Long> {

    @EntityGraph(attributePaths = {"animation", "animation.activity", "exercises", "attempts"})
    List<HomeworkAssignment> findByEnfantParentIdOrderByCreatedAtDesc(Long parentId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises", "attempts"})
    List<HomeworkAssignment> findByAnimateurIdOrderByCreatedAtDesc(Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises", "attempts"})
    Optional<HomeworkAssignment> findByIdAndEnfantParentId(Long id, Long parentId);

    boolean existsBySourceAttemptIdAndEnfantId(Long sourceAttemptId, Long enfantId);
}
