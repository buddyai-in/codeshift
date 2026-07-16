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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Multi-tenancy: projects created under one tenant (org) are invisible to another,
 * enforced by scoping every read to the X-Tenant-Id header. Against a real (H2) database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tenants;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class TenantIsolationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String createOrg(String name) throws Exception {
        MvcResult res = mvc.perform(post("/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("orgId").asText();
    }

    private void createProject(String orgId, String name) throws Exception {
        mvc.perform(post("/projects")
                        .header(TenantFilter.TENANT_HEADER, orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"sourceLanguage\":\"JAVA_8\","
                                + "\"targetStack\":\"JAVA_21_SPRING_BOOT\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void projectsAreIsolatedPerTenant() throws Exception {
        String acme = createOrg("acme");
        String globex = createOrg("globex");

        createProject(acme, "acme-orders");
        createProject(acme, "acme-billing");
        createProject(globex, "globex-crm");

        // Acme sees only its two projects.
        mvc.perform(get("/projects").header(TenantFilter.TENANT_HEADER, acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='globex-crm')]").isEmpty());

        // Globex sees only its one project.
        mvc.perform(get("/projects").header(TenantFilter.TENANT_HEADER, globex))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("globex-crm"));

        // A request with no tenant header lands in the default org — neither tenant's projects.
        mvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
