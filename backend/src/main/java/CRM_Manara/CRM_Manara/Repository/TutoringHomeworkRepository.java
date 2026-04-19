package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringHomework;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringHomeworkRepository extends JpaRepository<TutoringHomework, Long> {
    List<TutoringHomework> findByStudentIdAndStatus(Long studentId, String status);
    List<TutoringHomework> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    void deleteByAxisSessionId(Long sessionId);
}
