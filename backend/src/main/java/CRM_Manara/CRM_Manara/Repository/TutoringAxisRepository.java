package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringAxis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringAxisRepository extends JpaRepository<TutoringAxis, Long> {
    List<TutoringAxis> findBySessionId(Long sessionId);
    void deleteBySessionId(Long sessionId);
}
