package CRM_Manara.CRM_Manara.dto;

public record SendChatMessageRequestDto(
        Long conversationId,
        Long recipientUserId,
        String body
) {
}
