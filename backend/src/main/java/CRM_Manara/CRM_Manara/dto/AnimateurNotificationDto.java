package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AnimateurNotificationDto(
        Long id,
        String category,
        String title,
        String message,
        LocalDateTime createdAt,
        boolean read,
        boolean archived
) {
}
