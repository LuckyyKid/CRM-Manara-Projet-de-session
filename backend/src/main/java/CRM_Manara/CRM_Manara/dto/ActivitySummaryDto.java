package CRM_Manara.CRM_Manara.dto;

public record ActivitySummaryDto(
        Long id,
        String name,
        int ageMin,
        int ageMax,
        int capacity,
        String status,
        String type
) {
}
