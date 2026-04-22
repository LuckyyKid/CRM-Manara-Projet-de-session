package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ActivityRepo extends JpaRepository<Activity, Long> {

    @Query("SELECT a FROM Activity a ORDER BY LOWER(a.activyName)")
    List<Activity> findAllOrderedByName();

    long countByStatus(status status);
}
