package CRM_Manara.CRM_Manara.dto;

public record CurrentUserDto(
        Long id,
        String accountType,
        UserDto user,
        ParentDto parent,
        AnimateurDto animateur,
        AdminDto admin
) {
}
