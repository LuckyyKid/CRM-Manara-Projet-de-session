package CRM_Manara.CRM_Manara.dto;

public record AuthResponseDto(
        String token,
        CurrentUserDto currentUser
) {
}
