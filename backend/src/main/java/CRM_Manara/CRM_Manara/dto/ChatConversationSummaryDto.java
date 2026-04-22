package CRM_Manara.CRM_Manara.dto;

import java.util.Date;

public record ChatConversationSummaryDto(
        Long id,
        ChatParticipantDto participant,
        String lastMessagePreview,
        Date lastMessageAt,
        long unreadCount
) {
}
