package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {

    @EntityGraph(attributePaths = {"quiz", "quiz.animation", "quiz.animation.activity", "enfant", "answers", "answers.question"})
    List<QuizAttempt> findByEnfantParentIdOrderBySubmittedAtDesc(Long parentId);

    boolean existsByQuizIdAndEnfantId(Long quizId, Long enfantId);
}
