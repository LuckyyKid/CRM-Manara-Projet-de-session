package CRM_Manara.CRM_Manara.dto;

import java.time.Instant;

public record SubscriptionDto(
        String status,
        boolean active,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        String provider,
        int coveredChildrenCount,
        int pendingCoveredChildrenCount,
        long firstChildMonthlyAmountCents,
        long additionalChildMonthlyAmountCents
) {
}
