package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AnimationSummaryDto(
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String role,
        String status,
        ActivitySummaryDto activity,
        AnimateurSummaryDto animateur
) {
}
