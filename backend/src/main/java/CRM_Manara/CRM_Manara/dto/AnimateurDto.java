package CRM_Manara.CRM_Manara.dto;

public record AnimateurDto(
        Long id,
        String nom,
        String prenom,
        UserDto user
) {
}
