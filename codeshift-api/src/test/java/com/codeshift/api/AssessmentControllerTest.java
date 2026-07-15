package com.codeshift.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeshift.assessment.AssessmentReport;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Web-slice test for the public assessment endpoint — standalone MockMvc, no
 * Spring context and no database, so it stays fast and infra-free.
 */
class AssessmentControllerTest {

    private final AssessmentService assessment = mock(AssessmentService.class);
    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new AssessmentController(assessment)).build();

    private AssessmentReport canned() {
        return new AssessmentReport("demo", 5, 42, 3, 1, false,
                List.of("JMS"), true, List.of("Uses the javax.* namespace ..."),
                12.5, 2, 999, "Starter",
                List.of("com.acme.repo.OrderRepository", "com.acme.web.OrderController"));
    }

    @Test
    void pathEndpointReturnsReportJson() throws Exception {
        when(assessment.assessDirectory(eq("demo"), any(Path.class))).thenReturn(canned());

        mvc.perform(post("/public/assess/path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectPath\":\"/tmp/demo\",\"projectName\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleCount").value(5))
                .andExpect(jsonPath("$.suggestedTier").value("Starter"))
                .andExpect(jsonPath("$.priceEstimateUsd").value(999))
                .andExpect(jsonPath("$.messagingSystems[0]").value("JMS"));
    }
}
