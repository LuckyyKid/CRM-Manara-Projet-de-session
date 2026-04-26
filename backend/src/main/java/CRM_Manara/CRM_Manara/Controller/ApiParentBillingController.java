package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.BillingChildCoverageDto;
import CRM_Manara.CRM_Manara.dto.CheckoutSessionRequestDto;
import CRM_Manara.CRM_Manara.dto.CheckoutSessionDto;
import CRM_Manara.CRM_Manara.dto.SubscriptionDto;
import CRM_Manara.CRM_Manara.dto.UpdateCoveredChildrenRequestDto;
import CRM_Manara.CRM_Manara.service.BillingService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parent/billing")
public class ApiParentBillingController {

    private final BillingService billingService;

    public ApiParentBillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/subscription")
    public SubscriptionDto subscription(Authentication authentication) {
        return billingService.getSubscriptionStatus(requireEmail(authentication));
    }

    @PostMapping("/checkout")
    public CheckoutSessionDto checkout(Authentication authentication, @RequestBody(required = false) CheckoutSessionRequestDto request) {
        int coveredChildrenCount = request == null ? 1 : request.coveredChildrenCount();
        return billingService.createCheckoutSession(requireEmail(authentication), coveredChildrenCount);
    }

    @GetMapping("/covered-children")
    public List<BillingChildCoverageDto> coveredChildren(Authentication authentication) {
        return billingService.listChildCoverage(requireEmail(authentication));
    }

    @PutMapping("/covered-children")
    public List<BillingChildCoverageDto> updateCoveredChildren(Authentication authentication,
                                                               @RequestBody UpdateCoveredChildrenRequestDto request) {
        return billingService.updateCoveredChildren(requireEmail(authentication), request == null ? List.of() : request.enfantIds());
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Utilisateur non authentifie.");
        }
        return authentication.getName();
    }
}
