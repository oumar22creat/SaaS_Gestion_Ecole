package com.schoolsaas.billing;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.billing.dto.CheckoutRequest;
import com.schoolsaas.billing.dto.CheckoutResponse;
import com.schoolsaas.billing.dto.InvoiceResponse;
import com.schoolsaas.billing.dto.PlanResponse;
import com.schoolsaas.billing.dto.SubscriptionResponse;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Abonnement et facturation d'un établissement — cahier-des-charges.md §4. */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final PlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final BillingCheckoutService billingCheckoutService;
    private final StripeWebhookService stripeWebhookService;
    private final InvoiceRepository invoiceRepository;

    public BillingController(
            PlanRepository planRepository,
            SubscriptionService subscriptionService,
            BillingCheckoutService billingCheckoutService,
            StripeWebhookService stripeWebhookService,
            InvoiceRepository invoiceRepository) {
        this.planRepository = planRepository;
        this.subscriptionService = subscriptionService;
        this.billingCheckoutService = billingCheckoutService;
        this.stripeWebhookService = stripeWebhookService;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanResponse>> plans() {
        return ApiResponse.of(planRepository.findAll().stream().map(PlanResponse::from).toList());
    }

    @GetMapping("/subscription")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
    public ApiResponse<SubscriptionResponse> subscription() {
        Subscription subscription = subscriptionService.getForTenant(currentTenantId());
        Plan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plan introuvable"));
        return ApiResponse.of(SubscriptionResponse.from(subscription, plan));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        StripeGateway.CheckoutSession session = billingCheckoutService.createCheckoutSession(
                currentTenantId(), request.planCode(), currentEmail());
        return ApiResponse.of(new CheckoutResponse(session.url()));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
    public ApiResponse<List<InvoiceResponse>> invoices(Pageable pageable) {
        Page<Invoice> page = invoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(currentTenantId(), pageable);
        List<InvoiceResponse> data = page.map(InvoiceResponse::from).getContent();
        return ApiResponse.of(data, new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    /**
     * Endpoint public appelé par Stripe (pas de JWT, pas de contexte tenant) : authentifié
     * uniquement par la signature {@code Stripe-Signature} (cahier-des-charges.md §4.3).
     */
    @PostMapping("/webhooks/stripe")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        stripeWebhookService.handle(payload, signature);
    }

    private Long currentTenantId() {
        return TenantContext.get();
    }

    private String currentEmail() {
        AuthenticatedPrincipal principal =
                (AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.email();
    }
}
