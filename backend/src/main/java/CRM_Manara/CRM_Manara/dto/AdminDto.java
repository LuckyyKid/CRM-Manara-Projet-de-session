package CRM_Manara.CRM_Manara.dto;

public record AdminDto(
        Long id,
        String nom,
        String prenom,
        String role,
        String status,
        UserDto user
) {
}
