package CRM_Manara.CRM_Manara.dto;

public record ActivityDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        int ageMin,
        int ageMax,
        int capacity,
        String status,
        String type
) {
}
