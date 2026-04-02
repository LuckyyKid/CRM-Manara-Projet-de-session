package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AnimationDto(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String role,
        String status,
        ActivityDto activity,
        AnimateurSummaryDto animateur
) {
}
