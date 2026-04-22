package CRM_Manara.CRM_Manara.dto;

public record AppointmentSlotCreateDto(
        String startTime,
        String endTime,
        String status
) {
}
