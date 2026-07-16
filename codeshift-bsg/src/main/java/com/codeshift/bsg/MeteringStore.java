package com.codeshift.bsg;

import com.codeshift.bsg.entity.UsageEventEntity;
import com.codeshift.bsg.model.Invoice;
import com.codeshift.bsg.repo.UsageEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records metered LLM usage and turns it into budgets + invoices — the
 * cost-accounting layer the architecture ties billing to.
 *
 * <p>Cost is computed by the caller (via the model gateway's CostEstimator, which
 * lives in another module) and passed in, so this store stays a pure persistence +
 * aggregation concern with no gateway dependency.
 */
public class MeteringStore {

    private final UsageEventRepository usage;
    private final ProjectStore projects;

    public MeteringStore(UsageEventRepository usage, ProjectStore projects) {
        this.usage = usage;
        this.projects = projects;
    }

    /**
     * Record one metered call and add its cost to the project's running spend.
     * Returns the resulting budget snapshot so callers can surface headroom.
     */
    @Transactional
    public ProjectStore.Budget record(UUID orgId, UUID projectId, String model,
            long inputTokens, long outputTokens, BigDecimal costUsd) {
        UsageEventEntity e = new UsageEventEntity();
        e.setOrgId(orgId);
        e.setProjectId(projectId);
        e.setModel(model);
        e.setInputTokens(inputTokens);
        e.setOutputTokens(outputTokens);
        e.setCostUsd(costUsd);
        usage.save(e);
        projects.addSpend(projectId, costUsd);
        return projects.budget(projectId);
    }

    @Transactional(readOnly = true)
    public List<UsageRecord> listByOrg(UUID orgId) {
        return usage.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                .map(MeteringStore::toRecord).toList();
    }

    /**
     * Build a tenant invoice: aggregate every usage event into one line item per
     * project, total it, and attach a payment intent from the provider.
     */
    @Transactional(readOnly = true)
    public Invoice invoiceFor(UUID orgId, PaymentProvider payments) {
        Map<UUID, Agg> byProject = new LinkedHashMap<>();
        for (UsageEventEntity e : usage.findByOrgIdOrderByCreatedAtDesc(orgId)) {
            byProject.computeIfAbsent(e.getProjectId(), k -> new Agg()).add(e);
        }

        List<Invoice.LineItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, Agg> entry : byProject.entrySet()) {
            Agg a = entry.getValue();
            String name = safeName(entry.getKey());
            BigDecimal amount = a.cost.setScale(4, RoundingMode.HALF_UP);
            items.add(new Invoice.LineItem(entry.getKey().toString(), name,
                    a.inTok, a.outTok, a.calls, amount));
            total = total.add(amount);
        }
        total = total.setScale(4, RoundingMode.HALF_UP);

        Invoice.PaymentIntent intent = payments.createIntent(orgId.toString(), total,
                "CodeShift usage — " + items.size() + " project(s)");
        return new Invoice(orgId.toString(), "USD", items, total, intent);
    }

    private String safeName(UUID projectId) {
        try {
            return projects.get(projectId).name();
        } catch (RuntimeException ex) {
            return projectId.toString();
        }
    }

    private static UsageRecord toRecord(UsageEventEntity e) {
        return new UsageRecord(e.getId().toString(), e.getProjectId().toString(), e.getModel(),
                e.getInputTokens(), e.getOutputTokens(), e.getCostUsd());
    }

    /** A recorded usage event, for the billing dashboard. */
    public record UsageRecord(String id, String projectId, String model,
            long inputTokens, long outputTokens, BigDecimal costUsd) {}

    private static final class Agg {
        long inTok;
        long outTok;
        int calls;
        BigDecimal cost = BigDecimal.ZERO;

        void add(UsageEventEntity e) {
            inTok += e.getInputTokens();
            outTok += e.getOutputTokens();
            calls++;
            cost = cost.add(e.getCostUsd());
        }
    }
}
