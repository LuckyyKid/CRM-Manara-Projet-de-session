package CRM_Manara.CRM_Manara.dto;

import java.time.Instant;
import java.util.List;

public record AdminSubscriptionRowDto(
        Long parentId,
        String parentName,
        String email,
        String status,
        boolean active,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        int coveredChildrenCount,
        List<String> coveredChildren
) {
}
