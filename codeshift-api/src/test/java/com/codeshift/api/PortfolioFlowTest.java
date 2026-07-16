package com.codeshift.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeshift.bsg.model.BsgGraph;
import com.codeshift.bsg.model.BsgNode;
import com.codeshift.common.BsgConfidence;
import com.codeshift.common.BsgNodeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Portfolio Intelligence + Performance/Debt endpoints, end-to-end against H2. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:portfolio;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
})
@AutoConfigureMockMvc
class PortfolioFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String createProjectWithBsg(String name, BsgConfidence confidence) throws Exception {
        var created = mvc.perform(post("/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("projectId").asText();
        BsgGraph bsg = new BsgGraph(id, 1, List.of(
                new BsgNode("BSG-001", BsgNodeType.BUSINESS_RULE, "Rule", "reads the product catalog",
                        "CatalogService", confidence, com.codeshift.common.HumanStatus.APPROVED,
                        com.codeshift.common.BsgOrigin.MIGRATED, null, true)),
                List.of());
        mvc.perform(post("/projects/" + id + "/bsg").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(bsg)))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void aggregatesPortfolioAndSurfacesPerformance() throws Exception {
        String healthy = createProjectWithBsg("clean-app", BsgConfidence.HIGH);
        createProjectWithBsg("risky-app", BsgConfidence.LOW);

        // Portfolio rolls up both projects.
        var res = mvc.perform(get("/portfolio")).andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").value(2)).andReturn();
        JsonNode portfolio = json.readTree(res.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(portfolio.get("projects").size()).isEqualTo(2);

        // Performance surfaces the caching recommendation for the read-heavy catalog rule.
        mvc.perform(get("/projects/" + healthy + "/performance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].type").exists());
    }
}
