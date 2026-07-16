package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Usage metering → budgets → invoice, against a real (H2) database: record metered
 * calls, watch spend accrue, get refused (402) when a call would blow the budget,
 * then generate a tenant invoice with a payment intent.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:billing;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class BillingFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void meterBudgetGuardrailThenInvoice() throws Exception {
        // Tenant + project.
        String org = json.readTree(mvc.perform(post("/orgs")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"acme\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("orgId").asText();
        String project = json.readTree(mvc.perform(post("/projects")
                        .header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme-orders\",\"sourceLanguage\":\"JAVA_8\","
                                + "\"targetStack\":\"JAVA_21_SPRING_BOOT\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("projectId").asText();

        // Tight $1 budget so the guardrail is reachable.
        mvc.perform(put("/projects/" + project + "/budget")
                        .header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"budgetUsd\":1.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingUsd").value(1.00));

        // A metered opus call: 100k in + 20k out ≈ $1.50 + $1.50 = $3.00 > $1 → refused.
        mvc.perform(post("/billing/usage")
                        .header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + project + "\",\"model\":\"claude-opus-4-8\","
                                + "\"inputTokens\":100000,\"outputTokens\":20000}"))
                .andExpect(status().isPaymentRequired());

        // A cheap haiku call fits the budget and is recorded.
        MvcResult recorded = mvc.perform(post("/billing/usage")
                        .header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + project + "\",\"model\":\"claude-haiku-4-5-20251001\","
                                + "\"inputTokens\":100000,\"outputTokens\":20000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentUsd").isNotEmpty())
                .andReturn();
        double spent = json.readTree(recorded.getResponse().getContentAsString()).get("spentUsd").asDouble();
        assertThat(spent).isGreaterThan(0.0).isLessThan(1.0);

        // The invoice aggregates that project's usage and carries a manual payment intent.
        mvc.perform(get("/billing/invoice").header(TenantFilter.TENANT_HEADER, org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.lineItems.length()").value(1))
                .andExpect(jsonPath("$.lineItems[0].projectName").value("acme-orders"))
                .andExpect(jsonPath("$.payment.provider").value("manual"))
                .andExpect(jsonPath("$.payment.status").value("MANUAL_PENDING"));
    }
}
