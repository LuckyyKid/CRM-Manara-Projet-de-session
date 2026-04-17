package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AnimationRequestDto(
        Long activityId,
        Long animateurId,
        String role,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
