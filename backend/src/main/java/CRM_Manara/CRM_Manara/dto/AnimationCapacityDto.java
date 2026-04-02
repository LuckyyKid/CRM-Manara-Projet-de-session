package CRM_Manara.CRM_Manara.dto;

public record AnimationCapacityDto(
        Long animationId,
        int approved,
        int pending,
        int capacity,
        int remaining,
        int waitlist,
        int fillRate,
        boolean full,
        int waitlistPosition
) {
}
