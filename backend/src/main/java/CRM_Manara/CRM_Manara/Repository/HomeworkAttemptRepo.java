package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.HomeworkAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeworkAttemptRepo extends JpaRepository<HomeworkAttempt, Long> {

    @EntityGraph(attributePaths = {"assignment", "assignment.animation", "assignment.animation.activity", "answers", "answers.exercise"})
    List<HomeworkAttempt> findByEnfantParentIdOrderBySubmittedAtDesc(Long parentId);

    @EntityGraph(attributePaths = {"assignment", "assignment.animation", "assignment.animation.activity", "answers", "answers.exercise"})
    Optional<HomeworkAttempt> findByIdAndEnfantParentId(Long id, Long parentId);
}
