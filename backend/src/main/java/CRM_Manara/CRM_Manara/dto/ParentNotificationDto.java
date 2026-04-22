package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record ParentNotificationDto(
        Long id,
        String category,
        String title,
        String message,
        LocalDateTime createdAt,
        boolean readStatus,
        boolean archivedStatus
) {
}
