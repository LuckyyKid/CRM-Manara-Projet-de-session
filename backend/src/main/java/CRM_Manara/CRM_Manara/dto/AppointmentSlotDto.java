package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AppointmentSlotDto(
        Long id,
        Long animateurUserId,
        String animateurName,
        Long parentUserId,
        String parentName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        LocalDateTime bookedAt
) {
}
