package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private final BillingService billingService;

    public StripeWebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature) {
        billingService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
