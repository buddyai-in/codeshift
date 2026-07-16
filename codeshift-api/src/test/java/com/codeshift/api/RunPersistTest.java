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
 * End-to-end proof that approving a migration run's BSG gate auto-persists the
 * curated BSG into a new project (an approved v1), against a real (H2) database —
 * the bridge from the in-memory run pipeline to the persistent new-code/portfolio pillars.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:runpersist;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class RunPersistTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void approvingBsgGateAutoPersistsProject() throws Exception {
        // 1. Start a run (no source path → the built-in sample modules drive discovery).
        MvcResult started = mvc.perform(post("/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"acme-legacy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awaitingHuman").value(true))
                .andReturn();
        String threadId = json.readTree(started.getResponse().getContentAsString())
                .get("threadId").asText();

        // 2. Approve the BSG gate → the run's BSG is auto-persisted into a new project.
        MvcResult resumed = mvc.perform(post("/runs/" + threadId + "/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistedProjectId").isNotEmpty())
                .andReturn();
        String projectId = json.readTree(resumed.getResponse().getContentAsString())
                .get("persistedProjectId").asText();

        // 3. The project shows up in the portfolio-facing project list.
        mvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + projectId + "')].name").value("acme-legacy"));

        // 4. It has an approved v1 BSG — the exact snapshot the human signed off on.
        mvc.perform(get("/projects/" + projectId + "/bsg/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].approved").value(true))
                .andExpect(jsonPath("$[0].approvedBy").value("migration-run"));

        // 5. Approving the second (architecture) gate must NOT create a duplicate project.
        mvc.perform(post("/runs/" + threadId + "/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persistedProjectId").value(projectId));

        mvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
