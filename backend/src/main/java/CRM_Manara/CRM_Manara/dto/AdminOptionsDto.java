package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AdminOptionsDto(
        List<String> activityStatuses,
        List<String> activityTypes,
        List<String> animationRoles,
        List<String> animationStatuses
) {
}
