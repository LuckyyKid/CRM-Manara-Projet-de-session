package CRM_Manara.CRM_Manara.dto;

public record ChatParticipantDto(
        Long userId,
        Long profileId,
        String accountType,
        String displayName,
        String email
) {
}
