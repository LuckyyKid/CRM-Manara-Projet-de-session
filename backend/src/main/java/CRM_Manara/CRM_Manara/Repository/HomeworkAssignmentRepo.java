package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkAssignmentRepo extends JpaRepository<HomeworkAssignment, Long> {

    @EntityGraph(attributePaths = {"animation", "animation.activity", "exercises"})
    List<HomeworkAssignment> findByEnfantParentIdOrderByCreatedAtDesc(Long parentId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises"})
    List<HomeworkAssignment> findByAnimateurIdOrderByCreatedAtDesc(Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises"})
    List<HomeworkAssignment> findByAnimateurIdAndEnfantIdOrderByCreatedAtDesc(Long animateurId, Long enfantId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises"})
    Optional<HomeworkAssignment> findByIdAndEnfantParentId(Long id, Long parentId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "enfant", "exercises"})
    Optional<HomeworkAssignment> findByIdAndAnimateurId(Long id, Long animateurId);

    boolean existsBySourceAttemptIdAndEnfantId(Long sourceAttemptId, Long enfantId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "exercises"})
    Optional<HomeworkAssignment> findBySourceAttemptIdAndEnfantParentId(Long sourceAttemptId, Long parentId);

    List<HomeworkAssignment> findBySourceQuizId(Long sourceQuizId);
}
