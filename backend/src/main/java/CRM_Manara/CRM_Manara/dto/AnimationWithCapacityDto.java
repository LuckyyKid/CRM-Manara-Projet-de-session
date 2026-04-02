package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AnimationWithCapacityDto(
        AnimationDto animation,
        AnimationCapacityDto capacity,
        List<EnfantSummaryDto> enrolledChildren
) {
}
