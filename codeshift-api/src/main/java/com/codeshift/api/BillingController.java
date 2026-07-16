package com.codeshift.api;

import com.codeshift.bsg.MeteringStore;
import com.codeshift.bsg.PaymentProvider;
import com.codeshift.bsg.PaymentStore;
import com.codeshift.bsg.ProjectStore;
import com.codeshift.bsg.TenantStore;
import com.codeshift.bsg.model.Invoice;
import com.codeshift.gateway.CostEstimator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Usage metering, per-project budgets, and tenant invoices (product doc Phase 6).
 *
 * <p>Every metered LLM call is recorded against a project's budget; a call that
 * would blow the budget is refused with 402 (budgets in code, not surprise
 * invoices). Invoices aggregate usage per project and attach a payment intent from
 * the configured {@link PaymentProvider}. Persistence-gated (503 under {@code nodb}).
 */
@RestController
public class BillingController {

    private final ObjectProvider<MeteringStore> meteringStore;
    private final ObjectProvider<ProjectStore> projectStore;
    private final ObjectProvider<TenantStore> tenantStore;
    private final ObjectProvider<PaymentStore> paymentStore;
    private final PaymentProvider payments;

    public BillingController(ObjectProvider<MeteringStore> meteringStore,
            ObjectProvider<ProjectStore> projectStore, ObjectProvider<TenantStore> tenantStore,
            ObjectProvider<PaymentStore> paymentStore, PaymentProvider payments) {
        this.meteringStore = meteringStore;
        this.projectStore = projectStore;
        this.tenantStore = tenantStore;
        this.paymentStore = paymentStore;
        this.payments = payments;
    }

    private <T> T require(ObjectProvider<T> provider) {
        T s = provider.getIfAvailable();
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Persistence disabled (running with the nodb profile). Start with a database.");
        }
        return s;
    }

    private UUID currentOrg() {
        return TenantContext.current().orElseGet(() -> require(tenantStore).defaultOrgId());
    }

    public record UsageRequest(String projectId, String model, long inputTokens, long outputTokens) {}

    public record UsageResponse(BigDecimal costUsd, BigDecimal budgetUsd, BigDecimal spentUsd,
            BigDecimal remainingUsd) {}

    public record BudgetResponse(BigDecimal budgetUsd, BigDecimal spentUsd, BigDecimal remainingUsd) {}

    public record SetBudgetRequest(BigDecimal budgetUsd) {}

    /** Record a metered LLM call. Refuses (402) if it would exceed the project budget. */
    @PostMapping("/billing/usage")
    public UsageResponse record(@RequestBody UsageRequest req) {
        MeteringStore metering = require(meteringStore);
        ProjectStore projects = require(projectStore);
        UUID projectId = UUID.fromString(req.projectId());
        BigDecimal cost = BigDecimal.valueOf(
                CostEstimator.estimate(req.model(), req.inputTokens(), req.outputTokens()))
                .setScale(6, RoundingMode.HALF_UP);

        if (!projects.withinBudget(projectId, cost)) {
            ProjectStore.Budget b = projects.budget(projectId);
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Project budget exceeded: spent " + b.spentUsd() + " + " + cost
                            + " > budget " + b.budgetUsd() + ". Raise the budget to continue.");
        }

        ProjectStore.Budget after = metering.record(currentOrg(), projectId, req.model(),
                req.inputTokens(), req.outputTokens(), cost);
        return new UsageResponse(cost, after.budgetUsd(), after.spentUsd(),
                after.budgetUsd().subtract(after.spentUsd()));
    }

    @GetMapping("/billing/usage")
    public List<MeteringStore.UsageRecord> usage() {
        return require(meteringStore).listByOrg(currentOrg());
    }

    @GetMapping("/projects/{projectId}/budget")
    public BudgetResponse budget(@PathVariable String projectId) {
        ProjectStore.Budget b = require(projectStore).budget(UUID.fromString(projectId));
        return new BudgetResponse(b.budgetUsd(), b.spentUsd(),
                b.budgetUsd().subtract(b.spentUsd()));
    }

    @PutMapping("/projects/{projectId}/budget")
    public BudgetResponse setBudget(@PathVariable String projectId,
            @RequestBody SetBudgetRequest req) {
        ProjectStore projects = require(projectStore);
        projects.setBudget(UUID.fromString(projectId), req.budgetUsd());
        ProjectStore.Budget b = projects.budget(UUID.fromString(projectId));
        return new BudgetResponse(b.budgetUsd(), b.spentUsd(),
                b.budgetUsd().subtract(b.spentUsd()));
    }

    /** Generate a usage-based invoice for the calling tenant. */
    @GetMapping("/billing/invoice")
    public Invoice invoice() {
        return require(meteringStore).invoiceFor(currentOrg(), payments);
    }

    public record CheckoutResponse(String reference, String provider, String status,
            BigDecimal amountUsd) {}

    /**
     * Start checkout for the tenant's current invoice total: create a payment intent
     * via the provider and persist it as pending, ready for the webhook to settle.
     */
    @PostMapping("/billing/checkout")
    public CheckoutResponse checkout() {
        UUID org = currentOrg();
        Invoice invoice = require(meteringStore).invoiceFor(org, payments);
        Invoice.PaymentIntent intent = invoice.payment();
        require(paymentStore).create(org, intent.id(), intent.provider(),
                intent.status(), invoice.totalUsd());
        return new CheckoutResponse(intent.id(), intent.provider(), intent.status(),
                invoice.totalUsd());
    }

    @GetMapping("/billing/payments")
    public List<PaymentStore.PaymentView> listPayments() {
        return require(paymentStore).listByOrg(currentOrg());
    }
}
