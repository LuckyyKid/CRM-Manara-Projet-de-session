package CRM_Manara.CRM_Manara.dto;

import java.util.Date;

public record ChatMessageDto(
        Long id,
        Long conversationId,
        ChatParticipantDto sender,
        ChatParticipantDto recipient,
        String body,
        Date createdAt,
        boolean mine,
        boolean readStatus
) {
}
