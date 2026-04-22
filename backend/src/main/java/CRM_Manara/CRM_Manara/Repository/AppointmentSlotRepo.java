package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.AppointmentSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentSlotRepo extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByAnimateurIdOrderByStartTimeAsc(Long animateurId);

    List<AppointmentSlot> findByAnimateurIdAndStatusOrderByStartTimeAsc(Long animateurId, String status);
}
