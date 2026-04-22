package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingDto(
        Long id,
        Long slotId,
        Long animateurUserId,
        String animateurName,
        Long parentUserId,
        String parentName,
        String childName,
        LocalDate date,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime cancelledAt
) {
}
