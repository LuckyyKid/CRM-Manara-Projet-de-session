package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringQuestionRepository extends JpaRepository<TutoringQuestion, Long> {
    List<TutoringQuestion> findByAxisId(Long axisId);
    List<TutoringQuestion> findByAxisSessionId(Long sessionId);
    void deleteByAxisSessionId(Long sessionId);
}
