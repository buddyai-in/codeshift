package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * On-prem / in-VPC model deployment against a real (H2) database: a tenant routes
 * its model calls to a private endpoint with a BYOK key; another tenant keeps the
 * managed cloud default; and the endpoint never leaks the key.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:deploy;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class ModelDeploymentFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String createOrg(String name) throws Exception {
        return json.readTree(mvc.perform(post("/orgs")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("orgId").asText();
    }

    @Test
    void tenantRoutesToInVpcEndpointWithByok() throws Exception {
        String acme = createOrg("acme");
        String globex = createOrg("globex");

        // Acme configures an in-VPC deployment...
        mvc.perform(put("/tenants/model-deployment").header(TenantFilter.TENANT_HEADER, acme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deploymentType\":\"IN_VPC\",\"provider\":\"acme-vllm\","
                                + "\"baseUrl\":\"https://models.acme.internal/v1\","
                                + "\"model\":\"llama-3.1-70b-instruct\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IN_VPC"))
                .andExpect(jsonPath("$.baseUrl").value("https://models.acme.internal/v1"))
                .andExpect(jsonPath("$.byokConfigured").value(false));

        // ...then adds a BYOK key for that provider.
        mvc.perform(put("/tenants/keys/acme-vllm").header(TenantFilter.TENANT_HEADER, acme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-acme-vllm-secret\"}"))
                .andExpect(status().isOk());

        // The resolved deployment now shows BYOK configured — but never the key.
        String resolved = mvc.perform(get("/tenants/model-deployment").header(TenantFilter.TENANT_HEADER, acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IN_VPC"))
                .andExpect(jsonPath("$.model").value("llama-3.1-70b-instruct"))
                .andExpect(jsonPath("$.byokConfigured").value(true))
                .andReturn().getResponse().getContentAsString();
        Assertions.assertThat(resolved).doesNotContain("sk-acme-vllm-secret");

        // Globex has no deployment → managed cloud default.
        mvc.perform(get("/tenants/model-deployment").header(TenantFilter.TENANT_HEADER, globex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CLOUD"))
                .andExpect(jsonPath("$.provider").value("platform"))
                .andExpect(jsonPath("$.byokConfigured").value(false));
    }

    @Test
    void onPremRequiresABaseUrl() throws Exception {
        String org = createOrg("needs-url");
        mvc.perform(put("/tenants/model-deployment").header(TenantFilter.TENANT_HEADER, org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deploymentType\":\"ON_PREM\",\"provider\":\"p\",\"model\":\"m\"}"))
                .andExpect(status().isBadRequest());
    }
}
