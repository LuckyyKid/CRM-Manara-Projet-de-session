package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepo extends JpaRepository<Booking, Long> {

    @Query("""
            select distinct b
            from Booking b
            join fetch b.animateur a
            left join fetch a.user
            join fetch b.parent p
            left join fetch p.user
            left join fetch p.enfants
            where a.id = :animateurId
            order by b.startTime desc
            """)
    List<Booking> findForAnimateur(@Param("animateurId") Long animateurId);

    @Query("""
            select distinct b
            from Booking b
            join fetch b.animateur a
            left join fetch a.user
            join fetch b.parent p
            left join fetch p.user
            left join fetch p.enfants
            where p.id = :parentId
            order by b.startTime desc
            """)
    List<Booking> findForParent(@Param("parentId") Long parentId);

    Optional<Booking> findFirstBySlotIdAndStatusOrderByCreatedAtDesc(Long slotId, String status);

    boolean existsBySlotIdAndStatus(Long slotId, String status);

    void deleteBySlotId(Long slotId);

    List<Booking> findBySlotId(Long slotId);
}
