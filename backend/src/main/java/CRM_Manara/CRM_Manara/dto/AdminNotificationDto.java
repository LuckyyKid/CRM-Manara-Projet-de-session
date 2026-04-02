package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AdminNotificationDto(
        Long id,
        String source,
        String type,
        String message,
        LocalDateTime createdAt
) {
}
