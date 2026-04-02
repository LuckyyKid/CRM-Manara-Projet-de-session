package CRM_Manara.CRM_Manara.dto;

public record UserDto(
        Long id,
        String email,
        String role,
        boolean enabled,
        String avatarUrl
) {
}
