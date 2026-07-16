package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Razorpay webhook lifecycle against a real (H2) database: check out an invoice,
 * then settle it via a signed webhook — and prove a forged (bad-signature) webhook
 * is rejected and never marks a payment paid.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:webhook;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "codeshift.billing.razorpay.webhook-secret=whsec_test_secret",
})
@AutoConfigureMockMvc
class WebhookFlowTest {

    private static final String SECRET = "whsec_test_secret";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String field(String body, String field) throws Exception {
        return json.readTree(body).get(field).asText();
    }

    @Test
    void checkoutThenSignedWebhookSettlesPayment() throws Exception {
        String org = field(mvc.perform(post("/orgs")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"acme\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "orgId");
        String project = field(mvc.perform(post("/projects")
                        .header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme-orders\",\"sourceLanguage\":\"JAVA_8\","
                                + "\"targetStack\":\"JAVA_21_SPRING_BOOT\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "projectId");

        // Some metered usage so the invoice has a non-zero total.
        mvc.perform(post("/billing/usage").header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + project + "\",\"model\":\"claude-haiku-4-5-20251001\","
                                + "\"inputTokens\":100000,\"outputTokens\":20000}"))
                .andExpect(status().isOk());

        // Checkout creates a pending payment.
        String reference = field(mvc.perform(post("/billing/checkout").header(TenantFilter.TENANT_HEADER, org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MANUAL_PENDING"))
                .andReturn().getResponse().getContentAsString(), "reference");

        // A genuine Razorpay webhook carrying our reference, signed with the shared secret.
        String payload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":"
                + "{\"id\":\"pay_123\",\"notes\":{\"reference\":\"" + reference + "\"}}}}}";
        String signature = RazorpaySignatures.sign(payload, SECRET);

        mvc.perform(post("/billing/webhook/razorpay")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handled").value(true))
                .andExpect(jsonPath("$.status").value("PAID"));

        // The payment is now PAID.
        mvc.perform(get("/billing/payments").header(TenantFilter.TENANT_HEADER, org))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference").value(reference))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        // A forged webhook (wrong signature) is rejected — payment stays PAID, not overwritten.
        mvc.perform(post("/billing/webhook/razorpay")
                        .header("X-Razorpay-Signature", "deadbeef")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }
}
