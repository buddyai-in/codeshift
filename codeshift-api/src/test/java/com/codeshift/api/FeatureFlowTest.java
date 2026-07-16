package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end new-code addition against a real (H2) database: create a project,
 * persist an initial BSG, add a feature via the Requirements Agent, and confirm a
 * new persisted version carrying the NEW_FEATURE node.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class FeatureFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createProjectSeedBsgThenAddFeature() throws Exception {
        // 1. Create a project.
        MvcResult created = mvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"acme\",\"sourceLanguage\":\"JAVA_8\",\"targetStack\":\"JAVA_21_SPRING_BOOT\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String projectId = json.readTree(created.getResponse().getContentAsString()).get("projectId").asText();

        // 2. Persist an initial BSG (v1) with one rule.
        BsgGraph v1 = new BsgGraph(projectId, 1, List.of(
                BsgNode.extracted("BSG-001", BsgNodeType.BUSINESS_RULE, "Order total", "d",
                        "OrderService", BsgConfidence.HIGH)),
                List.of());
        mvc.perform(post("/projects/" + projectId + "/bsg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(v1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(1));

        // 3. New-code addition: a feature request → a new NEW_FEATURE node in v2.
        MvcResult feature = mvc.perform(post("/projects/" + projectId + "/feature-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"request\":\"When an order ships, send an SMS via Twilio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andReturn();
        JsonNode nodes = json.readTree(feature.getResponse().getContentAsString()).get("bsg").get("nodes");
        boolean hasNewFeature = false;
        for (JsonNode n : nodes) {
            if ("NEW_FEATURE".equals(n.get("origin").asText())) {
                hasNewFeature = true;
            }
        }
        org.assertj.core.api.Assertions.assertThat(nodes.size()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(hasNewFeature).isTrue();

        // 4. The audit trail has both versions, newest first.
        mvc.perform(get("/projects/" + projectId + "/bsg/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(2))
                .andExpect(jsonPath("$[1].versionNumber").value(1));
    }
}
