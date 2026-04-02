package CRM_Manara.CRM_Manara.dto;

public record ActionResponseDto(
        boolean success,
        String message,
        Long id
) {
}
