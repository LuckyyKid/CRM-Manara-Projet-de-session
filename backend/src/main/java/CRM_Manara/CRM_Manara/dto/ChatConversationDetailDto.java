package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record ChatConversationDetailDto(
        Long id,
        ChatParticipantDto participant,
        long unreadCount,
        List<ChatMessageDto> messages
) {
}
