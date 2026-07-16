package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeshift.bsg.TenantSecretStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BYOK model keys: encrypted at rest, never returned in plaintext, and isolated
 * per tenant. Against a real (H2) database with the real AES-GCM cipher.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:byok;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class TenantKeyFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private TenantSecretStore secrets;

    private String createOrg(String name) throws Exception {
        return json.readTree(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/orgs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("orgId").asText();
    }

    @Test
    void byokKeysAreEncryptedIsolatedAndNeverEchoed() throws Exception {
        String acme = createOrg("acme");
        String globex = createOrg("globex");
        String acmeKey = "sk-ant-acme-secret";

        // Acme stores an Anthropic BYOK key.
        mvc.perform(put("/tenants/keys/anthropic").header(TenantFilter.TENANT_HEADER, acme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + acmeKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("anthropic"))
                .andExpect(jsonPath("$.configured").value(true));

        // Listing shows the provider is configured — but never the key value.
        String listBody = mvc.perform(get("/tenants/keys").header(TenantFilter.TENANT_HEADER, acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("anthropic"))
                .andReturn().getResponse().getContentAsString();
        Assertions.assertThat(listBody).doesNotContain(acmeKey);

        // Globex sees no keys — tenant isolation.
        mvc.perform(get("/tenants/keys").header(TenantFilter.TENANT_HEADER, globex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // At rest it's encrypted, but decrypts back for the owning tenant only.
        UUID acmeId = UUID.fromString(acme);
        Assertions.assertThat(secrets.get(acmeId, TenantKeyController.BYOK_PREFIX + "anthropic"))
                .contains(acmeKey);
        Assertions.assertThat(secrets.get(UUID.fromString(globex),
                        TenantKeyController.BYOK_PREFIX + "anthropic"))
                .isEmpty();

        // Delete removes it.
        mvc.perform(delete("/tenants/keys/anthropic").header(TenantFilter.TENANT_HEADER, acme))
                .andExpect(status().isOk());
        mvc.perform(get("/tenants/keys").header(TenantFilter.TENANT_HEADER, acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
