package CRM_Manara.CRM_Manara.dto;

public record ActivityRequestDto(
        String name,
        String description,
        int ageMin,
        int ageMax,
        int capacity,
        String status,
        String type
) {
}
